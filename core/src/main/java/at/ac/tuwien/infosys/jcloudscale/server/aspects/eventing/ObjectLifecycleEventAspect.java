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
package at.ac.tuwien.infosys.jcloudscale.server.aspects.eventing;


import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ObjectCreatedEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ObjectDestroyedEvent;
import at.ac.tuwien.infosys.jcloudscale.server.AbstractJCloudScaleServerRunner;
import at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServer;

@Aspect
public class ObjectLifecycleEventAspect extends EventAspect {
	@AfterReturning(pointcut = "execution(public String at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServer.createNewCloudObject(..))", returning = "ret")
	public void matchObjectCreated(JoinPoint jp, String ret) throws JCloudScaleException {
		
        try {
        	ObjectCreatedEvent event = new ObjectCreatedEvent();
        	initializeBaseEventProperties(event);
        	UUID objectId = UUID.fromString(ret);
        	event.setObjectId(objectId);
        	JCloudScaleServer server = (JCloudScaleServer)jp.getThis();
        	// XXX AbstractJCloudScaleServerRunner
        	// UUID serverId = JCloudScaleServerRunner.getInstance().getId();
        	UUID serverId = AbstractJCloudScaleServerRunner.getInstance().getId();
        	event.setHostId(serverId);
        	ClassLoader cl = server.getCloudObjectClassLoader(objectId);
        	Class<?> theClazz = Class.forName((String)(jp.getArgs()[0]), true, cl);
        	event.setObjectType(theClazz);
        	getMqHelper().sendEvent(event);
        	log.finer("Sent object created for object "+objectId.toString());
        } catch (Exception e) {
			e.printStackTrace();
			log.severe("Error while triggering ObjectCreatedEvent: "+e.getMessage());
		}
			
	}
	
	@Around("execution(public void at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServer.destroyCloudObject(..))")
	public void matchObjectDestroyed(ProceedingJoinPoint pjp) throws Throwable {
		
		Class<?> cloudObjectType = null;
		JCloudScaleServer server = null;
		UUID cloudObjectId = null;
        try {
        	server = (JCloudScaleServer)pjp.getThis();
			cloudObjectId = UUID.fromString((String)(pjp.getArgs()[0]));
			
			Object cloudObject = server.getCloudObject(cloudObjectId);
			if(cloudObject != null)
			    cloudObjectType = cloudObject.getClass();
        } catch (Exception e) {
			e.printStackTrace();
			log.severe("Error while triggering ObjectDestroyedEvent: "+e.getMessage());
		}
		
		pjp.proceed();
		
        try {
        	ObjectDestroyedEvent event = new ObjectDestroyedEvent();
        	initializeBaseEventProperties(event);
        	// XXX AbstractJCloudScaleServerRunner
        	// UUID serverId = JCloudScaleServerRunner.getInstance().getId();
        	UUID serverId = AbstractJCloudScaleServerRunner.getInstance().getId();
        	event.setHostId(serverId);
        	event.setObjectId(cloudObjectId);
        	event.setObjectType(cloudObjectType);
        	getMqHelper().sendEvent(event);
        	log.finer("Sent object destroyed for object "+cloudObjectId);
        } catch (Exception e) {
			e.printStackTrace();
			log.severe("Error while triggering ObjectDestroyedEvent: "+e.getMessage());
		}
			
	}
	
}
