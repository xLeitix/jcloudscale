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
package at.ac.tuwien.infosys.jcloudscale.classLoader.caching.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.classLoader.RemoteClassLoaderUtils;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderFile;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ContentType;

/**
 * @author rst
 * Cache manager that stores all information in the file system.
 * Special folder is created on the server and 
 * all code provided by clients is stored there. New requests
 * will offer client already available code in cache. If client agrees
 * that the version of the code offered is correct, server uses this code
 * without transmitting it over network. However, note that cache is local to
 * the machine and each machine will maintain its own cache.
 */
public class FileCacheManager extends CacheManagerAbstract
{
	/**
	 * Maps files to the items they belong to. (Which files belong to this item?)
	 */
	private Map<String, String[]> itemToFilesMap = new HashMap<String, String[]>();
	
	/**
	 * Maps classes that have files within this offer to the offer. (Which classes have additional files in this offer?) 
	 */
	private Map<String, String[]> itemToClassesWithFiles = new HashMap<String,String[]>();
	
	/**
	 * Maps item creation date to item name (When this item was created?)
	 */
	private Map<String, Long> itemToCreationDateMap = new HashMap<String,Long>();
	/**
	 * Maps classes to files+items they belong to. (Which classes belong to this file within this item?)
	 */
	private Map<String, String[]> itemFileToClassesMap = new HashMap<String, String[]>();
	/**
	 * Maps path in the file system to the file id+item id. (Where this file is located?)
	 */
	private Map<String, String> itemFileToPathMap = new HashMap<String, String>();
	/**
	 * Maps content type of the file to the file+item. (what type this file has?)
	 */
	private Map<String, ContentType> itemFileToContentType = new HashMap<String, ContentType>();
	/**
	 * Maps file id to the class+item. (which file represents class in the specified item?)
	 */
	private Map<String, String> itemClassToFileMap = new HashMap<String,String>();
	/**
	 * Maps all items to the class that they have. (which items this class belongs to?)
	 */
	private Map<String, String[]> classToItemsMap = new HashMap<String, String[]>();
	
	/**
	 * The collection of files that were deployed outside the cache and should be maintained separately.
	 */
	private Set<String> deployedFilesSet = new HashSet<String>();
	private FileCacheConfiguration cfg;
	
	// -------------------------------------------------------------------

	FileCacheManager(FileCacheConfiguration configuration)
	{
		super(configuration);
		this.cfg = configuration;
		
		loadIndex();
	}
	
	//-------------------------------------------CONFIGURATION------------------------------------------------
	
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class FileCacheConfiguration extends CacheConfiguration 
	{
		private static final long serialVersionUID = 1L;
		
		String cacheIndexFile = "index.props";

		@Override
		public ICacheManager createCacheManager() 
		{
			return new FileCacheManager(this);
		}
	}
	
	
	// --------------------------------------------INDEX OPERATIONS-------------------------------------------

	@XmlRootElement()
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class CacheManagerIndex
	{
		 UUID id;
		 Map<String, String[]> itemToFilesMap;
		 Map<String, String[]> itemToClassesWithFiles;
		 Map<String, Long> itemToCreationDateMap;
		 Map<String, String[]> itemFileToClassesMap;
		 Map<String, String> itemFileToPathMap;
		 Map<String, ContentType> itemFileToContentType;
		 Map<String, String> itemClassToFileMap;
		 Map<String, String[]> classToItemsMap;
		 Set<String> deployedFilesSet;
	}
	
