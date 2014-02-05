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
package at.ac.tuwien.infosys.jcloudscale.server.aspects;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServer;

@Aspect
public class ServerRemoteObjectLoggingAspect {
	
	private Logger log = null;
	
	public ServerRemoteObjectLoggingAspect() 
	{
		log = JCloudScaleConfiguration.getLogger(JCloudScaleServer.class);
	}
	
	@After("execution(at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServer.new())")
	public void constructorCalled() {
			
		log.info(String.format("Created new JCloudScaleServer instance"));
			
	}
	
	@After("execution(void at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServer.onConfigurationChange(..))")
	public void newConfig() {
			
		log.info(String.format("Successfully updated configuration in server"));
			
	}
	
	@Around("execution(String at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServer.createNewCloudObject(..)) && args(classname, *, *)")
	public Object newCloudObjectDeployed(ProceedingJoinPoint jp, String classname) throws Throwable {
			
		log.info(String.format("Creating new cloud object of runtime type %s",
				classname));
		
		Object ret = jp.proceed();
		
		log.info(String.format("ID of new cloud object is %s",
				ret));
		
		return ret;
			
	}
	
	@After("execution(String at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServer.startInvokingCloudObject(..)) && args(objectid, method, *, *)")
	public void startInvoking(String objectid, String method) {
		
		if(log.isLoggable(Level.INFO))	
			log.info(String.format("Scheduled execution of cloud object %s (method %s)",
															objectid, method));
			
	}
	
	@After("execution(void at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServer.destroyCloudObject(..)) && args(objectid)")
	public void destroyedObject(String objectid) {
		
		if(log.isLoggable(Level.INFO))	
			log.info(String.format("Destroyed cloud object %s", objectid));
			
	}
	
	@After("execution(void at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServer.shutdown())")
	public void shutdown() {
			
		log.info(String.format("Shutting down server!"));
			
	}
	
	@Around("execution(Object at.ac.tuwien.infosys.jcloudscale.utility.CgLibUtil.JCloudScaleReferenceHandler.intercept(..)) && args(object, method, *, *)")
	public Object referenceHandlerInvoked(ProceedingJoinPoint jp, Object object, Method method) throws Throwable {
		
		if(log.isLoggable(Level.INFO))
			log.info(String.format("Starting to invoke callback on reference type %s (method %s)",
								object.getClass().getName(), method.getName()));
		
		Object ret = jp.proceed();
		
		if(log.isLoggable(Level.INFO))
			log.info(String.format("Received callback answer. Returned result was %s", ret));
		
		return ret;
			
	}
	
	
}
