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
package at.ac.tuwien.infosys.jcloudscale.vm;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.logging.Logged;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.migration.IMigrationEnabledVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.migration.MigrationExecutor;
import at.ac.tuwien.infosys.jcloudscale.migration.MigrationReason;

@Logged
public class VirtualHostPool implements IVirtualHostPool {
	
	private Logger log;
	
	private List<IVirtualHost> hostsPool = null;
	private Map<UUID, IVirtualHost> idToHostMap = null;
	protected Map<UUID, UUID> objectMappings = null;
	private CloudPlatformConfiguration config = null;

	private Map<UUID, ReentrantReadWriteLock> objectLocks;
	private ExecutorService threadpool;
	private IdManager idManager;
	
	private COKeepAliveTask keepaliveTask;
	
	private MigrationExecutor migExecutor;
	
	public VirtualHostPool(CloudPlatformConfiguration config, MessageQueueConfiguration mqConfig) {
		
		this.config = config;
		this.log = JCloudScaleConfiguration.getLogger(this);
		this.idManager = new IdManager(mqConfig);
		
		//this is thread-safe version of ArrayList that is fine if we don't change it all the time.
		hostsPool = new CopyOnWriteArrayList<>();
		idToHostMap = new Hashtable<UUID, IVirtualHost>();
		objectMappings = new Hashtable<UUID, UUID>();
		objectLocks = new Hashtable<UUID, ReentrantReadWriteLock>();
		
		migExecutor = new MigrationExecutor(this, this);
		
		threadpool = Executors.newCachedThreadPool();
		
		keepaliveTask = new COKeepAliveTask();
	}
	
	public CloudPlatformConfiguration getConfiguration() {
		return config;
	}
	
	@Override
	public void close()
	{
		// waiting for all parallel requests to finish.
		if(threadpool != null)
		{
			threadpool.shutdown();
			try 
			{
				threadpool.awaitTermination(60, TimeUnit.SECONDS);
			} catch (InterruptedException e) 
			{
			}
			threadpool = null;
		}
		
		if(this.idManager != null)
		{
			this.idManager.close();
			this.idManager = null;
		}
		
		keepaliveTask.close();
		
		// shutting down everything explicitly.
		shutdownAll();
	}

	@Override
	public UUID deployCloudObject(IVirtualHost host, ClientCloudObject cloudObject, Object[] args, Class<?>[] paramTypes)
	{
		UUID id = host.deployCloudObject(cloudObject, args, paramTypes);
		objectLocks.put(id, new ReentrantReadWriteLock(true));
		objectMappings.put(id, host.getId());
		return id;
	}

	@Override
	public void destroyCloudObject(UUID cloudObjectId)
	{
		ReentrantReadWriteLock rwl = objectLocks.get(cloudObjectId);

		rwl.readLock().lock();
		try
		{
			IVirtualHost host = findManagingHost(cloudObjectId);
			host.destroyCloudObject(cloudObjectId);
			objectMappings.remove(cloudObjectId);
			objectLocks.remove(cloudObjectId);
		}
		finally
		{
			rwl.readLock().unlock();
		}
	}

	private void shutdownAll() 
	{
		for (IVirtualHost host : hostsPool)
			shutdownHost(host);
	}

	@Override
	public void shutdownHost(UUID hostId)//TODO: needed only for UIConnector
	{
		IVirtualHost host = idToHostMap.remove(hostId);
		hostsPool.remove(host);
		
		shutdownHost(host);
	}
	/**
	 * Destroys and removes the host with the specified id from the hosts pool.
	 * @param toRemove The id of the host to remove.
	 */
	private void shutdownHost(IVirtualHost host) 
	{
		if(host == null)
			return;
		
		try
		{	
			// Remove the host from the pool
			hostsPool.remove(host);
			
			if(host.getCloudObjectsCount() > 0)
			{
				log.warning("Asked to remove host "+host.getId()+
						", but there are still "+host.getManagedObjectIds().size()+" cloud objects mapped to it. Removing objects first...");
				
				for(UUID objId : new ArrayList<>(host.getManagedObjectIds()))//TODO: should we lock here on object reentrant lock?
				{
					objectMappings.remove(objId);
					objectLocks.remove(objId);
					try
					{
					    host.destroyCloudObject(objId);
					}
					catch(Exception ex)
					{
					    log.severe("Failed to destroy cloud object "+objId+" deployed on host "
					                        +host.getId()+" ("+host.getIpAddress()+"): "+ex);
					    ex.printStackTrace();
					}
				}
			}
		}
		finally
		{
			host.close();
		}
	}
	