	/**
	 * Loads index file and builds all required structures to work with cache data.
	 */
	protected synchronized void loadIndex()
	{
		//checking if there is an index to read.
		String indexFilePath =  this.cacheLocation.getPath()+File.separator+this.cfg.cacheIndexFile;
		File indexPathFile = new File(indexFilePath);
		if(!indexPathFile.exists())
			return;//we don't have index file

		// reading index
		CacheManagerIndex index = null;
		try 
		{
			index = (CacheManagerIndex)JAXBContext
								.newInstance(CacheManagerIndex.class)
								.createUnmarshaller()
								.unmarshal(indexPathFile);
		} 
		catch (JAXBException e) 
		{
			  log.severe("Failed to deserialize index file:"+e.toString());
			  //we have corrupted index, so why to use it?
			  return;//if we won't return here, nullreferenceexception will be thrown. 
		}
		
		//restoring maps
		this.cfg.id =  index.id;
		this.classToItemsMap =   index.classToItemsMap;
		this.itemToClassesWithFiles = index.itemToClassesWithFiles;
		this.itemClassToFileMap =   index.itemClassToFileMap;
		this.itemFileToClassesMap =   index.itemFileToClassesMap;
		this.itemFileToContentType =   index.itemFileToContentType;
		this.itemFileToPathMap =   index.itemFileToPathMap;
		this.itemToCreationDateMap =   index.itemToCreationDateMap;
		this.itemToFilesMap =   index.itemToFilesMap;
		this.deployedFilesSet =   index.deployedFilesSet;
	}

	/**
	 * Saves current index to the cache.
	 */
	protected synchronized void saveIndex()
	{
		// creating index object
		CacheManagerIndex index = new CacheManagerIndex();
		
		index.id = this.cfg.id;
		index.classToItemsMap = this.classToItemsMap;
		
		index.itemToClassesWithFiles = this.itemToClassesWithFiles;
		index.itemToCreationDateMap = this.itemToCreationDateMap;
		index.itemToFilesMap = this.itemToFilesMap;
		
		index.itemClassToFileMap = this.itemClassToFileMap;
		
		index.itemFileToClassesMap = this.itemFileToClassesMap;
		index.itemFileToContentType = this.itemFileToContentType;
		index.itemFileToPathMap = this.itemFileToPathMap;
		
		index.deployedFilesSet = this.deployedFilesSet;
		
		//saving to index file
		String indexFilePath =  this.cacheLocation.getPath()+File.separator+this.cfg.cacheIndexFile;
		
		try 
		{
			JAXBContext.newInstance(CacheManagerIndex.class)
						.createMarshaller()
						.marshal(index, new File(indexFilePath));
		} 
		catch (JAXBException e) 
		{
			  this.log.severe("Failed to serialize index: "+e.toString());
		}
	}

	// ---------------------------------------------------PUBLIC INTERFACE-------------------------------------------

	@Override
	protected synchronized void cleanup()
	{
		//cleaning up deployed files
		if(deployedFilesSet != null)
		{
			for(String path : deployedFilesSet)
			{//TODO: if we created any folders, we still don't delete them
				File file = new File(path);
				if(!file.exists())
					continue;
				
				if(!file.delete())
					file.deleteOnExit();
			}
		}
		
		if (!this.cfg.shareCache)
			RemoteClassLoaderUtils.deleteFolderRec(cacheLocation);
		else
			saveIndex();
	}
	
	@Override
	public synchronized String[] getItemNames(String className) 
	{
		if(className == null)
			return null;
		
		return this.classToItemsMap.get(className);
	}
	
	@Override
	public synchronized String[] getClassesWithFiles(String itemName)
	{
		if(itemName == null || itemName.length() == 0)
			return null;
		
		return this.itemToClassesWithFiles.get(itemName);
	}

	@Override
	public synchronized byte[] getClassBytecode(String className, String itemName) 
	{
		//getting file where this class is located.
		String fileName = this.itemClassToFileMap.get(formKey(itemName, className));
		if(fileName == null)
			return null;//no such file.
		
		// getting file location and type.
		String itemKey = formKey(itemName, fileName);
		ContentType type = this.itemFileToContentType.get(itemKey);
		String path = this.itemFileToPathMap.get(itemKey);
		try
		{
			// getting class bytecode
			if(type == ContentType.JAR)
				return this.getBytecodeFromJar(path, className);
			else if(type == ContentType.CLASS)
				try(FileInputStream fileStream = new FileInputStream(path))
				{
					return RemoteClassLoaderUtils.getByteArray(fileStream); 
				}
			else //some shit is going on. This is not jar and not class.
				throw new RuntimeException("Trying to retrieve bytecode for class "+className+" from item "+itemName+" we received non-code file.");
		}
		catch(IOException ex)
		{
			this.log.severe("Logger failed to read file "+path+" content:" +ex.toString());
			throw new RuntimeException("Failed to read file content", ex);
		}
	}

