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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderFile;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderRequest;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderResponse;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.fileCollectors.FileCollectorAbstract;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;

public class RemoteClassProvider implements MessageListener, Closeable
{
	protected Logger log;
	private IMQWrapper mq;
	private FileCollectorAbstract fileCollector;
	private CachingClassLoaderConfiguration cfg; 
	
	/**
	 * Creates new instance of RemoteClassProvider that satisfies class dependency
	 * requests from the cloud instances.
	 * 
	 * @param CachingClassLoaderConfiguration the configuration of the class provider
	 */
	RemoteClassProvider(CachingClassLoaderConfiguration configuration)
	{
		this.cfg = configuration;
		this.log = JCloudScaleConfiguration.getLogger(this);
		this.fileCollector = configuration.getFileCollector();
		
		try 
		{
			this.mq = JCloudScaleConfiguration.createMQWrapper();
			this.mq.createQueueConsumer(cfg.requestQueue);
			this.mq.registerListener(this);
		} 
		catch (JMSException | NamingException e) 
		{
			log.severe("Failed to connect to message queue from RemoteClassProvider:" + e.toString());
			throw new JCloudScaleException(e, "Failed to connect to message queue from RemoteClassProvider");
		}		
	}
	
	@Override
	public void close()
	{
		if(this.mq != null)
		{
			mq.close();
			mq = null;
		}
	}

