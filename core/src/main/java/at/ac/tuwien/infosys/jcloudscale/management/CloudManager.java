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
package at.ac.tuwien.infosys.jcloudscale.management;

import java.io.Closeable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.exception.ScalingException;
import at.ac.tuwien.infosys.jcloudscale.logging.Ignore;
import at.ac.tuwien.infosys.jcloudscale.logging.Logged;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.utility.ReflectionUtil;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudObjectState;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHostPool;

@Logged
public class CloudManager implements Closeable
{
	private static CloudManager instance = null;
	
	private IVirtualHostPool hosts;
	private Logger log;
	private UIConnector uiConnector;
	private StaticFieldsManager staticFieldsHandler;
	
	private ReferenceQueue<Object> cloudObjectReferenceQueue;
	private Timer refQueueMonitor;
	
	private static final long REF_QUEUE_RUN = 1000;
	
	private CloudManager(CloudPlatformConfiguration config)
	{
		// We just have to ensure that JCloudScaleClient is started before doing anything with JCloudScale. 
		// Required to ensure that environment (mq server?) is operating.
		JCloudScaleClient.getClient();
				
		this.hosts = config.getVirtualHostPool();
		this.log = JCloudScaleConfiguration.getLogger(this);
		
		this.cloudObjectReferenceQueue= new ReferenceQueue<>();
		refQueueMonitor = new Timer();
		refQueueMonitor.scheduleAtFixedRate(new RefQueueCheck(), REF_QUEUE_RUN, REF_QUEUE_RUN);
		
		try 
		{
			this.uiConnector = new UIConnector(this, JCloudScaleConfiguration.getConfiguration().common().UI());
			this.staticFieldsHandler = new StaticFieldsManager();
		} catch (Exception e) 
		{
			e.printStackTrace();
			this.log.severe(e.getMessage());
			throw new JCloudScaleException(e,
					"Could not connect to MQ for sending and receiving statistics. GUI will not work");
		}
	}

	@Ignore
	public synchronized static CloudManager getInstance()
	{
		if(instance == null)
			instance = new CloudManager(JCloudScaleConfiguration.getConfiguration().server().cloudPlatform());
		
		return instance;
	}

	public UUID createNewInstance(Class<?> type, Object[] args, Class<?>[] paramClasses, Object proxy) throws JCloudScaleException, ScalingException
	{

		IVirtualHost host = null;
		log.fine("Selecting cloud host for new object of type "+type.getName());
		long startTime = System.currentTimeMillis();
		
		// construct the clientcloudobject
		ClientCloudObject clientCloudObject = new ClientCloudObject(null, type, proxy, cloudObjectReferenceQueue);
		
		ReflectionUtil.injectClientCloudObject(proxy, clientCloudObject);
		
		try
		{
			AbstractScalingPolicy policy = JCloudScaleConfiguration.getConfiguration().common().scalingPolicy();
			
			try
			{
				host = (IVirtualHost)policy.selectHost(clientCloudObject, this.hosts);
			}
			catch(Exception ex)
			{
				throw new JCloudScaleException(ex, "Scaling policy \"selectHost\" method invocation thrown an exception: "+ex);
			}
			
			if (host == null) 
				host = (IVirtualHost)hosts.startNewHost();

		} catch (JCloudScaleException | ScalingException e)
		{
			e.printStackTrace();
			throw e;
		} catch (Exception e)
		{
			e.printStackTrace();
			throw new JCloudScaleException(e, "Internal JCloudScale exception");
		}
		
		log.fine("Selected host " + (host == null ? "NULL" : (host.getId()+" with address "+host.getIpAddress())+ 
				". (Selection took "+(System.currentTimeMillis() - startTime)+" ms)"));
		
		return this.hosts.deployCloudObject(host, clientCloudObject, args, paramClasses);
	}

	public Object invokeCloudObject(UUID id, Method method, Object[] args, Class<?>[] paramTypes)
	{
		ReentrantReadWriteLock rwl = hosts.getCOLock(id);
		rwl.readLock().lock();
		try
		{
			IVirtualHost host = this.hosts.findManagingHost(id);
			return host.invokeCloudObject(id, method, args, paramTypes);
		}
		finally
		{
			rwl.readLock().unlock();	
		}
	}

	public Object getFieldValue(UUID id, Field field) {
		
		// TODO: do we also need to lock like for invocations?
		IVirtualHost host = this.hosts.findManagingHost(id);
		return host.getCloudObjectFieldValue(id, field);
		
	}
	
	public void setFieldValue(UUID id, Field field, Object val) {
		
		// TODO: do we also need to lock like for invocations?
		IVirtualHost host = this.hosts.findManagingHost(id);
		host.setCloudObjectFieldValue(id, field, val);
		
	}
	
	public void destructCloudObject(UUID id) throws JCloudScaleException
	{
		this.hosts.destroyCloudObject(id);
	}

	@Override
	public void close() 
	{
		if(uiConnector != null)
			uiConnector.close();
		
		if(staticFieldsHandler != null)
			staticFieldsHandler.close();
		
		refQueueMonitor.cancel();
		hosts.close();
	}
	
	public static synchronized void closeCloudManager()
	{
		if(instance != null)
		{
			instance.close();
			instance = null;
		}
	}

	public int countVirtualMachines()
	{
		return this.hosts.getHostsCount();
	}
	
	public Collection<IVirtualHost> getHosts()
	{
		return this.hosts.getVirtualHosts();
	}
	
	public IVirtualHostPool getHostPool()
	{
		return this.hosts;
	}

	public int countCloudObjects()
	{
		return this.hosts.countCloudObjects();
	}

	public Set<UUID> getCloudObjects()
	{
		return this.hosts.getCloudObjects();
	}

	public IVirtualHost getHost(UUID objectId)
	{
		return this.hosts.findManagingHost(objectId);
	}

	public ClientCloudObject getClientCloudObject(Object proxy) {
		
		// is there a better way to do this?
		
		for(IVirtualHost host : this.hosts.getVirtualHosts()) {
			for(ClientCloudObject cco : host.getCloudObjects()) {
				if(cco.getProxy() == proxy)
					return cco;
			}
		}
		return null;
		
	}
	
	public ReferenceQueue<Object> getRefQueue() {
		return cloudObjectReferenceQueue;
	}
	
	class RefQueueCheck extends TimerTask {

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			
			log.finest("Running monitor thread for cloud object reference queue");
			
			// here we simply remove all objects that have been GC'ed since the last run
			WeakReference<Object> proxy = null;
			while((proxy = (WeakReference<Object>)cloudObjectReferenceQueue.poll()) != null) {
				
				ClientCloudObject cco = getClientCloudObject(proxy.get());
				if(cco == null || cco.getState() == CloudObjectState.DESTRUCTED)
					log.finer("Object "+cco+" seems to have been GC'ed, but apparently we have removed this object anyway. Ignoring");
				else {
					log.warning("Removing cloud object "+cco.getId().toString()+" as it seems to have been GC'ed in the target application");
					destructCloudObject(cco.getId());
				}
				
			}
			
		}
		
	}

}
