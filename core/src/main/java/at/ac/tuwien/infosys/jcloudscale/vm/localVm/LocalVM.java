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
package at.ac.tuwien.infosys.jcloudscale.vm.localVm;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.InvocationInfo;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.exception.ScalingException;
import at.ac.tuwien.infosys.jcloudscale.logging.Logged;
import at.ac.tuwien.infosys.jcloudscale.management.JCloudScaleReferenceManager;
import at.ac.tuwien.infosys.jcloudscale.messaging.TimeoutException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.CPUEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.RAMEvent;
import at.ac.tuwien.infosys.jcloudscale.migration.IMigrationEnabledJCloudScaleHost;
import at.ac.tuwien.infosys.jcloudscale.migration.MigrationEnabledVirtualHostProxy;
import at.ac.tuwien.infosys.jcloudscale.monitoring.CPUUsage;
import at.ac.tuwien.infosys.jcloudscale.monitoring.EventCorrelationEngine;
import at.ac.tuwien.infosys.jcloudscale.monitoring.IMetricsDatabase;
import at.ac.tuwien.infosys.jcloudscale.monitoring.MonitoringMetric;
import at.ac.tuwien.infosys.jcloudscale.monitoring.RAMUsage;
import at.ac.tuwien.infosys.jcloudscale.utility.CgLibUtil;
import at.ac.tuwien.infosys.jcloudscale.utility.ReflectionUtil;
import at.ac.tuwien.infosys.jcloudscale.utility.SerializationUtil;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudObjectState;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;
import at.ac.tuwien.infosys.jcloudscale.vm.IdManager;
import at.ac.tuwien.infosys.jcloudscale.vm.VirtualHost;
import at.ac.tuwien.infosys.jcloudscale.vm.VirtualHostProxy;

@Logged
public class LocalVM extends VirtualHost 
{
	private static final String SERVER_MODE_COMMAND = "-server";
	private static final String HEAP_SIZE_COMMAND = "-Xmx";
	private static final String CLASSPATH_COMMAND = "-cp";
	
	private static final String STANDARD_INSTANCE_SIZE_FLAG = "local.default";
	
	protected CloudPlatformConfiguration config;
	protected IMigrationEnabledJCloudScaleHost server;
	protected String serverIp;
	 
	protected Process jvmProcess;
	protected boolean startPerformanceMonitoring;
	
	protected List<ClientCloudObject> cloudObjects = new CopyOnWriteArrayList<>();
	
	protected Logger log;
	
	protected IdManager idManager;
	
	protected LocalVM(IdManager idManager, boolean startPerformanceMonitoring) {
		this.idManager = idManager;
		this.startPerformanceMonitoring = startPerformanceMonitoring;
		this.log = JCloudScaleConfiguration.getLogger(this);
		this.instanceSize = STANDARD_INSTANCE_SIZE_FLAG;
	}
	
	public LocalVM(LocalCloudPlatformConfiguration config, IdManager idManager, boolean startPerformanceMonitoring) 
	{
		this(idManager, startPerformanceMonitoring);
		this.config = config;
	}

	@Override
	public UUID deployCloudObject(ClientCloudObject cloudObject, Object[] args, Class<?>[] paramTypes) {
		
		try 
		{
			// we add object here to have it as soon as possible
			cloudObjects.add(cloudObject);
			
			ensureHostStarted();
			
			byte[] byteArgs = SerializationUtil.serializeToByteArray(args);
			String[] paramNames = ReflectionUtil.getNamesFromClasses(paramTypes);
			String id = server.createNewCloudObject(cloudObject.getCloudObjectClass().getName(), byteArgs, paramNames);
			
			UUID objectId = UUID.fromString(id);
			cloudObject.setId(objectId);
			
			addManagedObject(cloudObject);
			
			return objectId;
			
		} catch(IOException e) {
			throw new JCloudScaleException(e, "Could not serialize invocation parameters: " + Arrays.toString(args));
		}
		finally
		{
			lastRequestTime = new Date(System.currentTimeMillis());
		}
		
	}
	
