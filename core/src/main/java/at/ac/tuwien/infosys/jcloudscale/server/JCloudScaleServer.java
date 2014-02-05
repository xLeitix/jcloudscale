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
package at.ac.tuwien.infosys.jcloudscale.server;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.springframework.util.ReflectionUtils;

import at.ac.tuwien.infosys.jcloudscale.IJCloudScaleServer;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.IConfigurationChangedListener;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.exception.ProxyInstantiationException;
import at.ac.tuwien.infosys.jcloudscale.logging.Logged;
import at.ac.tuwien.infosys.jcloudscale.management.JCloudScaleReferenceManager;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.utility.CgLibUtil;
import at.ac.tuwien.infosys.jcloudscale.utility.ReflectionUtil;
import at.ac.tuwien.infosys.jcloudscale.utility.SerializationUtil;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudObjectState;

@Logged
public class JCloudScaleServer implements IJCloudScaleServer, IConfigurationChangedListener, Closeable
{

	protected Map<UUID, ServerCloudObject> cloudObjects = null;
	private Map<UUID, List<CloudObjectInvocation>> runningRequests = null;
	protected Map<UUID, ClassLoader> clientIdToClassloaderMap = null;
	protected Map<UUID, ClassLoader> cloudObjectToClassloaderMap = null;
	 
	public Logger log = null;
	private IMQWrapper mq;//we need it to send responses for cloudObjects
	
	private COCleanupTask cleanupTask;
	
	public JCloudScaleServer() throws NamingException, JMSException
	{
		this.cloudObjects = new Hashtable<UUID, ServerCloudObject>();
		this.runningRequests = new Hashtable<UUID, List<CloudObjectInvocation>>();
		this.clientIdToClassloaderMap = new Hashtable<>();
		this.cloudObjectToClassloaderMap = new Hashtable<UUID, ClassLoader>();
		
		this.log = JCloudScaleConfiguration.getLogger(this);
		
		initMessageQueue(JCloudScaleConfiguration.getConfiguration());
		
		// XXX AbstractJCloudScaleServerRunner
		// JCloudScaleServerRunner.getInstance().registerConfigurationChangeListner(this);
		AbstractJCloudScaleServerRunner.getInstance().registerConfigurationChangeListner(this);
		
		AbstractJCloudScaleServerRunner.getInstance().registerConfigurationChangeListner(cleanupTask = new COCleanupTask());
	}
	
	@Override
	public void onConfigurationChange(JCloudScaleConfiguration newConfiguration) 
	{
		log = JCloudScaleConfiguration.getLogger(newConfiguration, this);
		
		String oldResponseQueue = JCloudScaleConfiguration.getConfiguration().server().getResponseQueueName();
		String newResponseQueue = newConfiguration.server().getResponseQueueName();
		
		if(!this.mq.configurationEquals(newConfiguration.common().communication()) ||
				!oldResponseQueue.equals(newResponseQueue))
		{
			try 
			{
				initMessageQueue(newConfiguration);
			} catch (NamingException | JMSException e) 
			{
				e.printStackTrace();
			}
		}
	}

	private void initMessageQueue(JCloudScaleConfiguration cfg) throws NamingException, JMSException 
	{
		IMQWrapper wrapper = JCloudScaleConfiguration.createMQWrapper(cfg);
		wrapper.createTopicProducer(cfg.server().getResponseQueueName());
		
		//has to do it like that to remain thread-safe.
		IMQWrapper oldWrapper = this.mq;
		this.mq = wrapper;
		
		if(oldWrapper != null)
			oldWrapper.close();
	}
	
