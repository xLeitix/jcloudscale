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
package at.ac.tuwien.infosys.jcloudscale.classLoader.caching.fileCollectors;



import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;

/**
 * @author rst
 * This class collection does not collect anything and can be used to log classes that 
 * were not found on the server and, therefore, requested from the client. 
 */
public class StubFileCollector extends FileCollectorAbstract 
{
	
	@Override
	public ClassLoaderOffer collectFilesForClass(String classname) 
	{
		log.info("Class "+classname+" was requested, but current file collector will not provide any response!");
		return null;
	}
}
