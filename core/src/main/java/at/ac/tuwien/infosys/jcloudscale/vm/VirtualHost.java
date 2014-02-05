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
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.logging.Logged;
import at.ac.tuwien.infosys.jcloudscale.migration.IMigrationEnabledVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;

@Logged
public abstract class VirtualHost implements IMigrationEnabledVirtualHost {

	private static final long HOST_ONLINE_TIMEOUT_MS = 10*60*1000;
	
	protected UUID id;
	protected boolean isStatic = false;
	protected Map<UUID,List<UUID>> managedObjectInvocations = new Hashtable<UUID, List<UUID>>();
	protected Map<UUID, ClientCloudObject> managedObjects = new Hashtable<>();
	
	protected ScaleDownTask scaleDownTask;
	
	protected Date startupTime = null;
	protected Date lastRequestTime = null;
	private volatile boolean isOnline = false;
	private CountDownLatch onlineSignallingLatch = new CountDownLatch(1);
	
	protected String instanceSize = null;
	
	@Override
	public boolean isOnline() {
		return isOnline;
	}
	
	@Override
	public UUID getId() {
		return id;
	}
	
	@Override
	public boolean isStaticHost() {
		return isStatic;
	}
	
	@Override
	public Set<UUID> getManagedObjectIds() {
		return managedObjectInvocations.keySet();
	}
	
	@Override
	public ClientCloudObject getCloudObjectById(UUID objectId) 
	{
		return managedObjects.get(objectId);
	}
	
	
	@Override
	public Date getStartupTime() 
	{
		return startupTime;
	}

	@Override
	public Date getLastRequestTime() 
	{
		return lastRequestTime;
	}

	@Override
	public String getDeclaredInstanceSize() {
		return instanceSize;
	}
	
	protected void addManagedObject(ClientCloudObject object) 
	{
		this.managedObjects.put(object.getId(), object);
		this.managedObjectInvocations.put(object.getId(), new LinkedList<UUID>());
	}
	
	protected void addInvocation(UUID object, UUID inv) {
		this.managedObjectInvocations.get(object).add(inv);
	}
	
	protected void removedManagedObject(UUID object) {
		this.managedObjectInvocations.remove(object);
		this.managedObjects.remove(object);
	}
	
	protected void removeInvocation(UUID object, UUID inv) {
		this.managedObjectInvocations.get(object).remove(inv);
	}
	
	/**
	 * Informs that the host is started.
	 */
	protected void hostStarted()
	{
		isOnline = true;
		onlineSignallingLatch.countDown();
	}
	
	/**
	 * Informs that the host is shutting down.
	 */
	protected void hostShutdown()
	{
		isOnline = false;
		onlineSignallingLatch = null;
	}
	
	/**
	 * Waits for the host to successfully start.
	 * Throws exceptions in case the host does not start within timeout or is shut down already.
	 */
	protected void ensureHostStarted() 
	{
		try 
		{
			if(isOnline)
				return;
			
			if(onlineSignallingLatch == null)
				throw new JCloudScaleException("Waiting for the host to start, while it is shut down already.");
			
			if(!onlineSignallingLatch.await(HOST_ONLINE_TIMEOUT_MS, TimeUnit.MILLISECONDS))
				throw new JCloudScaleException("Host did not start within the timeout.");
			
		} catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
	}
	

	protected class ScaleDownTask extends TimerTask implements Closeable
	{
		private AbstractScalingPolicy scalingPolicy;
		private Timer scalingDownTimer;
		private IHostPool hostPool;
		private Logger log;
		
		public ScaleDownTask(IHostPool hostPool)
		{
			this.log = JCloudScaleConfiguration.getLogger(this);
			this.hostPool = hostPool;
			long scaleDownInterval = JCloudScaleConfiguration.getConfiguration().common().scaleDownIntervalInSec()*1000;
			scalingPolicy = JCloudScaleConfiguration.getConfiguration().common().scalingPolicy();
			
			if(scaleDownInterval > 0 && scalingPolicy != null)
			{
				this.scalingDownTimer = new Timer("ScaleDownTimer of "+getId());
				this.scalingDownTimer.schedule(this, scaleDownInterval, scaleDownInterval);
			}
		}

		@Override
		public void run() 
		{
			try
			{
				if(this.scalingPolicy.scaleDown(VirtualHost.this, hostPool))
					hostPool.shutdownHostAsync(VirtualHost.this);
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				log.severe("Exception occured while asking for scale down: "+ex);
			}
		}
		
		@Override
		public void close()
		{	
			if(this.scalingDownTimer != null)
			{
				scalingDownTimer.cancel();
				scalingDownTimer = null;
			}
		}
	}
}