	@Override
	public void onMessage(Message message)
	{
		String className = null;
		try
		{
			ClassLoaderRequest request = (ClassLoaderRequest) ((ObjectMessage) message).getObject();
			
			log.fine("Class load request received:" + request);
			
			if(request == null)
			{
				log.warning("NULL class load request received, dropping.");
				return;
			}
			
			className = request.getRequestedClassName();//we need it just for logging.

			ClassLoaderResponse response = createClassLoaderResponse(request);

			log.fine(response.toString());

			try
			{
				// we expect sender to specify the query he's waiting response into the JMSReplyTo
				mq.respond(response, message.getJMSReplyTo(), UUID.fromString(message.getJMSCorrelationID()));
			}
			catch (Exception ex)
			{
				log.severe("Failed to provide required class " + className + ": " + ex.toString());
				ex.printStackTrace();
			}
		}
		catch (Exception e)
		{
			log.severe("Failed to process message (" + className + "): " + e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Checks if the provided offer is acceptable and, updates it to be ready to send back 
	 * (removes file descriptions if everything is ok or adds code to the updated files.
	 * @return <b>true</b> if offer is acceptable; otherwise, <b>false</b>.
	 */
	private boolean isOfferAcceptable(ClassLoaderOffer offer, HashMap<String, ClassLoaderFile> classToFileMap, String[] classesWithFiles) 
	{
		if(offer == null)
			return false;
		
		if(offer.getFiles() == null)
		{
			log.severe("Offer "+offer.getName()+" has NULL instead of files.");
			return false;
		}
		
		//
		// we go through the files in the offer and search for files 
		// that are the same as our files. If necessary -- we update them.
		//
		List<ClassLoaderFile> updatedFiles = new ArrayList<ClassLoaderFile>();
		HashSet<String> foundFiles = new HashSet<String>();
		for(ClassLoaderFile fileFromOffer : offer.getFiles())
			if(classToFileMap.containsKey(fileFromOffer.getName()))
			{//we found it. now it will be either accepted or the whole offer will be rejected.
				ClassLoaderFile ourFile = classToFileMap.get(fileFromOffer.getName());
				if(fileFromOffer.getLastModifiedDate() > ourFile.getLastModifiedDate())
					return false;//we will have to create new offer anyways...
				
				foundFiles.add(ourFile.getName());
				
				if(ourFile.getLastModifiedDate() == fileFromOffer.getLastModifiedDate() &&
						ourFile.getSize() == fileFromOffer.getSize())
					continue;//they are the same.
				
				// ours is different, but not worse. we will update it.
				updatedFiles.add(ourFile);
			}
		
		//
		//TODO: decide what to do with the offer if we need to add few files to make it correct.
		// Both approaches (create new offer and extend) can produce issues.
		// create new offer will leave old offer unused in case we are in a continuing development cycle
		// extend offer might lead to conflicting files (for ex, few jars with dif. names with same class).
		//
		//if we decide to not add new files, this is the way to do it.
		//----------------
//		if(foundFiles.size() < classToFileMap.size())
//			return false;//we did not found all files.
		//----------------		
		if(foundFiles.size() == 0)
			return false;//if we did not found any file the we can use, than this is not our offer.
		
		//adding remaining files
		for(Entry<String, ClassLoaderFile> file : classToFileMap.entrySet())
			if(!foundFiles.contains(file.getKey()))
				updatedFiles.add(file.getValue());
		
		//adding classes with files, if necessary
		if(offer.getClassesWithFiles() == null)
			offer.setClassesWithFiles(classesWithFiles);
		else
		{
			List<String> registeredClasses = new ArrayList<String>(Arrays.asList(offer.getClassesWithFiles()));
			for(String clazz : classesWithFiles)
				if(!registeredClasses.contains(clazz))
					registeredClasses.add(clazz);
			
			offer.setClassesWithFiles(registeredClasses.toArray(new String[registeredClasses.size()]));
		}
		//----------------
		
		//if we are here, this offer was accepted. time to prepare it for sending back.
		ClassLoaderFile[] newFiles = null;
		if(updatedFiles.size()> 0)
		{//if we have some updated files, we have to prepare them.
			newFiles = new ClassLoaderFile[updatedFiles.size()];
			updatedFiles.toArray(newFiles);
			for(ClassLoaderFile file : newFiles)
				try 
				{
					fileCollector.loadContent(file);
				} 
				catch (IOException ex) 
				{
					log.severe("Failed to load content of the file "+file.getName()+": "+ex.toString());
				}
		}
		
		//updating or removing all files
		offer.setFiles(newFiles);
		return true;
	}
	
	private ClassLoaderResponse createClassLoaderResponse(ClassLoaderRequest request) 
	{	
		String classname = request.getRequestedClassName();
		ClassLoaderResponse response = new ClassLoaderResponse(classname);

		//
		// Getting our offer.
		//
		ClassLoaderOffer offerFiles = fileCollector.collectFilesForClass(classname);
		
		if(offerFiles == null || !offerFiles.hasFiles())
		{
			log.severe("Failed to collect files for the class "+classname);
			return new ClassLoaderResponse(classname, null);
		}
		
		HashMap<String,ClassLoaderFile> classToFileMap = new HashMap<String, ClassLoaderFile>();
		for(ClassLoaderFile file : offerFiles.getFiles())
			classToFileMap.put(file.getName(), file);
		
		//
		// Considering offered packages.
		//
		if(request.getOffers() != null)
		{
			ClassLoaderOffer selectedOffer = null;
			for(ClassLoaderOffer offer : request.getOffers())
				if(isOfferAcceptable(offer, classToFileMap, offerFiles.getClassesWithFiles()))
				{
					log.fine("For the class "+classname+" offer "+offer.getName()+
							" was accepted "+ (offer.getFiles() != null ? 
									"with updates." : "without updates."));
					
					selectedOffer = offer;
					break;
				}
			
			response.setAcceptedOffer(selectedOffer);
		}
		
		//if we did not specified accepted offer, we have to provide our code as a new offer.
		if(response.getAcceptedOffer() == null)
		{
			log.fine("None of the offers for class "+classname+" was acceptable, offering new one.");
			
			//loading file content
			for(ClassLoaderFile file : offerFiles.getFiles())
				try 
				{
					fileCollector.loadContent(file);
				} 
				catch (IOException ex) 
				{
					log.severe("Failed to load content of the file "+file.getName()+" while preparing package for class "+classname+": "+ex.toString());
				}
			//creating response
			response.setAcceptedOffer(offerFiles);
		}
		
		return response;
	}

}
