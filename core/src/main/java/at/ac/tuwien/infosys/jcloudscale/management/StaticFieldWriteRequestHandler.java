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

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.SetStaticValueRequest;
import at.ac.tuwien.infosys.jcloudscale.utility.CgLibUtil;
import at.ac.tuwien.infosys.jcloudscale.utility.SerializationUtil;

public class StaticFieldWriteRequestHandler implements MessageListener, Closeable {
	
	private Logger log;
	private ExecutorService threadpool;
	private IMQWrapper mq; 
	
	public StaticFieldWriteRequestHandler(IMQWrapper mq) {
		this.mq = mq;
		log = JCloudScaleConfiguration.getLogger(this);
		threadpool = Executors.newCachedThreadPool();
	}
	
	@Override
	public void close()
	{
		if(threadpool != null)
		{
			threadpool.shutdown();
			threadpool = null;
		}
	}
	
	@Override
	public void onMessage(Message msg) {
		
		threadpool.execute(new MessageHandler(msg));
		
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
				if(!(o instanceof SetStaticValueRequest)) {
					throw new JCloudScaleException("Processed unknown message from static fields queue: "+o.toString());
				}
				SetStaticValueRequest writeReq = (SetStaticValueRequest) o;
				setStaticField(writeReq);
				
				//sending response to ensure that we continue execution after field is set.
				UUID correlationId = UUID.fromString(msg.getJMSCorrelationID());
				mq.oneway(null, correlationId);
				
			} catch (Exception e) {
				e.printStackTrace();
				log.severe(e.getMessage());
			}
		}
		
		private void setStaticField(SetStaticValueRequest req) throws ClassNotFoundException, IOException,
			NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
			
			byte[] serData = req.getValue();
			
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Class<?> clazz = Class.forName(req.getClassName(), true, cl);
			Field field = clazz.getDeclaredField(req.getField());
			field.setAccessible(true);
			Object object = SerializationUtil.getObjectFromBytes(serData, cl);
			try {
				object = CgLibUtil.replaceRefWithProxy(object, cl);
			} catch(JCloudScaleException e) {
				// this should happen if the by-ref type did not have a default constructor
				e.printStackTrace();
				throw e;				
			} catch (Throwable e) {
				e.printStackTrace();
			}
			
			if(!java.lang.reflect.Modifier.isStatic(field.getModifiers()))
				throw new JCloudScaleException("Supposed to set a non-static field of "+
						"object "+clazz.getName()+" ("+field.getName()+")");
			
			// now we actually set this value in the class
			// note that we pass null for the object, as this is supposed to be a static field
			field.set(null, object);
			
		}

	}

	

}
