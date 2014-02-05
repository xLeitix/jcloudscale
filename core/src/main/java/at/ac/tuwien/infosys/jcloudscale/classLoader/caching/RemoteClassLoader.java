/*
   Copyright 2013 Philipp Leitner

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package at.ac.tuwien.infosys.jcloudscale.classLoader.caching;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.annotations.FileDependency;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.cache.ICacheManager;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderFile;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderRequest;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderResponse;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.TimeoutException;

public class RemoteClassLoader extends URLClassLoader implements Closeable
{
	private IMQWrapper mq;
	private Logger log;
	private ICacheManager cache;
	private UUID id = UUID.randomUUID();
	private boolean localFirst;
	private Set<String> usedOffers = new HashSet<String>();

	RemoteClassLoader(CachingClassLoaderConfiguration configuration)
	{
		super(new URL[] {});
		
		this.log = JCloudScaleConfiguration.getLogger(this);
		
		try
		{
			this.mq = JCloudScaleConfiguration.createMQWrapper(); 
			
			mq.createTopicConsumer(configuration.responseQueue, "JMSCorrelationID = '"+id.toString()+"'");
			mq.createQueueProducer(configuration.requestQueue);
		}
		catch(JMSException | NamingException e)
		{
			log.severe("Failed to connect to message queue from RemoteClassLoader:" + e.toString());
			throw new JCloudScaleException(e, "Failed to connect to message queue from RemoteClassLoader");
		}
		
		this.cache = configuration.getCacheManager();
		this.localFirst = configuration.localFirst;
	}

	@Override
	public void close()
	{
		mq.close();
		
		if(cache != null)
		{
			cache.close();
			cache = null;
		}
	}

	@Override
	protected synchronized Class<?> findClass(String name) throws ClassNotFoundException
	{
		Class<?> clazz = null;

		// trying to load from parent findClassMethod
		try
		{
			clazz = super.findClass(name);
			ensureAdditionalClassesLoaded(clazz);
			return clazz;
		}
		catch (ClassNotFoundException ex)
		{// this is fine, this means that we don't have it in registered jars.
		}

		clazz = loadClassData(name);

		if (clazz == null)
			throw new ClassNotFoundException(name);

		return clazz;
	}

	/**
	 * Ensures that all additional files that this file requires are loaded.
	 */
	private void ensureAdditionalClassesLoaded(Class<?> clazz) 
	{
		if(!cache.isClassLoadedFromCache(clazz))
			return;
		
		if(!hasFileDependencyAnnotation(clazz))
			return;
		
		// we're here if annotation is present. We have to detect if all files are loaded.
		//
		//getting items where this class is registered.
		//
		String[] items = cache.getItemNames(clazz.getName());
		
		// finding the item that we used to instantiate this class.
		String usedItem = null;
		if(items != null)
			for(String item : items)
				if(usedOffers.contains(item))
				{
					usedItem = item;
					break;
				}
		//
		//checking if files for this item were loaded.
		//
		ClassLoaderRequest request = null;
		if(usedItem != null)
		{
			String[] classesWithFiles = cache.getClassesWithFiles(usedItem);
			if(classesWithFiles != null && Arrays.asList(classesWithFiles).contains(clazz.getName()))
				return;//everything is fine, we loaded all necessary stuff.
			
			request = new ClassLoaderRequest(clazz.getName(), new ClassLoaderOffer[]{cache.createOfferByItemName(usedItem)});
		}
		else
		{
			log.info("Class "+clazz.getName()+" has FileDependency annotation, but we could not detect the offer that this class was taken from. Asking from the client.");
			request = new ClassLoaderRequest(clazz.getName());
		}
		
		// files were not loaded. asking client for the data for our class. 
		// Client will have to include files.
		try 
		{
			ClassLoaderResponse response = (ClassLoaderResponse) mq.requestResponse(request, id);
			ClassLoaderOffer acceptedOffer = response.getAcceptedOffer();

			//updating the offer
			if(acceptedOffer.hasFiles())
			{
				cache.registerOffer(acceptedOffer);
				// unpacking and using it.
				//we don't need class, just files. but let's do the whole sequence...
				useOffer(null, acceptedOffer);
			}
			else
				log.info("Trying to load missing dependent files of class "+clazz.getName()+" we received offer without files. Nothing to load.");
		} 
		catch (JMSException | TimeoutException e) 
		{
			log.severe("Failed to load additional files for class "+clazz.getName()+" . Exception occured: "+e);
			return;
		}
		
	}

	private boolean hasFileDependencyAnnotation(Class<?> clazz) 
	{
		//return clazz.isAnnotationPresent(FileDependency.class);// because of some reason, does not work. (different classloader?)
		Annotation[] annotations = clazz.getAnnotations();
		if(annotations == null || annotations.length == 0)
			return false;
		
		for(Annotation annotation : annotations)
			if(annotation.annotationType().getName().equals(FileDependency.class.getName()))
				return true;
		
		return false;
	}

	/**
	 * Loads class and all additional required information from the client.
	 * @param className The name of the class to load
	 * @return The Class object of the required class.
	 */
	private Class<?> loadClassData(String className)
	{
		long start = System.nanoTime();
		try
		{
			//
			// Creating request
			//
			ClassLoaderRequest request = createRequestFromCache(className);

			if (request == null)
			{	// this class is not registered in cache
				log.fine(String.format("Class %s cannot be found in cache, requesting code from the client.", className));
				request = new ClassLoaderRequest(className);
			}
			else
				log.fine(String.format("Class %s exists in cache in a %s offer(s). Verifying if client has the same code.",
						className, request.getOffers().length));

			ClassLoaderResponse response = (ClassLoaderResponse) mq.requestResponse(request, id);
			
			if(response == null)
			{
				log.severe("Client sent null response for class "+className+". Something is wrong out there.");
				return null;
			}
			
			ClassLoaderOffer acceptedOffer = response.getAcceptedOffer();
			
			if(acceptedOffer == null || (!acceptedOffer.hasFiles() && !acceptedOffer.hasName()))
			{//client does not like the stuff we sent and did not provide anything instead. Shit, we're screwed.
				if(request.getOffers() == null || request.getOffers().length == 0)
					log.severe(String.format("Client did not provide code for class %s and no cached version available.", className));
				else
					log.severe(String.format("Client did not provide code for class %s and did not selected cached version.", className));
				
				return null;
			}
			
			if (!acceptedOffer.hasFiles())//can't check whether the name is specified as it can be cache item update.
			{// no code from client, but offer is selected
				String selectedOffer = acceptedOffer.getName();
				log.fine(String.format("Client selected offer %s for class %s.",selectedOffer, className));
				return useOffer(className, request.getOfferByName(selectedOffer));
			}
			else
			{// client provided code.
				log.fine(String.format("Client provided code for class %s within %s files. saving it to cache and using it.",
						className, acceptedOffer.getFiles().length));
				
				//updating the offer
				cache.registerOffer(acceptedOffer);
				
				// merging 2 offers together and using it.(alternative would be to create offer again from cache, but not if NoCache is used)
				ClassLoaderOffer proposedOffer = request.getOfferByName(acceptedOffer.getName());
				if(proposedOffer != null)
					updateOfferFiles(acceptedOffer, proposedOffer.getFiles());
				
				return useOffer(className, acceptedOffer);
			}
		}
		catch (Exception e)
		{
			log.severe("Failed to load class "+ className +": " + e.toString());
			e.printStackTrace();
			return null;
		}
		finally
		{
			String msg = "REMOTE CLASS LOADER: loading of " + className + " took " +
					(System.nanoTime() - start) / 1000000 + "ms.";
			log.fine(msg);
		}
	}
	
	/**
	 * Updates files of offer to contain proposed files.
	 */
	private void updateOfferFiles(ClassLoaderOffer offer, ClassLoaderFile[] otherFiles) 
	{
		if(otherFiles == null || otherFiles.length == 0)
			return;
		
		List<ClassLoaderFile> filesList = new ArrayList<ClassLoaderFile>(Arrays.asList(offer.getFiles()));
		
		for(ClassLoaderFile otherFile : otherFiles)
			if(!containsFile(filesList, otherFile))
				filesList.add(otherFile);
		
		offer.setFiles(filesList.toArray(new ClassLoaderFile[filesList.size()]));
	}
	
	/**
	 * Checks whether list of files contains file with the same name as proposed.
	 */
	private boolean containsFile(List<ClassLoaderFile> filesList, ClassLoaderFile otherFile) 
	{
		for(ClassLoaderFile file : filesList)
			if(file.getName().equals(otherFile.getName()))
				return true;
		
		return false;
	}

	/**
	 * Creates request basing on the cache data.
	 */
	private ClassLoaderRequest createRequestFromCache(String className)
	{
		String[] items = cache.getItemNames(className);
		if(items == null || items.length == 0)
			return null;
		
		List<ClassLoaderOffer> offers = new ArrayList<ClassLoaderOffer>();
		for(String itemName : items)
			offers.add(cache.createOfferByItemName(itemName));
		
		return new ClassLoaderRequest(className, offers.toArray(new ClassLoaderOffer[offers.size()]));
	}

	private Class<?> useOffer(String className, ClassLoaderOffer offer) 
	{
		try
		{
			if(offer == null)
			{
				log.severe("Failed to use code to resolve class "+className+" as there's no offer provided.");
				return null;
			}
			Class<?> requestedClass = null;
			//we go through all files in this offer and register them within this classloader
			for(ClassLoaderFile file : offer.getFiles())
			{
				switch(file.getType())
				{
					case CLASS:
						
						if(super.findLoadedClass(file.getName()) != null)
						{
							log.info("Attempting to load already loaded class: "+file.getName());
							continue;
						}
						
						byte[] bytecode = file.getContent();
						if(bytecode == null)//if there's no content in transmitted file, we have to get it from the cache.
							bytecode = cache.getClassBytecode(file.getName(), offer.getName());
						
						if(bytecode == null)
						{
							log.severe("Failed to define class "+file.getName()+" from offer "+offer.getName());
							continue;
						}

						Class<?> clazz = defineClass(file.getName(), bytecode, 0, bytecode.length);
						if(clazz.getName().equals(className))
							requestedClass = clazz;
						
						break;
					
					case JAR:
						String path = cache.getFileLocation(file.getName(), offer.getName());
						
						if(path == null)
						{
							log.severe("Failed to find file "+file.getName()+" from package "+offer.getName());
							continue;
						}
						
						try 
						{
							addURL(new File(path).toURI().toURL());
						} 
						catch (MalformedURLException e) 
						{
							log.severe("Exception while registering file "+path +":"+e.toString());
							continue;
						}
						break;
						
					case ROFILE:
//					case RWFILE:
						if(!cache.deployFile(file.getName(), offer.getName()))
							log.severe("Failed to deploy required file "+file.getName()+". Failed to write file to disk.");
						break;
						
					default:
						log.severe("Unexpected type of file ("+file.getType()+") provided in the offer. skipping.");
				}
			}
			
			if(className != null && requestedClass == null)
			{//this can happen as it is possible that the required class is in the jar
				byte[] bytecode = cache.getClassBytecode(className, offer.getName());
				if(bytecode != null)
					requestedClass = defineClass(className, bytecode, 0, bytecode.length);
			}
			
			usedOffers.add(offer.getName());//if everything is fine, we add this offer as used.
			
			return requestedClass;
		}
		catch(Throwable ex)
		{
			log.severe("Failed to use offer "+ (offer != null ? offer.getName() : "NULL offer ") +" from the cache. Removing the offer and failing...");
			
			if(offer != null)
				cache.removeOfferByName(offer.getName());
			
			throw ex;
		}
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		@SuppressWarnings("resource")
		InputStream stream = localFirst ? super.getResourceAsStream(name) : getRemoteResourceAsStream(name);
		return stream != null ? stream : (localFirst ? getRemoteResourceAsStream(name) : super.getResourceAsStream(name));
	}

	/**
	 * Returns an input stream for reading the specified remote resource.<br/>
	 *
	 * @param name the resource name
	 * @return an input stream for reading the resource, or {@code null} if the resource could not be found
	 */
	protected InputStream getRemoteResourceAsStream(String name) {
		try 
		{
			log.fine("Trying to load resource "+name+" from the class provider.");
			
			ClassLoaderRequest request = new ClassLoaderRequest(name);
			ClassLoaderResponse response = (ClassLoaderResponse) mq.requestResponse(request, id);
			ClassLoaderOffer offer = response.getAcceptedOffer();
			
			log.fine("As a response to loading "+name+" received offer "+offer);
			
			// If a remote resource was found, store it in the cache and return it
			if (offer != null && offer.hasFiles()) 
			{
				cache.registerOffer(offer);
				useOffer(null, offer);
				String fileLocation = cache.getFileLocation(name, offer.getName());
				
				// we could have got null because of wrong platform-dependent 
				// separator used by user. Let's try again with fixed one. 
				// (linux can use windows-style separators for escaping, 
				// therefore it's not a good idea to always replace them)
				if(fileLocation == null)
					fileLocation = cache.getFileLocation(name.replace('/', File.separatorChar)
															 .replace('\\',  File.separatorChar), 
														 offer.getName());
				
				if (fileLocation != null) 
				{
					return new FileInputStream(fileLocation);
				}
				else
					log.warning("The received offer does not contain the file requested or the file could not be stored: cache returned NULL");
			}
		} catch (JMSException | TimeoutException e) {
			throw new JCloudScaleException(e);
		} catch (FileNotFoundException e) {
			// Ignore
		}
		return null;
	}
}