	@Override
	public IVirtualHost findManagingHost(UUID cloudObjectId) {
		UUID id = objectMappings.get(cloudObjectId);
		if(id == null)
			return null;
		return idToHostMap.get(id);
	}
	
	@Override
	public Iterable<IHost> getHosts() {
		return Collections.unmodifiableCollection(new ArrayList<IHost>(this.hostsPool));
	}
	
	@Override
	public Collection<IVirtualHost> getVirtualHosts() {
		return Collections.unmodifiableCollection(this.hostsPool);
	}
	
	@Override
	public int getHostsCount() {
		return this.hostsPool.size();
	}
	
	@Override
	public int countCloudObjects() {
		return objectMappings.entrySet().size();
	}

	@Override
	public Set<UUID> getCloudObjects() {
		return objectMappings.keySet();
	}
	
	@Override
	public ClientCloudObject getCloudObjectById(UUID objectId) {
		IVirtualHost host = findManagingHost(objectId);
		
		if(host == null)
			return null;
		
		return host.getCloudObjectById(objectId);
	}

	@Override
	public IHost getHostById(UUID hostId) {
		return this.idToHostMap.get(hostId);
	}

	/**
	 * Returns the {@link ReentrantReadWriteLock} for the given {@link CloudObject}.
	 * 
	 * @param cloudObjectId
	 *            The id of the CloudObject
	 * @return The <code>ReentrantReadWriteLock</code> or <i>null</i> if the
	 *         CloudObject doesn't exist.
	 */
	@Override
	public ReentrantReadWriteLock getCOLock(UUID cloudObjectId) {
		return objectLocks.get(cloudObjectId);
	}

	@Override
	public IVirtualHost startNewHost() 
	{
		return startNewHost(null);
	}
	
	@Override
	public IVirtualHost startNewHost(String size) 
	{
		IVirtualHost host = config.getVirtualHost(this.idManager);
		
		// trying to add host
		try
		{
			hostsPool.add(host);//adding host prior to startup to show everyone else that we have 1 more host.
			host.startupHost(this, size);
			idToHostMap.put(host.getId(), host);
		}
		catch(Exception ex)
		{
			//TODO: should we try to shutdown host in case we failed to start to clean resources?
//			shutdownHost(host); 
			hostsPool.remove(host);//we have to remove the host to not have reference to faulty host.
			throw ex;
		}
		
		return host;
	}
	
	@Override
	public IHost startNewHostAsync() 
	{
		return startNewHostAsync((IHostStartedCallback) null);
	}
	
	@Override
	public IHost startNewHostAsync(String size) 
	{
		return startNewHostAsync(null, size);
	}
	
	@Override
	public IHost startNewHostAsync(final IHostStartedCallback hostStartedCallback) 
	{
		return startNewHostAsync(hostStartedCallback, null);
	}
	
	@Override
	public IHost startNewHostAsync(final IHostStartedCallback hostStartedCallback, String size) 
	{
		final IVirtualHost host = config.getVirtualHost(this.idManager);
		hostsPool.add(host); //adding host prior to startup to show everyone else that we have 1 more host.
		
		threadpool.execute(new Runnable() {
			@Override
			public void run() 
			{
				try
				{
					host.startupHost(VirtualHostPool.this, null);
					idToHostMap.put(host.getId(), host);
					
					if(hostStartedCallback != null)
						hostStartedCallback.startupFinished(host);
				}
				catch(Exception ex)
				{
					log.severe("Exception while starting new host: "+ex);
					hostsPool.remove(host);
					throw ex;
				}
			}
		});
		
		return host;
	}
	
