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
package at.ac.tuwien.infosys.jcloudscale.vm;

import java.io.Closeable;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import at.ac.tuwien.infosys.jcloudscale.migration.IMigrationEnabledHostPool;

public interface IVirtualHostPool extends IMigrationEnabledHostPool, Closeable 
{
	
	public UUID deployCloudObject(IVirtualHost host, ClientCloudObject cloudObject, Object[] args, Class<?>[] paramTypes);
	
	public void destroyCloudObject(UUID cloudObjectId);
	
	public int countCloudObjects();
	
	public Set<UUID> getCloudObjects();
	
	public ReentrantReadWriteLock getCOLock(UUID cloudObjectId);
	
	public IVirtualHost findManagingHost(UUID cloudObjectId);
	
	public Collection<IVirtualHost> getVirtualHosts();
	
	public void shutdownHost(UUID hostId);//TODO: needed only for UIConnector
	
	@Override
	public void close();

}
