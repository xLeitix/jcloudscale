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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.JCloudScaleReference;
import at.ac.tuwien.infosys.jcloudscale.management.JCloudScaleReferenceManager;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.TimeoutException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.StartCallbackObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.StartCallbackResponseObject;
import at.ac.tuwien.infosys.jcloudscale.utility.CgLibUtil;
import at.ac.tuwien.infosys.jcloudscale.utility.SerializationUtil;

public class ServerCallbackManager {
	
	private static ServerCallbackManager instance;
	
	private Logger log;
	
	private ServerCallbackManager() {
		log = JCloudScaleConfiguration.getLogger(this);
	}
	
	public synchronized static ServerCallbackManager getInstance() {
		
		if(instance == null)
			instance = new ServerCallbackManager();
		
		return instance;
		
	}
	
	public Object callback(JCloudScaleReference ref, Method method, Object[] params,
			Class<?>[] paramTypes, ClassLoader classloader) {
		
		Object[] processedParams = JCloudScaleReferenceManager.getInstance().processArguments(method, params);
		
		byte[] serialized = null;
		try {
			serialized = SerializationUtil.serializeToByteArray(processedParams);
		} catch (IOException e) {
			e.printStackTrace();
			log.severe("Could not serialize objects to byte array "+e.getMessage());
		}
		
		String[] paramNames = new String[paramTypes.length];
		for(int i=0; i<paramNames.length; i++)
			paramNames[i] = paramTypes[i].getCanonicalName();
		
		StartCallbackObject callback = new StartCallbackObject();
		callback.setMethod(method.getName());
		callback.setParamNames(paramNames);
		callback.setParams(serialized);
		callback.setRef(ref);
		
		StartCallbackResponseObject response = null;
		try(IMQWrapper mq = JCloudScaleConfiguration.createMQWrapper()) {
			
			mq.createTopicProducer(JCloudScaleConfiguration.getConfiguration().server().getCallbackRequestQueueName());
			UUID callbackCorrid = UUID.randomUUID();
			mq.createTopicConsumer(
					JCloudScaleConfiguration.getConfiguration().server().getCallbackResponseQueueName(),
					"JMSCorrelationID = '"+callbackCorrid.toString()+"'");
			response = (StartCallbackResponseObject) mq.requestResponseToCSHost(callback,
					callbackCorrid, ref.getReferencingHostId());
			
		} catch (JMSException | TimeoutException | NamingException e) {
			e.printStackTrace();
			log.severe("Could not send message: "+e.getMessage());
		}
			
		Object theReturn = null;
		try {
			theReturn = SerializationUtil.getObjectFromBytes(response.getResponse(), classloader);
			// see if the result is a reference, and if it is, replace it
			// theReturn = JCloudScaleReferenceManager.getInstance().processReturn(method, returnVal);
			if(theReturn != null)
				theReturn = CgLibUtil.replaceRefWithProxy(theReturn, classloader);
		} catch(JCloudScaleException e) {
			// this should happen if the by-ref type did not have a default constructor
			e.printStackTrace();
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
			log.severe("Could not deserialize objects: "+e.getMessage());
		}
		
		return theReturn;
		
		
	}
	
}