	@Override
	public Object invokeCloudObject(UUID cloudObjectId, Method method,	Object[] args, Class<?>[] paramTypes) 
	{
		try 
		{
			ensureHostStarted();
			
			ClientCloudObject clientCo = managedObjects.get(cloudObjectId);
			
			if(clientCo == null)
				 throw new JCloudScaleException("The object with id "+cloudObjectId+ " was not found or was not deployed yet.");
			
			clientCo.addExecutingMethod(method.getName());
			Object proxy = clientCo.getProxy();
			byte[] retBytes = scheduleInvocation(cloudObjectId, method.getName(), args, paramTypes, proxy);
			
			clientCo.removeExecutingMethod(method.getName());
			
			Object returned = SerializationUtil.getObjectFromBytes(retBytes, this.getClass().getClassLoader());
			// if this is a reference, replace it with a proxy
			returned = CgLibUtil.replaceRefWithProxy(returned, this.getClass().getClassLoader());
			
			return returned; 
			
		} catch(IOException e) {
			e.printStackTrace();
			throw new JCloudScaleException(e, "Could not serialize invocation parameters: " + Arrays.toString(args));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new JCloudScaleException(e, "Could not deserialize return value. Class not found.");
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new JCloudScaleException(e, "Interrupted while waiting for server to become free.");
		} catch(JCloudScaleException e) {
			// this should happen if the by-ref type did not have a default constructor
			e.printStackTrace();
			throw e;			
		} catch (Throwable e) {
			e.printStackTrace();
			throw new JCloudScaleException(e);
		}
		finally
		{
			lastRequestTime = new Date(System.currentTimeMillis());
		}
	}
	
	@Override
	public Object getCloudObjectFieldValue(UUID id, Field field) {
		
		try 
		{
			ensureHostStarted();
			
			byte[] ser = server.getCloudObjectField(id.toString(), field.getName());
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			Object retField = SerializationUtil.getObjectFromBytes(ser, classLoader);
			
			return CgLibUtil.replaceRefWithProxy(retField, classLoader);
		} catch(JCloudScaleException e) {
			// this should happen if the by-ref type did not have a default constructor
			e.printStackTrace();
			throw e;			
		} catch (Throwable e) {
			e.printStackTrace();
			throw new JCloudScaleException(e, "Could not deserialize return value from field access");
		}
		finally
		{
			lastRequestTime = new Date(System.currentTimeMillis());
		}
	}
	
	@Override
	public void setCloudObjectFieldValue(UUID id, Field field, Object val) {
		
		try 
		{
			ensureHostStarted();
			
			val = JCloudScaleReferenceManager.getInstance().processField(field, val);
			byte[] ser = SerializationUtil.serializeToByteArray(val);
			server.setCloudObjectField(id.toString(), field.getName(), ser);
		} catch (IOException e) {
			e.printStackTrace();
			throw new JCloudScaleException(e, "Could not serialize value to set to field");
		}
		finally
		{
			lastRequestTime = new Date(System.currentTimeMillis());
		}
	}
	
	@Override
	public void destroyCloudObject(UUID cloudObjectId) 
	{
		ensureHostStarted();
		
		server.destroyCloudObject(cloudObjectId.toString());
		
		ClientCloudObject cloudObject = managedObjects.get(cloudObjectId);
		
		// we should be setting the state here anyway, as a client
		// may still hold a reference to this cco
		cloudObject.setDestructed();
		
		cloudObjects.remove(cloudObject);
		removedManagedObject(cloudObjectId);
		
		lastRequestTime = new Date(System.currentTimeMillis());
	}

