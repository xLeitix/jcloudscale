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

/**
 * @author rst
 *	The type of the cache to use in Caching classloader.
 */
public enum CacheType 
{
	/**
	 * Don't use any cache. On each missing class servers will ask 
	 * for the code from the client. 
	 */
	NoCache,
	/**
	 * Use File Cache. Special folder will be created on the server and 
	 * all code provided by clients will be stored there. New requests
	 * will offer client already available code in cache. If client agrees
	 * that the version of the code offered is correct, server uses this code
	 * without transmitting it over network. However, note that cache is local to
	 * the machine and each machine will maintain its own cache. 
	 * (So, client will have to send same code as many times as there are machines in setup.) 
	 */
	FileCache,
	/**
	 * Use Riak Server for the cache. Riak database will be used to store and access the code
	 * provided by clients. (Ensure Riak Server is running.) New requests
	 * will offer client already available code in cache. If client agrees
	 * that the version of the code offered is correct, server uses this code
	 * without transmitting it over network. All servers will share same code, but still
	 * there might be some race conditions when some servers will ask for the same code
	 * at the same time, causing multiple code transmissions.
	 */
	RiakCache
}
