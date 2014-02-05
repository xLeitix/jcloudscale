package at.ac.tuwien.infosys.jcloudscale.classLoader.caching.cache;
///*
//   Copyright 2013 Philipp Leitner
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//*/
//package at.ac.tuwien.infosys.jcloudscale.classLoader.caching.cache;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//import java.util.UUID;
//
//import javax.xml.bind.annotation.XmlAccessType;
//import javax.xml.bind.annotation.XmlAccessorType;
//import javax.xml.bind.annotation.XmlRootElement;
//
//import at.ac.tuwien.infosys.jcloudscale.classLoader.RemoteClassLoaderUtils;
//import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderFile;
//import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;
//import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ContentType;
//import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
//import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
//import at.ac.tuwien.infosys.jcloudscale.server.riak.RiakWrapper;
//
///**
// * @author rst
// * Cache manager that stores information in the Riak cache.
// * Riak database is used to store and access the code
// * provided by clients. (Ensure Riak Server is running.) New requests
// * will offer client already available code in cache. If client agrees
// * that the version of the code offered is correct, server uses this code
// * without transmitting it over network. All servers will share same code, but still
// * there might be some race conditions when some servers will ask for the same code
// * at the same time, causing multiple code transmissions.
// * 
// * WARNING: FIXME: remove this when fixed/improved.
// * Currently this cache manager is in raw state, does not give any speed benefits 
// * and introduces quite huge overhead caused by storing all data in riak db, that looks quite slow.
// * IT IS NOT RECOMMENTED TO USE. 
// */
//public class RiakCacheManager extends CacheManagerAbstract 
//{
//	/**
//	 * Key to get creation date of the item.
//	 */
//	private static final String ITEM_CREATION_DATE = "cacheItemCreation";
//	/**
//	 * Key to get files that belong to the item. (Which files belong to this item?)
//	 */
//	private static final String ITEM_FILES = "cacheFiles";
//	
//	/**
//	 * Key to get classes with additional files that belong to this item. 
//	 */
//	private static final String ITEM_CLASSESWITHFILES = "cacheClassesWithFiles";
//	/**
//	 * Key to get set of classes that belong to the file.
//	 */
//	private static final String ITEMFILE_CLASSES = "cacheFileClasses";
//	/**
//	 * Key to get bytecode of the file.
//	 */
//	private static final String ITEMFILE_BYTECODE = "cacheFileBytecode";
//	/**
//	 * Key to get last modified and size of the file.
//	 */
//	private static final String ITEMFILE_ATTRIBUTES = "cacheFileAttribs";
//	/**
//	 * Key to get type of the content type of the file.
//	 */
//	private static final String ITEMFILE_CONTENT_TYPE = "cacheFileContentType";
//	/**
//	 * Key to get file that contains this class.
//	 */
//	private static final String ITEMCLASS_FILE = "cacheClassFile";
//	/**
//	 * Key to get items that this class belongs to.
//	 */
//	private static final String CLASS_ITEMS = "cacheClassItems";
//	
//	/** TODO: Do something with this lock idea, it's only temporal solution.
//	 * Key to get if the current item is locked or not.
//	 */
//	private static final String ITEM_LOCK="lock";
//
//	/**
//	 * Counts number of users for global cache. Global cache is deleted when there's no users.
//	 */
//	private RiakWrapper riakWrapper = null;
//	/**
//	 * Maps files to path where they are already deployed.
//	 */
//	private Map<String,String> fileKeyToLocationMap = new HashMap<String,String>();
//	private RiakCacheConfiguration cfg;
//	//-------------------------------------------------------------------------
//
//	RiakCacheManager(RiakCacheConfiguration configuration) 
//	{
//		super(configuration);
//		this.cfg = configuration;
//		//
//		// Connecting to Riak
//		//
//		try
//		{	
//			riakWrapper = JCloudScaleConfiguration.getConfiguration()
//							.server().keyValueStorage().getKeyValueStorageWrapper();
//		}
//		catch(JCloudScaleException ex)
//		{
//			throw new JCloudScaleException(ex, "Failed to open connection to Riak server.");
//		}
//	}
//	
//	//--------------------------------------------------------------------------------
//	
//	@XmlRootElement
//	@XmlAccessorType(XmlAccessType.FIELD)
//	static class RiakCacheConfiguration extends CacheConfiguration 
//	{
//		private static final long serialVersionUID = 1L;
//		
//		public RiakCacheConfiguration()
//		{
//			shareCache = false;
//		}
//		@Override
//		public ICacheManager createCacheManager() 
//		{
//			return new RiakCacheManager(this);
//		}
//	}
//	
//	//--------------------------------------------------------------------------------
//	/**
//	 * Verifies if the item is locked and ensures that we will access unlocked item.
//	 * @param itemName
//	 */
//	private void checkLock(String itemName)
//	{
//		final int waitMils = 200;
//		int lockTimeout = 100;//10 sec should be enough
//		 Random rnd = new Random();
//		//waiting on the lock, this item is locked.
//		while(lockTimeout > 0 && this.riakWrapper.containsValue(ITEM_LOCK, itemName))
//			try
//			{
//				Thread.sleep(rnd.nextInt(waitMils));
//				lockTimeout--;
//			}
//			catch(InterruptedException ex)
//			{ 
//				break;
//			}
//		
//		if(lockTimeout <= 0)
//		{
//			log.severe("Item "+itemName+" is locked, but timeout occured, breaking the lock");
//			riakWrapper.deleteValue(ITEM_LOCK, itemName);
//		}
//	}
//	
//	//----------------------------PUBLIC METHODS-------------------------------
//	
//	@Override
//	protected synchronized void cleanup()
//	{
//		if(this.cfg.shareCache)
//			return;
//		
//		RemoteClassLoaderUtils.deleteFolderRec(cacheLocation);
//		
//		//now we still have to go through deployed files set and remove all remaining.
//		for(String path : this.fileKeyToLocationMap.values())
//		{//TODO: if we created any folders, we still don't delete them
//			File file = new File(path);
//			if(!file.exists())
//				continue;
//			
//			if(!file.delete())
//				file.deleteOnExit();
//		}
//	}
//
//	@Override
//	public String[] getItemNames(String className) 
//	{
//		try 
//		{
//			return this.riakWrapper.getValue(CLASS_ITEMS, className, String[].class);
//		} 
//		catch (IOException e) 
//		{
//			log.severe("Failed to get item names for class "+className+":"+e.toString());
//			return null;
//		}
//	}
//
//	@Override
//	public String[] getClassesWithFiles(String itemName)
//	{
//		if(itemName == null || itemName.length() == 0)
//			return null;
//		try 
//		{
//			checkLock(itemName);
//			
//			return this.riakWrapper.getValue(ITEM_CLASSESWITHFILES, itemName, String[].class);
//		} 
//		catch (IOException e) 
//		{
//			log.severe("Failed to get classes with files for item "+itemName+":"+e.toString());
//			return null;
//		}
//	}
//	
//	@Override
//	public byte[] getClassBytecode(String className, String itemName) 
//	{
//		try 
//		{
//			checkLock(itemName);
//			
//			//
//			//getting file
//			//
//			String fileName = this.riakWrapper.getValue(ITEMCLASS_FILE, formKey(itemName, className), String.class);
//			if(fileName == null)
//			{
//				log.severe("bytecode for class "+className+" from "+itemName+" was requested, but file was not found.");
//				return null;
//			}
//			
//			//
//			//getting file type
//			//
//			String fileKey = formKey(itemName, fileName);
//			ContentType contentType = this.riakWrapper.getValue(ITEMFILE_CONTENT_TYPE, fileKey , ContentType.class);
//			
//			//
//			// getting class bytecode
//			//
//			if(contentType == ContentType.JAR)
//				return this.getBytecodeFromJar(getFileLocation(fileName, itemName), className);
//			else if(contentType == ContentType.CLASS)
//				return this.riakWrapper.getValue(ITEMFILE_BYTECODE, fileKey, byte[].class);
//			else 
//			{	//some shit is going on. This is not jar and not class.
//				log.severe("Trying to retrieve bytecode for class "+className+" from item "+itemName+" we received non-code file "+fileName);
//				throw new JCloudScaleException("Trying to retrieve bytecode for class "+className+" from item "+itemName+" we received non-code file.");
//			}
//		} 
//		catch (IOException e) 
//		{
//			log.severe("Failed to get bytecode of class "+className+" from "+itemName+": "+e.toString());
//			return null;
//		}
//	}
//
//	@Override
//	public synchronized String getFileLocation(String fileName, String itemName) 
//	{
//		try
//		{
//			//waiting on the lock, this item is locked.
//			checkLock(itemName);
//			
//			String fileKey = formKey(itemName, fileName);
//			
//			//
//			//if we have this item in cache folder already, we just return it.
//			//
//			if(fileKeyToLocationMap.containsKey(fileKey))
//				return fileKeyToLocationMap.get(fileKey);
//			
//			//
//			// getting properties of this item.
//			//
//			byte[] bytecode = riakWrapper.getValue(ITEMFILE_BYTECODE, fileKey, byte[].class);
//			long lastModified = riakWrapper.getValue(ITEMFILE_ATTRIBUTES, fileKey, long[].class)[0];//last modified is stored first and size second.
//			ContentType contentType = riakWrapper.getValue(ITEMFILE_CONTENT_TYPE, fileKey, ContentType.class);
//			
//			//
//			// selecting location for the file.
//			//
//			String filePath = convertToCachedFileName(cacheLocation.getPath(), fileName, contentType);
//			File file = new File(filePath);
//			if (file.exists())
//			{
//				if(!isCorrectFileAttributes(file, bytecode.length, lastModified))
//				{
//					filePath = convertToCachedFileName(cacheLocation.getPath(), 
//														UUID.randomUUID().toString()+"_"+fileName, 
//														contentType);
//					file = new File(filePath);
//				}
//			}
//			
//			createFile(file, bytecode, lastModified);
//			
//			fileKeyToLocationMap.put(fileKey, filePath);
//			
//			return file.getCanonicalPath();
//		}
//		catch(IOException ex)
//		{
//			log.severe("Failed to provide location for the file ("+fileName+") from "+itemName+" :"+ex.toString());
//			return null;
//		}
//	}
//
//	@Override
//	public ClassLoaderOffer createOfferByItemName(String itemName) //TODO: this code is QUITE similar to file cache manager, consider merging into cache manager abstract.
//	{
//		try
//		{
//			//getting file list and checking if offer exists.
//			String[] files = this.riakWrapper.getValue(ITEM_FILES, itemName, String[].class);
//			
//			if(files == null)
//				return null;//there's no such offer.
//
//			checkLock(itemName);
//			
//			//locking offer
//			this.riakWrapper.setValue(ITEM_LOCK, itemName, true);
//			
//			long itemCreated = this.riakWrapper.getValue(ITEM_CREATION_DATE, itemName, Long.TYPE);
//	
//			ClassLoaderOffer offer = new ClassLoaderOffer(itemName, itemCreated);
//			
//			// filling the file list for the offer.
//			List<ClassLoaderFile> offerFiles = new ArrayList<ClassLoaderFile>();
//			for(String fileName : files)
//			{
//				String fileKey = formKey(itemName, fileName);
//				
//				ContentType contentType = this.riakWrapper.getValue(ITEMFILE_CONTENT_TYPE, fileKey, ContentType.class);//this.itemFileToContentType.get(fileKey);
//				
//				long[] fileAttributes = this.riakWrapper.getValue(ITEMFILE_ATTRIBUTES, fileKey, long[].class);
//				long lastModified = fileAttributes[0];
//				long fileLength = fileAttributes[1];
//				
//				offerFiles.add(new ClassLoaderFile(fileName, lastModified, fileLength, contentType));
//			}
//			
//			offer.setFiles(offerFiles.toArray(new ClassLoaderFile[offerFiles.size()]));
//			
//			return offer;
//		}
//		catch(IOException ex)
//		{
//			log.severe("Failed to create offer with name "+itemName+" due to "+ex.toString());
//			return null;
//		}
//		finally
//		{
//			this.riakWrapper.deleteValue(ITEM_LOCK, itemName);
//		}
//	}
//
//	@Override
//	public synchronized void registerOffer(ClassLoaderOffer offer) //TODO: this code is QUITE similar to file cache manager, consider merging into cache manager abstract.
//	{
//		if(offer == null)
//			return;
//		
//		try
//		{
//			//checking whether we have this offer in the system.
//			String offerName = offer.getName();
//			
//			if(offerName == null || offerName.length() == 0 || 
//					!this.riakWrapper.containsValue(ITEM_CREATION_DATE, offerName))//this.itemToFilesMap.containsKey(offerName))
//			{//we don't have it, let's generate name and creation date.
//				offerName = UUID.randomUUID().toString();
//				offer.setName(offerName);
//				offer.setCreationDate(System.currentTimeMillis());
//			}
//			else
//				checkLock(offerName);
//
//			this.riakWrapper.setValue(ITEM_LOCK, offerName, true);
//			
//			//updating offer lastModified
//			//this.itemToCreationDateMap.put(offerName, offer.getCreationDate());
//			this.riakWrapper.setValue(ITEM_CREATION_DATE, offerName, offer.getCreationDate());
//			
//			//
//			// updating files that relate to this offer.
//			//
//			List<String> fileNames = new ArrayList<String>();
//			for(ClassLoaderFile file : offer.getFiles())
//			{
//				fileNames.add(file.getName());
//				String fileKey = formKey(offerName, file.getName());
//				
//				//
//				// saving file to local cache
//				//
//				String path = null;//we need it accessible here to be able to read classes from stored file.
//				if(file.getType() == ContentType.JAR)
//				{//if it is jar, we have to store it there anyways.
//					path = this.fileKeyToLocationMap.get(fileKey);
//					File pathFile = null;
//					if(path == null)
//					{//we don't have this file, we should create it.
//						path = convertToCachedFileName(this.cacheLocation.getPath(), file.getName(), file.getType());
//						pathFile = new File(path);
//						if(pathFile.exists())
//							if(!this.isCorrectFileAttributes(pathFile, file.getSize(), file.getLastModifiedDate()))
//							{//there is such file, but it's not our. we go for another one.
//								path = convertToCachedFileName(this.cacheLocation.getPath(), 
//														UUID.randomUUID().toString() + file.getName(), 
//														file.getType());
//								pathFile = new File(path);
//							}
//							else
//								pathFile = null; //if it is the same file, there's no need to update
//					}
//					else 
//						pathFile = new File(path);
//					
//					try
//					{
//						if(pathFile != null)
//						{
//							createFile(pathFile, file.getContent(), file.getLastModifiedDate());
//							fileKeyToLocationMap.put(fileKey, path);//just in case it is still not there.
//						}
//					}
//					catch(IOException ex)
//					{
//						this.log.severe("Failed to store provided file"+path+" to the drive:" + ex.toString());
//						throw new RuntimeException("Failed to store provided file on the drive:", ex);
//					}
//				}
//				
//				//
//				// storing the file information to global cache.
//				//
//				this.riakWrapper.setValue(ITEMFILE_ATTRIBUTES, fileKey, new long[]{file.getLastModifiedDate(), file.getSize()});
//				this.riakWrapper.setValue(ITEMFILE_CONTENT_TYPE, fileKey, file.getType());
//				this.riakWrapper.setValue(ITEMFILE_BYTECODE, fileKey, file.getContent());
//				
//				//
//				// Collecting classes from file
//				//
//				List<String> classesList = null;
//				switch(file.getType())
//				{
//				case CLASS:
//					classesList = Arrays.asList(file.getName());
//					break;
//				case JAR:
//					try 
//					{
//						classesList = collectClassesFromJar(path);
//						break;
//					} catch (IOException ex) 
//					{
//						throw new RuntimeException("Failed to collect classes from the provided jar "+file.getName()+" while registering offer "+offerName, ex);
//					}
//				default://other types do not have classes.
//					continue;
//				}
//				
//				//adding classes to global cache.
//				if(classesList != null)
//					this.riakWrapper.setValue(ITEMFILE_CLASSES, fileKey, 
//							classesList.toArray(new String[classesList.size()]));
//				//this.itemFileToClassesMap.put(fileKey, classesList.toArray(new String[classesList.size()]));
//				
//				//
//				// Registering classes within file
//				//
//				for(String className : classesList)
//				{
//					//referencing to file
//					this.riakWrapper.setValue(ITEMCLASS_FILE, formKey(offerName, className), file.getName());//this.itemClassToFileMap.put(formKey(offerName, className), file.getName());
//					
//					//updating the set of items that contain this file.
//					String[] updatedOwningItems = updateArray(this.riakWrapper.getValue(CLASS_ITEMS, className, String[].class),
//																new String[]{offerName});
//					if(updatedOwningItems != null)
//						this.riakWrapper.setValue(CLASS_ITEMS, className, updatedOwningItems);
//				}
//			}
//			
//			//
//			// Updating the set of classes that have files within this offer.
//			//
//			String[] updatedClassesWithFiles = updateArray(this.riakWrapper.getValue(ITEM_CLASSESWITHFILES, offerName, String[].class), 
//															offer.getClassesWithFiles());
//			if(updatedClassesWithFiles != null)
//				this.riakWrapper.setValue(ITEM_CLASSESWITHFILES, offerName, updatedClassesWithFiles);
//		
//			//
//			// Updating the set of files that belong to this offer
//			//
//			String[] updatedRegisteredFiles = updateArray(this.riakWrapper.getValue(ITEM_FILES, offerName, String[].class),
//															fileNames.toArray(new String[fileNames.size()]));
//			if(updatedRegisteredFiles != null)
//				this.riakWrapper.setValue(ITEM_FILES, offerName, updatedRegisteredFiles);
//		}
//		catch(IOException ex)
//		{
//			log.severe("Failed to register offer "+offer.getName()+": "+ex.toString());
//			//TODO: consider rolling back changes... simple remove offer may not do the trick.
//			throw new RuntimeException("Failed to register offer "+offer.getName(), ex);
//		}
//		finally
//		{
//			this.riakWrapper.deleteValue(ITEM_LOCK, offer.getName());
//		}
//	}
//	
//	@Override
//	public synchronized void removeOfferByName(String itemName) 
//	{
//		if(itemName == null || itemName.length() == 0)
//			return;
//		try
//		{
//			checkLock(itemName);
//			
//			//locking the item
//			this.riakWrapper.setValue(ITEM_LOCK, itemName, true);
//			
//			String[] files = this.riakWrapper.getValue(ITEM_FILES, itemName, String[].class);
//			if(files == null)
//				return;
//			
//			this.riakWrapper.deleteValue(ITEM_CREATION_DATE, itemName);
//			
//			for(String file : files)
//			{
//				try
//				{
//					String fileKey = formKey(itemName, file);
//					
//					//removing file from local cache
//					String filePath = this.fileKeyToLocationMap.remove(fileKey);
//					if(filePath != null)
//					{
//						File fileFile = new File(filePath);
//						if(!fileFile.delete())
//							fileFile.deleteOnExit();
//					}
//					
//					//removing from riak cache.
//					this.riakWrapper.deleteValue(ITEMFILE_BYTECODE, fileKey);
//					this.riakWrapper.deleteValue(ITEMFILE_ATTRIBUTES, fileKey);
//					this.riakWrapper.deleteValue(ITEMFILE_CONTENT_TYPE, fileKey);
//					
//					String[] classes = this.riakWrapper.getValue(ITEMFILE_CLASSES, fileKey, String[].class);
//					if(classes == null)
//						continue;
//					
//					this.riakWrapper.deleteValue(ITEMFILE_CLASSES, fileKey);
//					
//					//removing classes
//					for(String clazz : classes)
//					{
//						this.riakWrapper.deleteValue(ITEMCLASS_FILE, formKey(itemName, clazz));
//		
//						String[] items = this.riakWrapper.getValue(CLASS_ITEMS, clazz, String[].class);
//						
//						if(items == null)
//							continue;
//						
//						List<String> newItems = new ArrayList<String>(items.length-1);
//						for(String item : items)
//							if(!item.equals(itemName))
//								newItems.add(item);
//						
//						if(newItems.size() > 0)
//							this.riakWrapper.setValue(CLASS_ITEMS, clazz, newItems.toArray(new String[newItems.size()]));//this.classToItemsMap.put(clazz, newItems.toArray(new String[newItems.size()]));
//						else
//							this.riakWrapper.deleteValue(CLASS_ITEMS, clazz);
//					}
//				}
//				catch(JCloudScaleException ex)
//				{	//we do nothing and allow silently to delete all files (if it is possible)
//					log.severe("Failed to remove file "+file+" from offer "+itemName);
//				}
//			}
//		}
//		catch(Exception ex)
//		{
//			log.severe("Failed to remove offer "+itemName+": "+ex.toString());
////			throw new RuntimeException("Failed to remove offer "+itemName, ex);//we won't throw exceptions on unregister.
//		}
//		finally
//		{
//			this.riakWrapper.deleteValue(ITEM_LOCK, itemName);
//		}
//	}
//
//	@Override
//	public synchronized boolean deployFile(String fileName, String offerName) 
//	{
//		String fileKey = formKey(offerName, fileName);
//		try
//		{
//			checkLock(offerName);
//			
//			byte[] bytecode = this.riakWrapper.getValue(ITEMFILE_BYTECODE, fileKey, byte[].class);//this.itemFileToPathMap.get(fileKey);
//			if(bytecode == null)
//				return false;
//			
//			long lastModified = this.riakWrapper.getValue(ITEMFILE_ATTRIBUTES, fileKey, long[].class)[0];//the last modified goes first.
//		
//			//we do not check and write any file we're asked.
//			File file = new File(fileName);
//			RemoteClassLoaderUtils.mkDirRec(file.getParentFile());//creating required folders
//			createFile(file, bytecode, lastModified);
//			fileKeyToLocationMap.put(fileKey, file.getCanonicalPath());
//			
//			return true;
//		}
//		catch(IOException ex)
//		{
//			log.severe("Failed to deploy file " + fileName+":"+ex.toString());
//			return false;
//		}
//	}
//
//}