	@Override
	public void refreshCloudObjects() 
	{
		if(!isOnline())
		    return; // if the host is not online now, there's no point to send isAlive messages.
		
		for(ClientCloudObject cco : cloudObjects) 
		{
		    log.info("Sending IsAlive message for object "+cco.getId()+" to host "+ id + "("+this.serverIp+")");
		    if(cco.getId() != null)
			server.keepAliveCloudObject(cco.getId());
		}
	}
	
	@Override
	public void startupHost(IHostPool hostPool, String size) throws ScalingException 
	{
		// see if we have a free ID available to us (for instance a static host)
		// TODO: in principle, we should associate IDs with instance sizes, so that
		//       we know whether a given static instance is of the size we wanted
		//       however, for now, I just assume that if a user explicitly requests
		//       a given size, we are starting up dynamically and ignore static instance
		UUID serverId = idManager.getFreeId(size == null);
		
		if(serverId == null) {
			launchHost(size);
			// now, at some point, we should get a free ID, which we assign to this host
			serverId = idManager.waitForId();
		}
		
		// check if this is an ID of a static or dynamic host
		if(idManager.isStaticId(serverId))
			isStatic = true;
		
		this.id = serverId;
		
		this.serverIp = idManager.getIpToId(serverId);
		log.info("Server "+serverId+" has IP address "+serverIp);
		
//		//sending configuration.
		try 
		{
		        JCloudScaleConfiguration.getConfiguration().sendConfigurationToStaticHost(id);
			log.info("Successfully sent configuration to host "+id+" ("+this.serverIp+")");
		} catch (NamingException | JMSException | TimeoutException | IOException e) 
		{
			throw new JCloudScaleException(e, "Failed to send configuration to host "+serverId + "("+this.serverIp+")");
		}
		
		server = new MigrationEnabledVirtualHostProxy(this.id);
		JCloudScaleClient.getClient().addProxy(this.id, (VirtualHostProxy) server);
		log.info("Created proxy for server");
		if(startPerformanceMonitoring) {
			registerCPUMetric();
			registerRAMMetric();
			log.info("Registered basic monitoring metrics for server");
		}
		
		startupTime = new Date(System.currentTimeMillis());
		
		hostStarted();
		
		this.scaleDownTask = new ScaleDownTask(hostPool);
		log.info("Started scale-down task for new server");
		
	}
	
	@Override
	public void close() 
	{
		if(scaleDownTask != null)
		{
			scaleDownTask.close();
			scaleDownTask = null;
		}
		
		hostShutdown();
		
		if(startPerformanceMonitoring) {
			EventCorrelationEngine.getInstance().unregisterMetric("CPULoad_"+id.toString());
			EventCorrelationEngine.getInstance().unregisterMetric("RAMUsage_"+id.toString());
		}
		
		idManager.releaseId(id);

		if(!isStaticHost()) {
			server.shutdown();
			idManager.removeId(id);
		}
		((VirtualHostProxy)server).close();
	}
	
