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
package at.ac.tuwien.infosys.jcloudscale.vm.bursting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.migration.IMigrationEnabledVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.migration.MigrationExecutor;
import at.ac.tuwien.infosys.jcloudscale.migration.MigrationReason;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHostPool;
import at.ac.tuwien.infosys.jcloudscale.vm.VirtualHostPool;

public class AggregatedVirtualHostPool implements IVirtualHostPool 
{
	private List<IVirtualHostPool> virtualHostSubPools = new ArrayList<>();
	
	public AggregatedVirtualHostPool(AggregatedCloudPlatformConfiguration config) 
	{
		for(CloudPlatformConfiguration cfg : config.getCloudPlatforms())
			virtualHostSubPools.add(cfg.getVirtualHostPool());
	}
	
	public List<IVirtualHostPool> getVirtualHostSubPools()
	{
		return Collections.unmodifiableList(virtualHostSubPools);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<IHost> getHosts() 
	{
		return (Collection<IHost>)(Collection<?>)getVirtualHosts();
	}
	
	@Override
	public Collection<IVirtualHost> getVirtualHosts() {
		List<IVirtualHost> hosts = new ArrayList<>();
		
		for(IVirtualHostPool pool : virtualHostSubPools)
			hosts.addAll(pool.getVirtualHosts());
			
		return Collections.unmodifiableList(hosts);
	}

	@Override
	public int getHostsCount() {
		int res = 0;
		
		for(IVirtualHostPool hostPool : virtualHostSubPools)
			res+= hostPool.getHostsCount();
		
		return res;
	}

	@Override
	public IHost startNewHost() 
	{//TODO: we were talking about priorities...
		throw new JCloudScaleException("Cannot start new host on Aggregated Virtual Host Pool. Start on one of the sub-pools");
	}

	@Override
	public IHost startNewHostAsync() 
	{
		throw new JCloudScaleException("Cannot start new host on Aggregated Virtual Host Pool. Start on one of the sub-pools");
	}

	@Override
	public IHost startNewHostAsync(IHostStartedCallback afterHostStarted) 
	{
		throw new JCloudScaleException("Cannot start new host on Aggregated Virtual Host Pool. Start on one of the sub-pools");
	}
	
	@Override
	public IHost startNewHost(String arg0) {
		throw new JCloudScaleException("Cannot start new host on Aggregated Virtual Host Pool. Start on one of the sub-pools");
	}

	@Override
	public IHost startNewHostAsync(String arg0) {
		throw new JCloudScaleException("Cannot start new host on Aggregated Virtual Host Pool. Start on one of the sub-pools");
	}

	@Override
	public IHost startNewHostAsync(IHostStartedCallback arg0, String arg1) {
		throw new JCloudScaleException("Cannot start new host on Aggregated Virtual Host Pool. Start on one of the sub-pools");
	}
	
	private IVirtualHostPool findHostPoolForHost(IHost knownHost)
	{//cannot use uuid here as it is possible that we have to find the host that does not have uuid yet.
		for(IVirtualHostPool pool : virtualHostSubPools)
			for(IVirtualHost host : pool.getVirtualHosts())
				if(host == knownHost)
					return pool;
		
		return null;
	}
	
	private IVirtualHostPool findHostPoolForHost(UUID knownHostId)
	{
		for(IVirtualHostPool pool : virtualHostSubPools)
			for(IVirtualHost host : pool.getVirtualHosts())
				if(host.getId().equals(knownHostId))
					return pool;
		
		return null;
	}

	@Override
	public void shutdownHostAsync(IHost host) 
	{
		if(host == null)
			return;
		
		IVirtualHostPool hostPool = findHostPoolForHost(host);
		if(hostPool == null)
			return;
		
		hostPool.shutdownHostAsync(host);
	}
	
	@Override
	public void shutdownHost(UUID hostId) 
	{
		if(hostId == null)
			return;
		
		IVirtualHostPool hostPool = findHostPoolForHost(hostId);
		if(hostPool == null)
			return;
		
		hostPool.shutdownHost(hostId);
	}

	@Override
	public UUID deployCloudObject(IVirtualHost host, ClientCloudObject cloudObject, Object[] args, Class<?>[] paramTypes) 
	{
		if(host == null)
			return null;
		
		IVirtualHostPool hostPool = findHostPoolForHost(host);
		if(hostPool == null)
			return null;
		
		return hostPool.deployCloudObject(host, cloudObject, args, paramTypes);
	}

	@Override
	public void destroyCloudObject(UUID cloudObjectId) 
	{
		for(IVirtualHostPool pool : virtualHostSubPools)
			if(pool.findManagingHost(cloudObjectId) != null)
				pool.destroyCloudObject(cloudObjectId);
	}

	@Override
	public int countCloudObjects() 
	{
		int res = 0;
		
		for(IVirtualHostPool pool : virtualHostSubPools)
			res += pool.countCloudObjects();
		
		return res;
	}

	@Override
	public Set<UUID> getCloudObjects() 
	{
		Set<UUID> res = new HashSet<>();
		
		for(IVirtualHostPool pool: virtualHostSubPools)
			res.addAll(pool.getCloudObjects());
		
		return res;
	}

	@Override
	public ReentrantReadWriteLock getCOLock(UUID cloudObjectId) 
	{
		for(IVirtualHostPool pool : virtualHostSubPools)
		{
			ReentrantReadWriteLock lock = pool.getCOLock(cloudObjectId);
			if(lock != null)
				return lock;
		}
		
		return null;
	}

	@Override
	public IVirtualHost findManagingHost(UUID cloudObjectId) 
	{
		for(IVirtualHostPool pool : virtualHostSubPools)
		{
			IVirtualHost host = pool.findManagingHost(cloudObjectId);
			if(host != null)
				return host;
		}
		
		return null;
	}

	@Override
	public void close() 
	{
		for(IVirtualHostPool pool : virtualHostSubPools)
			pool.close();
		
		virtualHostSubPools.clear();
	}

	@Override
	public ClientCloudObject getCloudObjectById(UUID uuid) {
		for(IVirtualHostPool pool : virtualHostSubPools) {
			ClientCloudObject cco = pool.getCloudObjectById(uuid);
			if(cco != null)
				return cco;
		}
		return null;
	}

	@Override
	public IHost getHostById(UUID uuid) {
		IHostPool pool = findHostPoolForHost(uuid);
		return pool.getHostById(uuid);
	}

	@Override
	public void migrateObject(UUID uuid, IHost targetHost) {
//		IMigrationEnabledVirtualHost sourceHost = (IMigrationEnabledVirtualHost)
//			findManagingHost(uuid);
//		findHostPoolForHost(sourceHost).migrateObject(uuid, targetHost);
		
		IMigrationEnabledVirtualHost sourceHost = (IMigrationEnabledVirtualHost)
				findManagingHost(uuid);
		VirtualHostPool sourcePool = (VirtualHostPool) findHostPoolForHost(sourceHost);
		VirtualHostPool targetPool = (VirtualHostPool) findHostPoolForHost(targetHost);
		
		MigrationExecutor mig = new MigrationExecutor(sourcePool, targetPool);
		mig.migrateObjectToHost(uuid, (IMigrationEnabledVirtualHost) targetHost, new MigrationReason());
		
	}

	@Override
	public void migrateObjectAsync(UUID uuid, IHost targetHost) {
		IHost origHost = findManagingHost(uuid);
		findHostPoolForHost(origHost).migrateObjectAsync(uuid, targetHost);
	}

	@Override
	public void migrateObjectAsync(UUID uuid, IHost targetHost,
			IObjectMigratedCallback callback) {
		IHost origHost = findManagingHost(uuid);
		findHostPoolForHost(origHost).migrateObjectAsync(uuid, targetHost, callback);
	}

}
