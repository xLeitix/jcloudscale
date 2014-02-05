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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;

/**
 * @author rst
 * Represents Cache manager stub that does not provide any cache.
 * Actually, extends FileCacheManager to not give any offers and not do any stupid work.
 */
public class NoCacheManager extends FileCacheManager
{
	public NoCacheManager(NoCacheConfiguration configuration) 
	{
		super(configuration);
	}
	
	//---------------------------------CONFIGURATION--------------------------------------------------
	
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class NoCacheConfiguration extends FileCacheConfiguration
	{
		private static final long serialVersionUID = 1L;

		public NoCacheConfiguration()
		{
			this.shareCache = false;
		}
		
		@Override
		public ICacheManager createCacheManager() 
		{
			return new NoCacheManager(this);
		}
	}
	
	//-------------------Overriding methods to remove implementation--------------------------------

	@Override
	protected void loadIndex(){}
	
	@Override
	protected void saveIndex(){}

	@Override
	public synchronized String[] getItemNames(String className)
	{
		return null;
	}
	
	@Override
	public synchronized ClassLoaderOffer createOfferByItemName(String itemName)
	{
		return null;
	}
}
