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
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.GetStaticValueObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.GetStaticValueReturnObject;
import at.ac.tuwien.infosys.jcloudscale.utility.SerializationUtil;

public class StaticFieldReadRequestHandler implements MessageListener, Closeable {
	
	private Logger log;
	private ExecutorService threadpool;
	private IMQWrapper mq; 
	
	public StaticFieldReadRequestHandler() {
		log = JCloudScaleConfiguration.getLogger(this);
		threadpool = Executors.newCachedThreadPool();
		try {
			mq = JCloudScaleConfiguration.createMQWrapper();
			mq.createTopicProducer(JCloudScaleConfiguration.getConfiguration().server().getStaticFieldReadResponsesQueueName());
		} catch (NamingException | JMSException e) {
			e.printStackTrace();
			log.severe("Unable to register to message queue: "+e.getMessage());
		}
	}
	
	@Override
	public void onMessage(Message msg) {
		
		threadpool.execute(new MessageHandler(msg));
		
	}
	
	@Override
	public void close() {
		mq.close();
		
		if(threadpool != null)
		{
			threadpool.shutdown();
			threadpool = null;
		}
	}
	
	private class MessageHandler implements Runnable {

		private Message msg;
		
		public MessageHandler(Message msg) {
			this.msg = msg;
		}
		
		@Override
		public void run() {
			
			
			
			if(!(msg instanceof ObjectMessage)) {
				throw new JCloudScaleException("Processed unknown message from static fields queue: "+msg.toString());
			} 
			
			Object o;
			try {
				
				o = ((ObjectMessage)msg).getObject();
				if(!(o instanceof GetStaticValueObject)) {
					throw new JCloudScaleException("Processed unknown message from static fields queue: "+o.toString());
				}
				
				GetStaticValueObject readReq = (GetStaticValueObject) o;
				
				Object value = readStaticField(readReq);
				byte[] ser = SerializationUtil.serializeToByteArray(value);
				GetStaticValueReturnObject returnObj = new GetStaticValueReturnObject();
				returnObj.setValue(ser);
				
				mq.oneway(returnObj, UUID.fromString(msg.getJMSCorrelationID()));
				
			} catch (Exception e) {
				e.printStackTrace();
				log.severe(e.getMessage());
			}
			
			
		}
		
		private Object readStaticField(GetStaticValueObject req) throws ClassNotFoundException, IOException,
			NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
			
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Class<?> clazz = Class.forName(req.getClassName(), true, cl);
			Field field = clazz.getDeclaredField(req.getField());
			
			field.setAccessible(true);
			
			
			if(!java.lang.reflect.Modifier.isStatic(field.getModifiers()))
				throw new JCloudScaleException("Supposed to get a value from a non-static field of "+
						"object "+clazz.getName()+" ("+field.getName()+")");
			
			// now we actually set this value in the class
			// note that we pass null for the object, as this is supposed to be a static field
			Object val = field.get(null);
			
			// we also need to replace this with a reference, if it is a reference field
			val = JCloudScaleReferenceManager.getInstance().processField(field, val);
			
			return val;
			
		}

	}
}