	protected void launchHost(String size) {
		
		this.instanceSize = size;
		
	        if(!(this.config instanceof LocalCloudPlatformConfiguration))
		    throw new JCloudScaleException("Failed to launch local virtual VM: preconfigured configuration is of the wrong type:"
		                            +(this.config==null ?"NULL": this.config.getClass().getName())+"instead of LocalCloudPlatformConfiguration");
	        
	        LocalCloudPlatformConfiguration config = (LocalCloudPlatformConfiguration)this.config;
	        
		File workingDir = new File(config.getStartupDirectory());
		
		try
		{
			//For even higher platform independency, we can use ant to start new jvm.
			String javaPath = config.getJavaPath();
			
			ProcessBuilder pb = new ProcessBuilder(
				/*JAVA executable*/				javaPath,
				/*Class path of the server*/	CLASSPATH_COMMAND, config.getClasspath(), //should go as separate args.
				/*Server startup class*/ 		config.getServerStartupClass()
					);
			
			// adding memory limit parameter
			if(config.getJavaHeapSizeMB() > 0)
				pb.command().add(1, HEAP_SIZE_COMMAND + config.getJavaHeapSizeMB()+"m");
			
			// adding server mode parameter
			if(config.isServerMode())
				pb.command().add(1, SERVER_MODE_COMMAND);
			
			// add custom JVM parameters as defined by the user
			for(String arg : config.getCustomJVMArgs()) {
				pb.command().add(1, arg);
			}
			
			pb.directory(workingDir);
			
			// redirecting output to the same destination as of our process
			//(so that server will write output to client's console/error stream.)
			pb.redirectOutput(Redirect.INHERIT);
			pb.redirectError(Redirect.INHERIT);
			
			jvmProcess = pb.start();
			try 
			{
				Thread.sleep(400);
				int exitCode = jvmProcess.exitValue();
				String message = "JVM failed to launch. Exit code "+exitCode;
				
				throw new ScalingException(message);
			} 
			catch(IllegalThreadStateException | InterruptedException e1) 
			{
				// ignore
			}
		} 
		catch (IOException e) 
		{
			throw new ScalingException("Could not start local VM. Error message was: "+e.getMessage());
		}
	}
	
	private byte[] scheduleInvocation(UUID cloudObjectId, String method, Object[] args, Class<?>[] paramTypes, Object obj)
			throws InterruptedException, IOException {
		
		byte[] byteArgs = SerializationUtil.serializeToByteArray(args);
		String[] paramNames = ReflectionUtil.getNamesFromClasses(paramTypes);
		
		
		String cloudObjectIdString = cloudObjectId.toString();
		String invocationIdString = server.startInvokingCloudObject(cloudObjectIdString, method, byteArgs, paramNames);
		UUID invocationId = UUID.fromString(invocationIdString);
		addInvocation(cloudObjectId, invocationId);
		
		ReflectionUtil.addInvocationInfo(obj,
			new InvocationInfo(cloudObjectIdString, invocationIdString, method, args)	
		);
		
		byte[] ret = ((VirtualHostProxy) server).waitForResult(invocationIdString);
		
		ReflectionUtil.removeInvocationInfo(obj, invocationIdString);
		removeInvocation(cloudObjectId, invocationId);
		
		return ret;
	}

	@Override
	public String getIpAddress() {
		return serverIp;
	}
	
	@Override
	public Class<?> getCloudObjectType(UUID cloudObjectId) throws JCloudScaleException {
		
		ClientCloudObject clientCo = managedObjects.get(cloudObjectId);
		Class<?> clazz = clientCo.getCloudObjectClass();
		return clazz;
		
	}
	
	@Override
	public Iterable<ClientCloudObject> getCloudObjects() {
		return Collections.unmodifiableCollection(cloudObjects);
	}
	
	@Override
	public int getCloudObjectsCount()
	{
		return cloudObjects.size();
	}
	
	@Override
	public void suspendRunningInvocation(UUID cloudObject, UUID invocation) {
		server.suspendInvocation(cloudObject.toString(), invocation.toString());
	}
	
	@Override
	public void continueRunningInvocation(UUID cloudObject, UUID invocation) {
		server.resumeInvocation(cloudObject.toString(), invocation.toString());
	}
	
	@Override
	public CPUUsage getCurrentCPULoad() {
		EventCorrelationEngine engine = EventCorrelationEngine.getInstance();
		if(engine == null)
			return null;
		IMetricsDatabase db = engine.getMetricsDatabase();
		Object val = db.getLastValue("CPULoad_"+id.toString());
		if(val == null)
			return null;
		CPUEvent cpuEvent = (CPUEvent) val;
		return
			new CPUUsage(
				cpuEvent.getSystemCpuLoad(),
				cpuEvent.getNrCPUs(),
				cpuEvent.getArch()
			);
	}
	