	@Override
	public void shutdownHostAsync(final IHost host) 
	{
		//remove host from hosts pool to not schedule something else on it in parallel.
		hostsPool.remove(host);
		
		if(threadpool.isShutdown()) {
			log.warning(String.format("Asked for termination of host %s, but shutdown executor is already closed",
					host.getId().toString()));
		}
		else {
			threadpool.execute(new Runnable() {
				@Override
				public void run() 
				{
					shutdownHost((IVirtualHost)host);
				}
			});
		}
	}
	
	@Override
	public void migrateObject(UUID object, IHost targetHost) {
		
		if(!(targetHost instanceof IMigrationEnabledVirtualHost))
			throw new JCloudScaleException("Cannot migrate object "+object.toString()+" to host "+
					targetHost.getId().toString()+", as this is not a migration-enabled host");
		
		migExecutor.migrateObjectToHost(object, (IMigrationEnabledVirtualHost) targetHost, new MigrationReason());
		
		log.info("Destroyed old cloud object and updated refs");
		
	}

	@Override
	public void migrateObjectAsync(final UUID object, final IHost targetHost) {
		
		threadpool.execute(new Runnable() {
			@Override
			public void run() 
			{
				try
				{
					migrateObject(object, targetHost);
				}
				catch(Exception ex)
				{
					log.severe("Exception while migrating object: "+ex);
					throw ex;
				}
			}
		});
		
	}
	
	@Override
	public void migrateObjectAsync(final UUID object, final IHost targetHost,
			final IObjectMigratedCallback callback) {
		
		threadpool.execute(new Runnable() {
			@Override
			public void run() 
			{
				try
				{
					migrateObject(object, targetHost);
					if(callback != null)
						callback.migrationFinished();
				}
				catch(Exception ex)
				{
					log.severe("Exception while migrating object: "+ex);
					throw ex;
				}
			}
		});
		
	}
	
	public void removeCOFromPool(IMigrationEnabledVirtualHost sourceHost,
			UUID cloudObjectId, ReentrantReadWriteLock coLock) {
		if (!coLock.isWriteLocked()) {
			coLock.writeLock().lock();
		}
		sourceHost.removeCloudObject(cloudObjectId);
		objectMappings.remove(cloudObjectId);
	}
	
	public void addNewMapping(IMigrationEnabledVirtualHost destinationHost, UUID cloudObjectId) {
		objectMappings.put(cloudObjectId, destinationHost.getId());
	}
	
	private class COKeepAliveTask extends TimerTask implements Closeable
	{
		private Timer keepaliveTimer;
		private Logger log;
		
		public COKeepAliveTask()
		{
			this.log = JCloudScaleConfiguration.getLogger(this);
			long keepaliveInterval = JCloudScaleConfiguration.getConfiguration().common().keepAliveIntervalInSec()*1000;
			
			if(keepaliveInterval > 0)
			{
				this.keepaliveTimer = new Timer("KeepaliveTimer");
				this.keepaliveTimer.schedule(this, keepaliveInterval, keepaliveInterval);
				log.info("KeepAlive task is scheduled every "+keepaliveInterval+" ms");
			}
		}

		@Override
		public void run() 
		{
			try
			{
				for(IVirtualHost host : hostsPool) {
					host.refreshCloudObjects();
				}
				
			}
			catch(Exception ex)
			{
				log.severe("Exception occured while sending keepalive messages: "+ex);
			}
		}
		
		@Override
		public void close()
		{	
			if(this.keepaliveTimer != null)
			{
				keepaliveTimer.cancel();
				keepaliveTimer = null;
			}
		}
	}
	
}
