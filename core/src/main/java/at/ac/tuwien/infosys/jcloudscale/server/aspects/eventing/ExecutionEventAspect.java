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


import java.lang.reflect.Field;
import java.util.UUID;

import javax.jms.JMSException;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ExecutionFailedEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ExecutionFinishedEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ExecutionStartedEvent;
import at.ac.tuwien.infosys.jcloudscale.server.AbstractJCloudScaleServerRunner;
import at.ac.tuwien.infosys.jcloudscale.server.CloudObjectInvocation;
import at.ac.tuwien.infosys.jcloudscale.utility.InvocationStatus;

@Aspect
public class ExecutionEventAspect extends EventAspect {
	
	@Before("within(at.ac.tuwien.infosys.jcloudscale.server.CloudObjectInvocation) && execution(public void java.lang.Thread+.run())")
	public void matchStartInvocation(JoinPoint jp) throws JCloudScaleException {
		
        try {
        	Field this$0 = jp.getThis().getClass().getDeclaredField("this$0");
        	this$0.setAccessible(true);
			CloudObjectInvocation inv = (CloudObjectInvocation) this$0.get(jp.getThis());
			// XXX AbstractJCloudScaleServerRunner
			// UUID serverId = JCloudScaleServerRunner.getInstance().getId();
			UUID serverId = AbstractJCloudScaleServerRunner.getInstance().getId();
			ExecutionStartedEvent event = new ExecutionStartedEvent();
			initializeBaseEventProperties(event);
			event.setMethod(inv.getMethod().getName());
			event.setObjectId(inv.getObjectId());
			event.setRequestId(inv.getRequestId());
			event.setObjectType(inv.getCloudObject().getClass());
			event.setHostId(serverId);
			getMqHelper().sendEvent(event);
			log.finer("Sent start invocation event for object "+inv.getObjectId());
		} catch (Exception e) {
			e.printStackTrace();
			log.severe("Error while triggering ExecutionStartedEvent: "+e.getMessage());
		}
			
	}
	
	@After("within(at.ac.tuwien.infosys.jcloudscale.server.CloudObjectInvocation) && execution(public void java.lang.Thread+.run())")
	public void matchFinishInvocation(JoinPoint jp) throws JCloudScaleException {
		
        try {
        	Field this$0 = jp.getThis().getClass().getDeclaredField("this$0");
        	this$0.setAccessible(true);
			CloudObjectInvocation inv = (CloudObjectInvocation) this$0.get(jp.getThis());
			// XXX AbstractJCloudScaleServerRunner
			// UUID serverId = JCloudScaleServerRunner.getInstance().getId();
			UUID serverId = AbstractJCloudScaleServerRunner.getInstance().getId();
			if(inv.getStatus().equals(InvocationStatus.FINISHED)) {
				ExecutionFinishedEvent event = new ExecutionFinishedEvent();
				initializeBaseEventProperties(event);
				event.setMethod(inv.getMethod().getName());
				event.setObjectId(inv.getObjectId());
				event.setRequestId(inv.getRequestId());
				event.setObjectType(inv.getCloudObject().getClass());
				event.setHostId(serverId);
				getMqHelper().sendEvent(event);
				log.finer("Sent finished invocation event for object "+inv.getObjectId());
			} else if(inv.getStatus().equals(InvocationStatus.FAULTED)) {
				ExecutionFailedEvent event = new ExecutionFailedEvent();
				initializeBaseEventProperties(event);
				event.setMethod(inv.getMethod().getName());
				event.setObjectId(inv.getObjectId());
				event.setRequestId(inv.getRequestId());
				event.setException(inv.getError());
				event.setObjectType(inv.getCloudObject().getClass());
				event.setHostId(serverId);
				getMqHelper().sendEvent(event);
				log.finer("Sent failed invocation event for object "+inv.getObjectId());
			} else {
				log.severe("Finished invocation with unsupported outcome: "+inv.getStatus());
			}
		} catch (JMSException | NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
			log.severe("Error while triggering ExecutionFinishedEvent: "+e.getMessage());
		}
			
	}
	
}