	@Override
	public void close() 
	{
		if(mq != null)
		{
			mq.close();
			mq = null;
		}
		
		// closing classloaders.
		for(ClassLoader classLoader : cloudObjectToClassloaderMap.values())
			if(classLoader instanceof Closeable && !ClassLoader.getSystemClassLoader().equals(classLoader))
			{
				try 
				{
					((Closeable)classLoader).close();
				} catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		
		cloudObjectToClassloaderMap.clear();
		runningRequests.clear();
		cloudObjects.clear();
		
		cleanupTask.close();
		
	}
	
	protected synchronized ClassLoader getClassloaderForClient(UUID clientId)
	{
	    if(!clientIdToClassloaderMap.containsKey(clientId))
	    {
	        clientIdToClassloaderMap.put(clientId, 
	                JCloudScaleConfiguration.getConfiguration().common()
                                                            .classLoader()
                                                            .createClassLoader());
	    }
	     
	    return clientIdToClassloaderMap.get(clientId);
	}
	
	@Override
	public String createNewCloudObject(String classname,
			byte[] params, String[] paramNames) {
		
		UUID id = UUID.randomUUID();
		UUID clientId = JCloudScaleConfiguration.getConfiguration().common().clientID(); //this may be send along with the request
		// XXX AbstractJCloudScaleServerRunner
		// this.classLoaders.put(id, JCloudScaleServerRunner.getInstance().createClassLoader(id.toString()));
		this.cloudObjectToClassloaderMap.put(id, getClassloaderForClient(clientId));

		Class<?> clazz = null;
		try
		{
			clazz = Class.forName(classname, false, this.cloudObjectToClassloaderMap.get(id));
			
		} catch (ClassNotFoundException e)
		{
			logException(e);
			throw new JCloudScaleException(e,
					"Unable to load user class into custom classloader");
		}

		
		Object cloudObject = null;
		try
		{
			Object[] objectParams = SerializationUtil
					.getObjectArrayFromBytes(params, this.cloudObjectToClassloaderMap.get(id));
			Class<?>[] clazzes = ReflectionUtil.getClassesFromNames(paramNames, this.cloudObjectToClassloaderMap.get(id));
			
			// this uses the cgLib library to replace everything we have passed by
			// ref by dynamic proxies (which will redirect all invocations)
			objectParams = CgLibUtil.replaceRefsWithProxies(objectParams, this.cloudObjectToClassloaderMap.get(id));
	
			Constructor<?> constr = null;
			
			constr = ReflectionUtil.findConstructor(clazz, clazzes);
			cloudObject = constr.newInstance(objectParams);
		}
		catch(InvocationTargetException e)
		{
			String errorMessage = "Error while instantiating user class";
			
			Throwable target = e.getTargetException();
			
			if(target == null)
				throw new JCloudScaleException(e, errorMessage);
			
			logException(target);
			if(target instanceof ProxyInstantiationException)
				throw new JCloudScaleException(target.getCause(), errorMessage);
			else
				throw new JCloudScaleException(target, errorMessage);
		}
		catch (Throwable e)
		{
			logException(e);
			throw new JCloudScaleException(e, "Error while instantiating user class");
		}

		ReflectionUtil.injectCloudId(cloudObject, id);

		ServerCloudObject sco = new ServerCloudObject();
		sco.setCloudObjectClass(clazz);
		sco.setId(id);
		sco.setObject(cloudObject);
		sco.setState(CloudObjectState.IDLE);
		touchServerCloudObject(sco); 
		this.cloudObjects.put(id, sco);

		return id.toString();

	}

	@Override
	public String startInvokingCloudObject(String objectId, String method,
			byte[] params, String[] paramNames) throws JCloudScaleException
	{

		UUID theId = UUID.fromString(objectId);

		CloudObjectInvocation inv = new CloudObjectInvocation(this, this.mq);
		
		Class<?>[] clazzes = null;
		
		try
		{
			
			Object[] objectParams = SerializationUtil
					.getObjectArrayFromBytes(params, this.cloudObjectToClassloaderMap.get(theId));
			
			clazzes = ReflectionUtil.getClassesFromNames(paramNames, this.cloudObjectToClassloaderMap.get(theId));
			
			// this uses the cgLib library to replace everything we have passed by
			// ref by dynamic proxies (which will redirect all invocations)
			objectParams = CgLibUtil.replaceRefsWithProxies(objectParams, this.cloudObjectToClassloaderMap.get(theId));
			
			inv.setParams(objectParams);
			
		} catch (Throwable e)
		{
			logException(e);
			throw new JCloudScaleException(e, "Unable to deserialize parameters");
		}

		ServerCloudObject sco = this.cloudObjects.get(theId);
		if (sco == null)
			throw new JCloudScaleException(
					"Cloud object does not exist in server cache: " + objectId);
		Object theObject = sco.getObject();
		
		inv.setCloudObject(theObject);

		UUID invocationId = UUID.randomUUID();
		inv.setRequestId(invocationId);
		inv.setObjectId(theId);

		try
		{
			Method theMethod = ReflectionUtil.findMethod(theObject.getClass(),
					method, clazzes);
			inv.setMethod(theMethod);
		} catch (Throwable e)
		{
			logException(e);
			throw new JCloudScaleException(e, "Unable to invoke user object");
		}

		// this launches a separate thread and starts the invocation
		inv.invoke();

		// update status of SCO
		sco.getExecutingMethods().add(method);
		sco.setState(CloudObjectState.EXECUTING);
		touchServerCloudObject(sco);
		
		if (!this.runningRequests.containsKey(theId))
			this.runningRequests.put(theId,
					new LinkedList<CloudObjectInvocation>());
		this.runningRequests.get(theId).add(inv);

		return inv.getRequestId().toString();

	}

	@Override
	public byte[] getCloudObjectField(String objectId, String field) {
		
		UUID theId = UUID.fromString(objectId);
		ServerCloudObject sco = this.cloudObjects.get(theId);
		if (sco == null)
			throw new JCloudScaleException(
					"Cloud object does not exist in server cache: " + objectId);
		touchServerCloudObject(sco);
		Object theObject = sco.getObject();
		
		
		byte[] returnVal = null;
		try {
			Field theField = ReflectionUtil.findField(theObject.getClass(), field);
			ReflectionUtils.makeAccessible(theField);
			Object val = theField.get(theObject);
			val = JCloudScaleReferenceManager.getInstance().processField(theField, val);
			returnVal = SerializationUtil.serializeToByteArray(val);
		} catch (NoSuchFieldException | SecurityException |
				IllegalArgumentException | IllegalAccessException | IOException e) {
			logException(e);
			throw new JCloudScaleException(e, "Unable to get field value from cloud object");
		}
		
		
		return returnVal;
		
	}
	
	@Override
	public void setCloudObjectField(String objectId, String field, byte[] value) {
		
		UUID theId = UUID.fromString(objectId);
		ServerCloudObject sco = this.cloudObjects.get(theId);
		if (sco == null)
			throw new JCloudScaleException(
					"Cloud object does not exist in server cache: " + objectId);
		touchServerCloudObject(sco);
		Object theObject = sco.getObject();
		
		try {
			Field theField = ReflectionUtil.findField(theObject.getClass(), field);
			ReflectionUtils.makeAccessible(theField);
			Object valueToSet = SerializationUtil.getObjectFromBytes(value, this.cloudObjectToClassloaderMap.get(theId));
			valueToSet = CgLibUtil.replaceRefWithProxy(valueToSet, this.cloudObjectToClassloaderMap.get(theId));
			theField.set(theObject, valueToSet);
		} catch(JCloudScaleException e) {
			// this should happen if the by-ref type did not have a default constructor
			e.printStackTrace();
			throw e;			
		} catch (Throwable e) {
			logException(e);
			throw new JCloudScaleException(e, "Unable to set field value to cloud object");
		}
		
	}

	
	@Override
	public void suspendInvocation(String objectId, String requestId)
			throws JCloudScaleException
	{

		CloudObjectInvocation inv = checkInputData(objectId, requestId);
		inv.suspend();

	}

	@Override
	public void resumeInvocation(String objectId, String requestId)
			throws JCloudScaleException
	{

		CloudObjectInvocation inv = checkInputData(objectId, requestId);
		inv.resume();

	}

	@Override
	public void destroyCloudObject(String id) throws JCloudScaleException
	{

		UUID theId = UUID.fromString(id);

		if (!this.cloudObjects.containsKey(theId))
			throw new JCloudScaleException(
					"Cloud object does not exist in server cache: " + id);

		this.cloudObjects.get(theId).setState(CloudObjectState.DESTRUCTED);
		touchServerCloudObject(this.cloudObjects.get(theId));
		
		this.cloudObjects.remove(theId);
		if (this.cloudObjectToClassloaderMap.containsKey(theId))
		{
//			ClassLoader classLoader = 
			        this.cloudObjectToClassloaderMap.remove(theId);
//			
//			// if this classloader manages some resources, we have to close it.
//			if(classLoader != null && classLoader instanceof Closeable)
//				try {
//					((Closeable)classLoader).close();
//				} catch (IOException e) 
//				{
//					logException(e);
//				}
		}
		
	}

	@Override
	public void shutdown()
	{
		try
		{
			// XXX AbstractJCloudScaleServerRunner
			// JCloudScaleServerRunner.getInstance().shutdown();
			AbstractJCloudScaleServerRunner.getInstance().shutdown();
		} catch (Exception e)
		{
			this.log.severe(e.getMessage());
		}
		new ShutdownThread().start();
	}

	void logException(Throwable e)
	{
		StackTraceElement[] trace = e.getStackTrace();
		StringBuilder sb = new StringBuilder();
		sb.append("Exception Trace\n");
		sb.append(e.getClass().getName() + ": ");
		if (e.getMessage() != null)
			sb.append(e.getMessage() + "\n");
		for (StackTraceElement el : trace)
		{
			sb.append("\tat " + el.toString());
			sb.append("\n");
		}
		this.log.severe(sb.toString());
	}

	@Override
	public String getCloudObjectType(String id) throws JCloudScaleException
	{
		Object cloudObject = this.cloudObjects.get(UUID.fromString(id));

		if (cloudObject == null)
			throw new JCloudScaleException(
					"Cloud object does not exist in server cache: " + id);

		return cloudObject.getClass().getName();
	}
	
	private CloudObjectInvocation checkInputData(String objectId,
			String requestId)
	{

		UUID theObjectId = UUID.fromString(objectId);
		UUID theInvocationId = UUID.fromString(requestId);
		if (!this.runningRequests.containsKey(theObjectId))
			throw new JCloudScaleException("No running requests for object "
					+ objectId);

		CloudObjectInvocation inv = null;
		for (CloudObjectInvocation i : this.runningRequests.get(theObjectId))
		{
			if (i.getRequestId().equals(theInvocationId))
			{
				inv = i;
				break;
			}
		}
		if (inv == null)
			throw new JCloudScaleException("There is no invocation with id "
					+ requestId + " for object " + objectId);
		return inv;
	}

	public Object getCloudObject(UUID objectId)
	{
		if(this.cloudObjects.containsKey(objectId))
			return this.cloudObjects.get(objectId).getObject();
		else
			return null;
	}
	
	public ClassLoader getCloudObjectClassLoader(UUID objectId)
	{
		return this.cloudObjectToClassloaderMap.get(objectId);
	}
	
	@Override
	public void keepAliveCloudObject(UUID id) {
		
		ServerCloudObject sco = this.cloudObjects.get(id);
		if (sco == null)
		{
		    log.warning("Keep alive message received for cloud object "+id+
		            ", but there's no object with this id on server "+
		            AbstractJCloudScaleServerRunner.getInstance().getId()+".");
//			throw new JCloudScaleException(
//					"Cloud object does not exist in server cache: " + id);
		}
		else
		    touchServerCloudObject(sco);
	}
	
	protected void touchServerCloudObject(ServerCloudObject sco) {
	        
	    if(log.isLoggable(Level.FINE))
	        log.fine("Received isAlive message for object "+sco.getId()+
	                ". Last isAlive "+TimeUnit.MILLISECONDS.convert(System.nanoTime() - sco.getLastTouched(), TimeUnit.NANOSECONDS)
	                +"ms ago.");
	    
		sco.setLastTouched(System.nanoTime());	
	}
	
	private static class ShutdownThread extends Thread
	{
		@Override
		public void run()
		{
			try
			{
				Thread.sleep(3000);
				System.exit(0);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private class COCleanupTask implements Closeable, IConfigurationChangedListener
	{
		private Timer cleanupTimer;
		private long keepaliveInterval;
		private Logger log;
		
		public COCleanupTask()
		{
		    initialize(JCloudScaleConfiguration.getConfiguration());
		}
		
		@Override
                public void onConfigurationChange(JCloudScaleConfiguration newConfiguration) 
                {
                    initialize(newConfiguration);
                }
		
		private void initialize(JCloudScaleConfiguration newConfiguration)
		{
		    this.log = JCloudScaleConfiguration.getLogger(newConfiguration, this);
                    
		    // we assume that we do cleanup every 3 keepalive intervals
                    long keepaliveInterval = newConfiguration.common().keepAliveIntervalInSec()*1000 * 3;  
                    
                    if(keepaliveInterval > 0 && keepaliveInterval != this.keepaliveInterval)
                    {
                        this.keepaliveInterval = keepaliveInterval;
                        
                        if(this.cleanupTimer != null)
                            this.cleanupTimer.cancel();
                        
                        this.cleanupTimer = new Timer("CleanupTimer");
                        // we have to create a new timer task each time as otherwise next line will throw exception 
                        // that this task was already scheduled at some point in past.
                        this.cleanupTimer.schedule(new CleanupTimerTask(), keepaliveInterval, keepaliveInterval);
                        
                    }
		}
		
		@Override
		public void close()
		{	
			if(this.cleanupTimer != null)
			{
				cleanupTimer.cancel();
				cleanupTimer = null;
			}
		}
		
		private class CleanupTimerTask extends TimerTask
                {
                    @Override
                        public void run() 
                        {
                                try
                                {
                                        //
                                        // iterate over all our COs and see which ones have not been touched for some time
                                        //
                                        Collection<ServerCloudObject> coll = new ArrayList<>(cloudObjects.values());
                                        for(ServerCloudObject sco : coll) {
                                                long noActivityPeriod = TimeUnit.MILLISECONDS.convert(System.nanoTime() - sco.getLastTouched(), TimeUnit.NANOSECONDS);
                                                if(noActivityPeriod >  keepaliveInterval) {
                                                        log.info(String.format("Removing cloud object %s as there was no activity on it for %d seconds",
                                                                sco.getId().toString(), noActivityPeriod / 1000));
                                                        destroyCloudObject(sco.getId().toString());
                                                }
                                        }
                                        
                                        //
                                        // cleaning up classloaders that are no longer used
                                        //
                                        synchronized (this) 
                                        {
                                            List<UUID> oldClients = new ArrayList<>();
                                            for(Entry<UUID, ClassLoader> classLoader : clientIdToClassloaderMap.entrySet())
                                            {
                                                if(!cloudObjectToClassloaderMap.containsValue(classLoader.getValue()))
                                                    oldClients.add(classLoader.getKey());
                                            }
                                            
                                            for(UUID clientId : oldClients)
                                            {
                                                ClassLoader classLoader = clientIdToClassloaderMap.remove(clientId);
                                                if(!classLoader.equals(ClassLoader.getSystemClassLoader()) && (classLoader instanceof Closeable))
                                                {
                                                    log.info("Destroying classloader for client "+clientId+" as there's no objects using it.");
                                                    ((Closeable)classLoader).close();
                                                }
                                            }
                                        }
                                }
                                catch(JCloudScaleException | IOException ex)
                                {
                                        log.severe("Exception occured while checking COs for outdated objects: "+ex);
                                }
                        }
                }
	}
}
