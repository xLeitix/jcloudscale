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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.FieldSignature;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.JCloudScaleReferenceManager;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.TimeoutException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.GetStaticValueObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.GetStaticValueReturnObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.SetStaticValueRequest;
import at.ac.tuwien.infosys.jcloudscale.utility.CgLibUtil;
import at.ac.tuwien.infosys.jcloudscale.utility.SerializationUtil;

@Aspect
public class StaticFieldAspect 
{
	
	private Logger log;
	
	public StaticFieldAspect() {
		
		log = JCloudScaleConfiguration.getLogger(this);
		
	}
	
	@Around("args(newval) && set(@at.ac.tuwien.infosys.jcloudscale.annotations.CloudGlobal static * *.*)")
	public void writeStaticValueToClient(ProceedingJoinPoint pjp, Object newval) throws Throwable {
		
		if(!JCloudScaleConfiguration.isServerContext())
		{
			pjp.proceed();
			return;
		}
		
		try(IMQWrapper mq = JCloudScaleConfiguration.createMQWrapper()) {
			UUID corrId = UUID.randomUUID();
			
			FieldSignature sig = (FieldSignature) pjp.getSignature();
			Field field = sig.getField();
			
			Object newValProcessed = JCloudScaleReferenceManager.getInstance().processField(field, newval);
			byte[] serialzed = SerializationUtil.serializeToByteArray(newValProcessed);
					
			SetStaticValueRequest req = new SetStaticValueRequest();
			req.setField(field.getName());
			req.setClassName(field.getDeclaringClass().getCanonicalName());
			req.setValue(serialzed);
			
			mq.createQueueProducer(JCloudScaleConfiguration.getConfiguration().server().getStaticFieldWriteRequestQueueName());
			mq.createTopicConsumer(JCloudScaleConfiguration.getConfiguration().server().getStaticFieldWriteResponseTopicName(),
					"JMSCorrelationID = '"+corrId.toString()+"'");
			
			// we send request and wait for response to ensure that we in fact 
			// changed the value before continuing. 
			mq.requestResponse(req, corrId);
			
		} catch (JMSException | NamingException | IOException e) {
			e.printStackTrace();
			log.severe("Could not write static field: "+e.getMessage());
		}
			
	}
	
	@Around("get(@at.ac.tuwien.infosys.jcloudscale.annotations.CloudGlobal static * *.*)")
	public Object readStaticValueFromClient(ProceedingJoinPoint pjp) throws Throwable {
		
		if(!JCloudScaleConfiguration.isServerContext())
			return pjp.proceed();
		
		Object returned = null;
		
		try(IMQWrapper mq = JCloudScaleConfiguration.createMQWrapper()) {
			
			FieldSignature sig = (FieldSignature) pjp.getSignature();
			Field field = sig.getField();
			
			GetStaticValueObject req = new GetStaticValueObject();
			req.setClassName(field.getDeclaringClass().getCanonicalName());
			req.setField(field.getName());
			
			mq.createQueueProducer(JCloudScaleConfiguration.getConfiguration().server().getStaticFieldReadRequestQueueName());
			UUID corrId = UUID.randomUUID();
			mq.createTopicConsumer(JCloudScaleConfiguration.getConfiguration().server().getStaticFieldReadResponsesQueueName(),
				"JMSCorrelationID = '"+corrId.toString()+"'");
			GetStaticValueReturnObject ret = (GetStaticValueReturnObject) mq.requestResponse(req, corrId);
			
			ClassLoader classLoader = field.getDeclaringClass().getClassLoader();
			returned = SerializationUtil.getObjectFromBytes(ret.getValue(), classLoader);
			returned = JCloudScaleReferenceManager.getInstance().processField(field, returned);
			returned = CgLibUtil.replaceRefWithProxy(returned, classLoader);
			
			
		} catch(JCloudScaleException e) {
			// this should happen if the by-ref type did not have a default constructor
			e.printStackTrace();
			throw e;
		} catch (JMSException | NamingException | TimeoutException | ClassNotFoundException | IOException e) {
			e.printStackTrace();
			log.severe("Could not write static field: "+e.getMessage());
		}
		
		return returned;
	}
	
//	@After("args(newval) && set(@at.ac.tuwien.infosys.jcloudscale.annotations.CloudGlobal static * *.*)")
//	public void writeStaticValueToRiak(JoinPoint jp, Object newval) throws JCloudScaleException {
//		
//		if(jp.getThis() != null) {
//			
//			// we are only setting the static field outside of initialization
//			// (this should be captured by THIS being null, hopefully :) )
//			String name = jp.getSignature().getDeclaringTypeName();
//			
//			String fieldName = jp.getSignature().getName(); 
//			
//			try 
//			{
//				JCloudScaleConfiguration.getConfiguration()
//				.server().keyValueStorage().getKeyValueStorageWrapper()
//				.setValue(name, fieldName, newval);
//			} catch (Exception e) {
//				e.printStackTrace();
//				throw new JCloudScaleException(e, "Failed to store value in Riak");
//			}
//			
//		}
//			
//	}
//	
//	@Before("get(@at.ac.tuwien.infosys.jcloudscale.annotations.CloudGlobal static * *.*)")
//	public void readStaticValue(JoinPoint jp) throws JCloudScaleException {
//		
//		String name = jp.getSignature().getDeclaringTypeName();
//		String fieldName = jp.getSignature().getName();
//		
//		ClassLoader cl = jp.getThis().getClass().getClassLoader();
//		
//		try {
//			Object val = JCloudScaleConfiguration.getConfiguration()
//					.server().keyValueStorage().getKeyValueStorageWrapper().getValue(name, fieldName, cl, Object.class);
//			if(val != null) {
//				Field field = lookupField(jp.getSignature());
//				field.setAccessible(true);
//				field.set(null, val);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new JCloudScaleException(e, "Failed to set static value from Riak");
//		}
//	}
//
//	private Field lookupField(Signature sig) throws SecurityException, NoSuchFieldException {
//		
//		Class<?> clazz = sig.getDeclaringType();
//		return clazz.getDeclaredField(sig.getName());
//		
//	}
	
}
