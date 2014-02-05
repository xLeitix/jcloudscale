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

import java.io.Serializable;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class CacheConfiguration implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	String cacheFolder = "classLoaderCache";
	boolean shareCache = true;
	UUID id = UUID.randomUUID();
	
	public void setCacheFolder(String cacheFolder) {
		this.cacheFolder = cacheFolder;
	}
	
	public void setShareCache(boolean shareCache) {
		this.shareCache = shareCache;
	}
	
	public ICacheManager getCacheManager()
	{
		if(shareCache)
			return getGlobalCache(this);
		else
			return createCacheManager();
	}
	
	abstract ICacheManager createCacheManager();
	
	public static CacheConfiguration createCacheConfig(CacheType cacheType, boolean shareCache)
	{
		CacheConfiguration config = null;
		switch(cacheType)
		{
			case FileCache:
				config = new FileCacheManager.FileCacheConfiguration();
				break;
			case NoCache:
				config = new NoCacheManager.NoCacheConfiguration();
				break;
//			case RiakCache:
//				config = new RiakCacheManager.RiakCacheConfiguration();
//				break;
			default:
				throw new RuntimeException("Unexpected cache type!");
		}
		
		config.setShareCache(shareCache);
		return config;
	}
	
	//----------------------GLOBAL CACHE MANAGEMENT----------------------------------
	
	private static volatile ICacheManager globalCache;
	private static volatile int globalCacheReferenceCounter = 0;
	
	/**
	 * Determines whether provided cache is a global cache object.
	 * @param cache The cache to verify
	 * @return <b>true</b> if the provided cache is a global cache. 
	 * <b>false</b> if it is a private cache. 
	 */
	static boolean isGlobalCache(ICacheManager cache)
	{
		return globalCache == cache;
	}

	/**
	 * Gets the global cache or creates if it is missing.
	 */
	private static synchronized ICacheManager getGlobalCache(CacheConfiguration configuration)
	{
		if(globalCache == null)
			globalCache = configuration.createCacheManager();
		
		globalCacheReferenceCounter++;
		return globalCache;
	}
	
	/**
	 * Closes global cache and disposes all resources occupied by it.
	 */
	private static synchronized void closeGlobalCache()
	{
		if (globalCache == null)
			return;
		
		globalCache.close();
		globalCache = null;
		globalCacheReferenceCounter = 0;
	}
	
	/**
	 * Signals to CacheManager that this object won't use global cache and CacheManager
	 * can decrement global cache users count by 1. If there will be no more global cache users,
	 * global cache will be closed automatically.
	 */
	static synchronized void stopUsingGlobalCache()
	{
		globalCacheReferenceCounter--;
		if(globalCacheReferenceCounter <= 0)
			closeGlobalCache();
	}
	
	/**
	 * Gets the count of global cache references.
	 * @return amount of registered references to global cache.
	 */
	static synchronized int getGlobalCacheReferenceCount()
	{
		return globalCacheReferenceCounter;
	}
}
