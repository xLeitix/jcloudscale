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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.server.AbstractJCloudScaleServerRunner;
import at.ac.tuwien.infosys.jcloudscale.utility.ReferenceHashmap;
import at.ac.tuwien.infosys.jcloudscale.utility.ReflectionUtil;

public class JCloudScaleReferenceManager implements Closeable {
	
	private static JCloudScaleReferenceManager instance = null;
	
	private Hashtable<UUID, Object> references = new
			Hashtable<UUID, Object>();
	private Map<Object, UUID> referencesReverse = new ReferenceHashmap<>();
	
	private JCloudScaleReferenceCallbackListener callback;
	private IMQWrapper mq; 
	
	private UUID thisHostId;
	
	private JCloudScaleReferenceManager() {
		
		// if we are on server-side, we send to the fixed client UUID from config 
		// on client-side, we need to answer the correct host UUID
		if(JCloudScaleConfiguration.isServerContext()) {
			// XXX AbstractJCloudScaleServerRunner
			// thisHostId = JCloudScaleServerRunner.getInstance().getId();
			thisHostId = AbstractJCloudScaleServerRunner.getInstance().getId();
		} else {
			thisHostId = JCloudScaleConfiguration.getConfiguration().common().clientID();
		}
		
		try {
			mq = JCloudScaleConfiguration.createMQWrapper();
			mq.createTopicProducer(JCloudScaleConfiguration.getConfiguration().server().getCallbackResponseQueueName());
			
			// if we are on server-side, we register for callbacks to our server ID
			// on clide-side we use the fixed client UUID from config
			String filterLabel = null;
			if(JCloudScaleConfiguration.isServerContext())
				// XXX AbstractJCloudScaleServerRunner
				// filterLabel = JCloudScaleServerRunner.getInstance().getId().toString();
				filterLabel = AbstractJCloudScaleServerRunner.getInstance().getId().toString();
			else
				filterLabel = JCloudScaleConfiguration.getConfiguration().common().clientID().toString();
			mq.createTopicConsumer(
				JCloudScaleConfiguration.getConfiguration().server().getCallbackRequestQueueName(),
				"CS_HostId = '"+filterLabel+"'");
			
			callback = new JCloudScaleReferenceCallbackListener();
			mq.registerListener(callback);
		} catch (NamingException | JMSException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	public synchronized static JCloudScaleReferenceManager getInstance() {
		
		if(instance == null)
			instance = new JCloudScaleReferenceManager();
		
		return instance;
		
	}
	
	public Object[] processArguments(Constructor<?> constructor, Object[] origArguments) {
		
		boolean[] byRef = ReflectionUtil.findByRefParams(constructor);
		return replaceRefs(byRef, origArguments);
		
	}
	
	public Object[] processArguments(Method method, Object[] origArguments) {
		
		boolean[] byRef = ReflectionUtil.findByRefParams(method);
		return replaceRefs(byRef, origArguments);
		
	}
	
	public Object processReturn(Method method, Object origReturn) {
		
		boolean byRef = ReflectionUtil.isByRefReturn(method);
		if(byRef && origReturn != null) {
			UUID refId = isObjectManaged(origReturn)
					? getReferenceId(origReturn)
					: addReference(origReturn);
			return new JCloudScaleReference(refId, thisHostId, origReturn);
		} else {
			return origReturn;
		}
		
	}
	
	public Object processField(Field field, Object fieldValue) {
		
		boolean byRef = ReflectionUtil.isByRef(field);
		if(byRef) {
			UUID refId = isObjectManaged(fieldValue)
					? getReferenceId(fieldValue)
					: addReference(fieldValue);
			return new JCloudScaleReference(refId, thisHostId, fieldValue);
		} else {
			return fieldValue;
		}
		
	}
	
	public  Object getReference(UUID id) {
		if(!references.containsKey(id))
			return null;
		else
			return references.get(id);
	}
	
	private Object[] replaceRefs(boolean[] byRef, Object[] origArguments) {
		
		// now replace all byref params with actual references
		Object[] args = new Object[byRef.length];
		for(int i=0; i<byRef.length; i++) {
			if(byRef[i] && origArguments[i] != null) {
				UUID refId = isObjectManaged(origArguments[i])
						? getReferenceId(origArguments[i])
						: addReference(origArguments[i]);
				args[i] = new JCloudScaleReference(refId, thisHostId,
						origArguments[i]);
			} else {
				args[i] = origArguments[i];
			}
		}
		return args;

		
		}
	
	private boolean isObjectManaged(Object obj) {
		return referencesReverse.containsKey(obj);
	}
	
	private UUID addReference(Object obj) {
		UUID id = UUID.randomUUID();
		references.put(id, obj);
		return id;
	}
	
	
	
	private UUID getReferenceId(Object object) {
		if(!referencesReverse.containsKey(object))
			return null;
		else
			return referencesReverse.get(object);
	}
	
//	private void deleteReference(UUID id) {
//		references.remove(id);
//	}

	@Override
	public void close() {
		
		references.clear();
		referencesReverse.clear();
		callback.close();
		mq.close();
		
	}
	
	public synchronized static void closeInstance() {
		
		if(instance != null) 
		{
			instance.close();
			instance = null;
		}
		
	}
	
	
}
