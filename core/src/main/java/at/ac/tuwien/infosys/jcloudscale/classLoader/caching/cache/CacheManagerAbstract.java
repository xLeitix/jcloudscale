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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import at.ac.tuwien.infosys.jcloudscale.classLoader.RemoteClassLoaderUtils;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ContentType;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;

public abstract class CacheManagerAbstract implements ICacheManager 
{
	protected File cacheLocation = null;
	protected Logger log;
	
	//----------------------PROTECTED SHARED METHODS--------------
	
	protected CacheManagerAbstract(CacheConfiguration cfg)
	{
		this.log = JCloudScaleConfiguration.getLogger(this);
		
		this.cacheLocation = cfg.shareCache ? 
				new File(cfg.cacheFolder) : 
				new File(cfg.cacheFolder + File.separator + cfg.id);

		if (!this.cacheLocation.exists())
		try
		{
		if (!RemoteClassLoaderUtils.mkDirRec(this.cacheLocation.getCanonicalFile()))
			throw new JCloudScaleException("Failed to create cache directory: " + this.cacheLocation.getCanonicalFile());
		}
		catch (IOException e)
		{
			this.log.severe("Cache failed to create folder: "+e.toString());
			throw new JCloudScaleException(e, "Cache failed to create folder");
		}
	}
	
	protected List<String> collectClassesFromJar(String jarFilename) throws IOException
	{
		List<String> classes = new ArrayList<String>();
		
		try(JarFile jarFile = new JarFile(jarFilename))
		{
			// collecting classes
			final Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements())
			{
				final JarEntry entry = entries.nextElement();
				final String entryName = entry.getName();
				if (!RemoteClassLoaderUtils.isClassName(entryName))
					continue;
				
				String className = RemoteClassLoaderUtils.convertClassPathToClassName(entryName);
				classes.add(className);
			}
		}
		
		return classes;
	}
	
	/**
	 * Gets bytecode for the specified class from the jar stored as a file.
	 */
	protected byte[] getBytecodeFromJar(String jarFilename, String className) throws IOException
	{
		try(JarFile file = new JarFile(jarFilename))
		{
			JarEntry entry = file.getJarEntry(RemoteClassLoaderUtils.convertClassToPath(className));
			return RemoteClassLoaderUtils.getByteArray(file.getInputStream(entry));
		}
	}
	/**
	 * Creates file on the specified location with the specified content.
	 * @param filePath The File that specified the path where to create file.
	 * @param bytecode The content of the file.
	 * @param lastModified The last modified date of the file.
	 * @throws IOException If writing failed.
	 */
	protected void createFile(File filePath, byte[] bytecode, long lastModified) throws IOException
	{
		try
		{
			try(RandomAccessFile writer = new RandomAccessFile(filePath, "rw"))
			{
				writer.write(bytecode);
			}

			if(!filePath.setLastModified(lastModified))
				log.warning("Failed to set lastModified to "+lastModified+" for file "+filePath.getAbsolutePath());
		}
		catch (FileNotFoundException ex)
		{
//			Logger.getAnonymousLogger().severe(System.getProperty("user.dir"));
//			Logger.getAnonymousLogger().severe(filePath.getCanonicalPath());
			ex.printStackTrace();
		}
	}
	
	/**
	 * Verifies if the provided file has specified size and last modified date.
	 */
	protected boolean isCorrectFileAttributes(File file, long length, long lastModified) 
	{
		return file.length() == length && file.lastModified() == lastModified;
	}
	
	/**
	 * Forms a key from the 2 provided substrings (item and file/class).
	 * @param itemName The name of the item that should form the key.
	 * @param subElement The sub-item element (file or class) that should form a key with item.
	 * @return The merged key.
	 */
	protected String formKey(String itemName, String subElement) 
	{
		return itemName+subElement;
	}
	
	/**
	 * Converts the fileName to the path name that can be stored in the cache.
	 * @param fileName
	 * @return
	 */
	protected String convertToCachedFileName(String cacheLocation, String fileName, ContentType contentType)
	{
		switch(contentType)
		{
		case CLASS:
			return cacheLocation + File.separator + fileName;
		case JAR:
			return cacheLocation + File.separator + fileName;
			
		case ROFILE:
//		case RWFILE:
			return cacheLocation + File.separator + RemoteClassLoaderUtils.escapeFilePath(fileName);
		
		default:
			return cacheLocation + File.separator + fileName;
		}
	}
	
	/**
	 * Updates the array with new values. If the array was actually changed, 
	 * returns the new array. Otherwise, returns null. 
	 * @param existingArray
	 * The array that should be updated.
	 * @param newValues
	 * The new values that should be added.
	 * @return
	 * The new, updated, array that contains new values or null if array was not changed.
	 */
	protected String[] updateArray(String[] existingArray, String[] newValues)
	{
		//if we have no new values, nothing to update.
		if(newValues == null || newValues.length == 0)
			return null;
		
		//if we have no old values, the new values are update.
		if(existingArray == null || existingArray.length == 0)
			return newValues;
		
		//easy cases out, now we have to update.
		boolean newFilesAdded = false;
		
		//checking if there's new files to add.
		Set<String> existingSet = new LinkedHashSet<String>(Arrays.asList(existingArray));
		
		for(String value : newValues)
			if(!existingSet.contains(value))
			{
				existingSet.add(value);
				newFilesAdded = true;
			}
		
		if(newFilesAdded)
			return existingSet.toArray(new String[existingSet.size()]);
		
		return null;//no new files added.
	}
	
	//----------------------PUBLIC INTERFACE----------------------
	
	@Override
	public void close()
	{
		if(CacheConfiguration.isGlobalCache(this) && CacheConfiguration.getGlobalCacheReferenceCount() > 0)
		{
			CacheConfiguration.stopUsingGlobalCache();
		}
		else 
		{	 // this is private cache, we can close and clean up.
			cleanup();
		}
	}
	
	@Override
	public boolean isClassLoadedFromCache(Class<?> clazz)
	{
		if(cacheLocation == null)
			return false;//we cannot detect where it is from.
		
		String file = RemoteClassLoaderUtils.getContainingFile(clazz);
		boolean res = (file == null || file.toLowerCase().contains(cacheLocation.getName().toLowerCase()));
		return res;
	}
	
	//----------------------------------------------------------

	/**
	 * Cleans all cache related resources before closing.
	 */
	protected abstract void cleanup();

}
