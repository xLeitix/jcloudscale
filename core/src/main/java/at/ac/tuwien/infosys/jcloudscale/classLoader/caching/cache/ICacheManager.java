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

import java.io.Closeable;

import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;

public interface ICacheManager extends Closeable
{
	/**
	 * Gets the value indicating if the class was loaded from cache (<b>true</b>)
	 * or from the classpath of the application (<b>false</b>).
	 * @param clazz The class to determine loading source.
	 * @return <b>true</b> if class was loaded from the cache. Otherwise -- false.
	 */
	public boolean isClassLoadedFromCache(Class<?> clazz);
	
	/**
	 * Gets the name of the items that contain provided class.
	 * 
	 * @param className
	 *            The name of the class to get items for.
	 * @return The collection of the item names that contain this class.
	 * If the class is not registered, null.
	 */
	public String[] getItemNames(String className);
	
	/**
	 * Gets the list of classes that have additional files within the item.
	 * @param itemName The name of the item that classes with additional files should be related to.
	 * @return The list of the classes that have items within specified item. 
	 * If the offer is not registered or no files within this offer, returns null.
	 */
	public String[] getClassesWithFiles(String itemName);
	
	/**
	 * Gets the bytecode of the specified class if it is registered in the cache.
	 * 
	 * @param className
	 *            The name of the class to retrieve bytecode for.
	 * @param itemName
	 * 			  The name of the item that should contain the specified class
	 * @return The bytecode that represents the specified class. 
	 * If the class cannot be found, <b>null</b>
	 */
	public byte[] getClassBytecode(String className, String itemName);
	
	/** 
	 * @param fileName
	 *            The name of the file to get location for.
	 * @param itemName
	 * 			  The name of the item that should contain this file.
	 * @return the location of the file with the content required for the speicified item.
	 */
	public String getFileLocation(String fileName, String itemName);
	
	/**
	 * Creates offer that corresponds to the specified item name
	 * @param itemName The name of the item that offer should correspond to.
	 * @return
	 * The classLoaderOffer class that contains all required files for this item, 
	 * but without content.
	 */
	public ClassLoaderOffer createOfferByItemName(String itemName);
	
	/**
	 * Removes the offer with the specified name from the cache.
	 * @param itemName
	 */
	public void removeOfferByName(String itemName);
	
	/**
	 * Updates or registers new offer with the specified in offer parameters.
	 * @param offer The offer that should be updated or registered.
	 */
	public void registerOffer(ClassLoaderOffer offer);
	
	/**
	 * Deploys the specified file from the cache to the correct location.
	 * @param fileName The name of the file to deploy
	 * @param offerName The name of the offer where this file was registered.
	 * @return <b>true</b> if file was registered successfully. Otherwise, 
	 * (if exception occurred) -- <b>false</b>.
	 * Question is what to do if such file exists and it is different from ours.
	 * Now we overwrite. 
	 */
	public boolean deployFile(String fileName, String offerName);
	
	@Override
	public void close();
}
