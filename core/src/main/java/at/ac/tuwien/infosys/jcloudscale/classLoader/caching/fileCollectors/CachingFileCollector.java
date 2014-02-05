/*
   Copyright 2014 Philipp Leitner 

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
package at.ac.tuwien.infosys.jcloudscale.classLoader.caching.fileCollectors;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;

public class CachingFileCollector extends FileCollectorAbstract 
{	
	private FileCollectorAbstract fileCollector;
	private int cacheDepth;
	private Map<String, ClassLoaderOffer> cache;
	private Map<String, Long> lastUsedMap;
	
	private Object syncObject = new Object();
	
	public CachingFileCollector(){}
	
	public CachingFileCollector(FileCollectorAbstract fileCollector, int cacheDepth)
	{
		this.fileCollector = fileCollector;
		this.cacheDepth = cacheDepth;
		this.cache = new ConcurrentHashMap<>(this.cacheDepth+1);//just in case we will be overloaded
		this.lastUsedMap = new ConcurrentHashMap<>(this.cacheDepth+1);
	}
	
	public void setFileCollector(FileCollectorAbstract fileCollector) {
		this.fileCollector = fileCollector;
	}
	public void setCacheDepth(int cacheDepth) {
		this.cacheDepth = cacheDepth;
	}

	@Override
	public ClassLoaderOffer collectFilesForClass(String classname) 
	{
		try
		{
			ClassLoaderOffer offer = cache.get(classname);
			
			if(offer != null)
				return offer;
			
			// offer is null, we need to calculate new one.
			synchronized (syncObject) 
			{
				// checking again if nothing changed while we were waiting for lock
				offer = cache.get(classname);
				if(offer != null)
					return offer;
				
				offer = fileCollector.collectFilesForClass(classname);
				if(offer == null)
					return null;
				
				cache.put(classname, offer);
				
				optimizeCacheDepth();
			}
			
			return offer;
		}
		finally
		{
			this.lastUsedMap.put(classname, System.nanoTime());// updating last used time
		}
	}

	private void optimizeCacheDepth() 
	{
		if(this.cache.size() <= cacheDepth)
			return;
			
		Entry<String, Long> oldestEntry = null;
		
		for(Entry<String, Long> entry : this.lastUsedMap.entrySet())
			if(oldestEntry == null || oldestEntry.getValue() > entry.getValue())
				oldestEntry = entry;
		
		this.cache.remove(oldestEntry.getKey());
		this.lastUsedMap.remove(oldestEntry.getKey());
	}
}