	@Override
	public synchronized String getFileLocation(String fileName, String itemName) 
	{
		return this.itemFileToPathMap.get(formKey(itemName, fileName));
	}

	@Override
	public synchronized ClassLoaderOffer createOfferByItemName(String itemName) 
	{
		if(!itemToFilesMap.containsKey(itemName))
			return null;
		
		long itemCreated = this.itemToCreationDateMap.get(itemName);
		ClassLoaderOffer offer = new ClassLoaderOffer(itemName, itemCreated);
		
		List<ClassLoaderFile> offerFiles = new ArrayList<ClassLoaderFile>();
		for(String fileName : this.itemToFilesMap.get(itemName))
		{
			String fileKey = formKey(itemName, fileName);
			File file = new File(this.itemFileToPathMap.get(fileKey));
			ContentType contentType = this.itemFileToContentType.get(fileKey);
			
			offerFiles.add(new ClassLoaderFile(fileName, file.lastModified(), file.length(), contentType));
		}
		
		offer.setFiles(offerFiles.toArray(new ClassLoaderFile[offerFiles.size()]));
		offer.setClassesWithFiles(this.itemToClassesWithFiles.get(itemName));
		
		return offer;
	}

	@Override
	public synchronized void registerOffer(ClassLoaderOffer offer) 
	{
		//checking whether we have this offer in the system.
		String offerName = offer.getName();

		if(offerName == null || offerName.length() == 0 || 
				!this.itemToFilesMap.containsKey(offerName))
		{//we don't have it, let's generate name and creation date.
			offerName = UUID.randomUUID().toString();
			offer.setName(offerName);
			offer.setCreationDate(System.currentTimeMillis());
		}
		
		//updating offer lastModified
		this.itemToCreationDateMap.put(offerName, offer.getCreationDate());
		
		//updating files
		List<String> fileNames = new ArrayList<String>();
		for(ClassLoaderFile file : offer.getFiles())
		{
			fileNames.add(file.getName());
			String fileKey = formKey(offerName, file.getName());
			
			//
			// storing the file on the drive.
			//
			String path = this.itemFileToPathMap.get(fileKey);
			File pathFile = null;
			if(path == null)
			{//we don't have this file, we should create it.
				path = convertToCachedFileName(this.cacheLocation.getPath(), file.getName(), file.getType());
				pathFile = new File(path);
				if(pathFile.exists())
					if(!this.isCorrectFileAttributes(pathFile, file.getSize(), file.getLastModifiedDate()))
					{//there is such file, but it's not our. we go for another one.
						path = convertToCachedFileName(this.cacheLocation.getPath(), 
														UUID.randomUUID().toString() + file.getName(), 
														file.getType());
						pathFile = new File(path);
					}
					else
						pathFile = null; //if it is the same file, there's no need to update
			}
			else 
				pathFile = new File(path);
			
			try
			{
				if(pathFile != null)
					createFile(pathFile, file.getContent(), file.getLastModifiedDate());
			}
			catch(IOException ex)
			{
				log.severe("Failed to store provided file"+path+" to the drive:" + ex.toString());
				throw new RuntimeException("Failed to store provided file on the drive:", ex);
			}
			
			this.itemFileToContentType.put(fileKey, file.getType());
			this.itemFileToPathMap.put(fileKey, path);
			
			//
			// Collecting classes from file
			//
			List<String> classesList = null;
			switch(file.getType())
			{
			case CLASS:
				classesList = Arrays.asList(file.getName());
				break;
			case JAR:
				try 
				{
					classesList = collectClassesFromJar(path);
					break;
				} catch (IOException ex) 
				{
					throw new RuntimeException("Failed to collect classes from the provided jar "+file.getName()+" while registering offer "+offerName, ex);
				}
			default://other types do not have classes.
				continue;
			}
			
			this.itemFileToClassesMap.put(fileKey, classesList.toArray(new String[classesList.size()]));
			
			//
			// Registering classes
			//
			for(String className : classesList)
			{
				this.itemClassToFileMap.put(formKey(offerName, className), file.getName());
				
				String[] updatedOwningItems = updateArray(this.classToItemsMap.get(className), 
														 new String[]{offerName});
				if(updatedOwningItems != null)
					this.classToItemsMap.put(className, updatedOwningItems);
			}
		}
		
		//
		// Updating the set of registered classes that belong to this offer.
		//
		
		String[] updatedClassesWithFiles = updateArray(this.itemToClassesWithFiles.get(offerName), 
														offer.getClassesWithFiles());
			if(updatedClassesWithFiles != null)
				this.itemToClassesWithFiles.put(offerName, updatedClassesWithFiles);
		//
		// Updating the set of files that belong to this offer
		//
		String[] updatedRegisteredFiles = updateArray(this.itemToFilesMap.get(offerName), 
														fileNames.toArray(new String[fileNames.size()]));
		if(updatedRegisteredFiles != null)
			this.itemToFilesMap.put(offerName, updatedRegisteredFiles);
	}

