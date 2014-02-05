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
import java.lang.reflect.Method;
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
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.StartCallbackObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.StartCallbackResponseObject;
import at.ac.tuwien.infosys.jcloudscale.utility.CgLibUtil;
import at.ac.tuwien.infosys.jcloudscale.utility.ReflectionUtil;
import at.ac.tuwien.infosys.jcloudscale.utility.SerializationUtil;

public class JCloudScaleReferenceCallbackListener implements MessageListener, Closeable {
	
	private Logger log;
	private ExecutorService threadpool;
	private IMQWrapper mq; 
	
	public JCloudScaleReferenceCallbackListener() {
		log = JCloudScaleConfiguration.getLogger(this);
		threadpool = Executors.newCachedThreadPool();
		try {
			mq = JCloudScaleConfiguration.createMQWrapper();
			mq.createTopicProducer(
					JCloudScaleConfiguration.getConfiguration().server().getCallbackResponseQueueName());
		} catch (NamingException | JMSException e) {
			e.printStackTrace();
			log.severe("Cannot connect to message queue: "+e.getMessage());
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
				throw new JCloudScaleException("Processed unknown message from callback queue: "+msg.toString());
			} 
			
			Object o;
			try {
				o = ((ObjectMessage)msg).getObject();
				if(!(o instanceof StartCallbackObject)) {
					throw new JCloudScaleException("Processed unknown message from callback queue: "+o.toString());
				}
				StartCallbackObject callback = (StartCallbackObject) o;
				executeCallback(callback);
			} catch (JMSException e) {
				e.printStackTrace();
			}
			
			
		}
		
		private void executeCallback(StartCallbackObject callback) {
			
			StartCallbackResponseObject response = new StartCallbackResponseObject();
			byte[] retSerialized = null;
			
			if(callback.getMethod().equals("getIntValue")) {
				System.out.println();
			}
			
			JCloudScaleReferenceManager refs = JCloudScaleReferenceManager.getInstance();
			
			Object theObject = refs.getReference(callback.getRef().getReferenceObjectId());
			
			ClassLoader contextClassloader = theObject.getClass().getClassLoader();
			
			Object[] params;
			try {
				params = SerializationUtil.getObjectArrayFromBytes(callback.getParams(), contextClassloader);
				
				Class<?>[] clazzes = ReflectionUtil.getClassesFromNames(callback.getParamNames(),
						contextClassloader);
				
				params = CgLibUtil.replaceRefsWithProxies(params, contextClassloader);
				
				Method theMethod = ReflectionUtil.findMethod(theObject.getClass(),
						callback.getMethod(), clazzes);
				Object result = theMethod.invoke(theObject, params);
				
				// see if the result is a reference, and if it is, replace it
				Object theReturn = JCloudScaleReferenceManager.getInstance().processReturn(theMethod, result);
				
				retSerialized = SerializationUtil.serializeToByteArray(theReturn);
			
				if(response.getException() == null)
					response.setResponse(retSerialized);
			
				mq.oneway(response, UUID.fromString(msg.getJMSCorrelationID()));
			} catch (Throwable e) {
				e.printStackTrace();
				log.severe("Unable to send callback answer: "+e.getMessage());
			}
			
		}

	}

}