	@Override
	public RAMUsage getCurrentRAMUsage() {
		EventCorrelationEngine engine = EventCorrelationEngine.getInstance();
		if(engine == null)
			return null;
		IMetricsDatabase db = engine.getMetricsDatabase();
		Object val = db.getLastValue("RAMUsage_"+id.toString());
		if(val == null)
			return null;
		RAMEvent ramEvent = (RAMEvent) val;
		return
			new RAMUsage(
				ramEvent.getMaxMemory(),
				ramEvent.getUsedMemory(),
				ramEvent.getFreeMemory()
			);
	}
	
	@Override
	public CloudObjectState getCloudObjectState(UUID cloudObject) {
		
		if(!managedObjects.containsKey(cloudObject))
			throw new JCloudScaleException("Cannot get state for unmanaged cloud object "+cloudObject.toString());
		
		return managedObjects.get(cloudObject).getState();
		
	}

	@Override
	public List<String> getExecutingMethods(UUID cloudObject) {

		if(!managedObjects.containsKey(cloudObject))
			throw new JCloudScaleException("Cannot get executing methods for unmanaged cloud object "+cloudObject.toString());
		
		return managedObjects.get(cloudObject).getExecutingMethods();
		
	}
	
	// MIGRATION METHODS
	@Override
	public byte[] serializeToMigrate(UUID cloudObjectId) throws JCloudScaleException {
		try {
			ensureHostStarted();
			return server.serializeToMigrate(cloudObjectId.toString());
		} finally {
			lastRequestTime = new Date(System.currentTimeMillis());
		}
	}

	@Override
	public void deployMigratedCloudObject(UUID cloudObjectId, Class<?> cloudObjectType,
			byte[] serializedCloudObject, Object proxy, ReferenceQueue<Object> queue)
			throws JCloudScaleException {
		try {
			ensureHostStarted();
			server.deployMigratedCloudObject(cloudObjectId.toString(),
					cloudObjectType.getName(), serializedCloudObject);

			ClientCloudObject clientCo = new ClientCloudObject(cloudObjectId, cloudObjectType, proxy, queue);
			cloudObjects.add(clientCo);
			addManagedObject(clientCo);
		} finally {
			lastRequestTime = new Date(System.currentTimeMillis());
		}
	}
	
	@Override
	public void removeCloudObject(UUID cloudObjectId) throws JCloudScaleException {
		try {
			ensureHostStarted();
			server.removeCloudObject(cloudObjectId.toString());
			ClientCloudObject cloudObject = managedObjects.get(cloudObjectId);
			cloudObjects.remove(cloudObject);
			removedManagedObject(cloudObjectId);
		} finally {
			lastRequestTime = new Date(System.currentTimeMillis());
		}
	}

	@Override
	public long getCloudObjectSize(UUID cloudObjectId) {
		throw new RuntimeException("Not implemented!");
	}
	
	@Override
	public Object getProxyObject(UUID cloudObjectId) {
		try {
			ensureHostStarted();
			if (!managedObjects.containsKey(cloudObjectId))
				throw new JCloudScaleException("Cannot get proxy for unmanaged cloud object " + cloudObjectId);

			return managedObjects.get(cloudObjectId).getProxy();
		} finally {
			lastRequestTime = new Date(System.currentTimeMillis());
		}
	}
	
	private void registerCPUMetric() {
		MonitoringMetric cpuMetric = new MonitoringMetric();
		cpuMetric.setName("CPULoad_"+id.toString());
		cpuMetric.setEpl("select * from CPUEvent");
		// cpuMetric.setResultField("cpuLoad");
        EventCorrelationEngine.getInstance().registerMetric(cpuMetric);
	}
	
	private void registerRAMMetric() {
		MonitoringMetric ramMetric = new MonitoringMetric();
        ramMetric.setName("RAMUsage_"+id.toString());
        ramMetric.setEpl("select * from RAMEvent");
        // ramMetric.setResultField("usedMemory");
        EventCorrelationEngine.getInstance().registerMetric(ramMetric);
	}
}