	@Override
	public synchronized void removeOfferByName(String itemName) 
	{
		if(itemName == null || itemName.length() == 0)
			return;
		
		this.itemToCreationDateMap.remove(itemName);
		String[] files = this.itemToFilesMap.remove(itemName);
		if(files == null)
			return;
		
		for(String file : files)
		{
			String fileKey = formKey(itemName, file);
			
			//removing file
			String filePath = this.itemFileToPathMap.remove(fileKey);
			if(filePath != null)
			{
				File fileFile = new File(filePath);
				if(!fileFile.delete())
					fileFile.deleteOnExit();
			}
			//removing from other collections.
			this.itemFileToContentType.remove(fileKey);
			String[] classes = this.itemFileToClassesMap.remove(fileKey);
			
			if(classes == null)
				continue;
			
			//removing classes
			for(String clazz : classes)
			{
				this.itemClassToFileMap.remove(formKey(itemName, clazz));

				String[] items = this.classToItemsMap.remove(clazz);
				if(items == null)
					continue;
				
				List<String> newItems = new ArrayList<String>(items.length-1);
				for(String item : items)
					if(!item.equals(itemName))
						newItems.add(item);
				
				if(newItems.size() > 0)
					this.classToItemsMap.put(clazz, newItems.toArray(new String[newItems.size()]));
			}
		}
	}
	
	@Override
	public synchronized boolean deployFile(String fileName, String offerName) 
	{
		String fileKey = formKey(offerName, fileName);
		String path = this.itemFileToPathMap.get(fileKey);
		if(path == null)
			return false;
		
		//we do not check and write any file we're asked.
		try
		{
			try(FileInputStream fileStream = new FileInputStream(path))
			{
				File file = new File(fileName);
				
				RemoteClassLoaderUtils.mkDirRec(file.getParentFile());//creating required folders
				
				this.createFile(file, RemoteClassLoaderUtils.getByteArray(fileStream), new File(path).lastModified());
				
				// in case deployed file set is missing...
				if(deployedFilesSet == null)
					deployedFilesSet = new HashSet<>();
					
				deployedFilesSet.add(file.getCanonicalPath());
			}
		}
		catch(IOException ex)
		{
			log.severe("Failed to deploy file " + fileName + " to path " + path+". Exception: "+ex);
			return false;
		}
		return true;
	}
}
