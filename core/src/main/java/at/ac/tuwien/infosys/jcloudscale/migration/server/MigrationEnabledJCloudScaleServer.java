/*
   Copyright 2013 Fritz Schrogl

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
package at.ac.tuwien.infosys.jcloudscale.migration.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.logging.Logged;
import at.ac.tuwien.infosys.jcloudscale.migration.IMigrationEnabledJCloudScaleHost;
import at.ac.tuwien.infosys.jcloudscale.migration.serialization.ISerializationProvider;
import at.ac.tuwien.infosys.jcloudscale.migration.utility.MigrationUtil;
import at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServer;
import at.ac.tuwien.infosys.jcloudscale.server.ServerCloudObject;
import at.ac.tuwien.infosys.jcloudscale.utility.ReflectionUtil;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudObjectState;

/**
 * Migration-enabled version of {@link JCloudScaleServer}
 */
@Logged
public class MigrationEnabledJCloudScaleServer extends JCloudScaleServer implements IMigrationEnabledJCloudScaleHost {

	public MigrationEnabledJCloudScaleServer() throws JMSException, NamingException {
		super();
	}

	@Override
	public byte[] serializeToMigrate(String id) throws JCloudScaleException {
		ServerCloudObject cco = cloudObjects.get(UUID.fromString(id));
		Object cloudObject = cco.getObject();
		if (cloudObject == null)
			throw new JCloudScaleException(
					"Cloud object does not exist in server cache: " + id);

		try { 
			// Get SerializationProvider to serialize CloudObject
			ISerializationProvider sp = MigrationUtil.getSerializationProvider(
					cloudObject.getClass());

			// Get methods and fields that need to be invoked/reseted before
			// the object can be serialized (also collect from superclasses)
			Class<?> clazz = cloudObject.getClass();
			List<Set<Field>> fields2Reset = new ArrayList<Set<Field>>();
			List<List<Method>> methods2Invoke = new ArrayList<List<Method>>();
			while (clazz != null) {
				fields2Reset.add(MigrationUtil.getJCloudScaleFields(clazz));
				fields2Reset.add(MigrationUtil.getTransientFields(clazz));
				methods2Invoke.add(MigrationUtil.getSortedPreMigrate(clazz));
				clazz = clazz.getSuperclass();
			}

			// Invoke @PreMigrate methods; superclasses first (reverse order)
			for (int i = methods2Invoke.size() - 1; i >= 0; i--) {
				for (Method m : methods2Invoke.get(i)) {
					m.setAccessible(true);
					m.invoke(cloudObject, new Object[0]);
				}
			}

			// Reset transient + JCloudScale fields; order not important
			for (Set<Field> fieldSet : fields2Reset) {
				MigrationUtil.resetFields(cloudObject, fieldSet);
			}

			// Serialize
			try (InputStream is = sp.serialize(cloudObject);
					ByteArrayOutputStream bos = new ByteArrayOutputStream(is.available());)
			{
				while (is.available() > 0) {
					bos.write(is.read());
				}
				return bos.toByteArray();
			}
		} catch (Exception e) {
			logException(e);
			throw new JCloudScaleException(e,
					"Could not serialize cloud object: "
							+ cloudObject.getClass().getName());
		}
	}

	@Override
	public void deployMigratedCloudObject(String stringId, String classname, byte[] serializedCloudObject)
			throws JCloudScaleException {
		try {
			UUID uuid = UUID.fromString(stringId);
			Class<?> clazz = null;

			UUID clientId = JCloudScaleConfiguration.getConfiguration().common().clientID();
			
			// Setup the necessary classloader
			this.cloudObjectToClassloaderMap.put(uuid, getClassloaderForClient(clientId));
			
			try {
				clazz = Class.forName(classname, false, this.cloudObjectToClassloaderMap.get(uuid));
			} catch (ClassNotFoundException e) {
				logException(e);
				throw new JCloudScaleException(e,
						"Unable to load user class into custom classloader");
			}

			// Deploy migrated CloudObject
			ISerializationProvider sp = MigrationUtil.getSerializationProvider(clazz);
			Object migratedCloudObject = sp
					.deserialize(new ByteArrayInputStream(serializedCloudObject));

			// Get all fields and methods that must to be initialized/invoked
			List<Set<Field>> fields2Init = new ArrayList<Set<Field>>();
			List<List<Method>> methods2Invoke = new ArrayList<List<Method>>();
			while (clazz != null) {
				fields2Init.add(MigrationUtil.getTransientFields(clazz));
				methods2Invoke.add(MigrationUtil.getSortedPostMigrate(clazz));
				clazz = clazz.getSuperclass();
			}

			// Init transient fields
			for (Set<Field> fieldSet : fields2Init) {
				MigrationUtil.initTransientFields(migratedCloudObject, fieldSet);
			}

			// Inject JCloudScale fields
			ReflectionUtil.injectCloudId(migratedCloudObject, uuid);
			ReflectionUtil.injectEventSink(migratedCloudObject);

			// Invoke @PostMigrate methods
			for (List<Method> methodList : methods2Invoke) {
				for (Method m : methodList) {
					m.setAccessible(true);
					m.invoke(migratedCloudObject, new Object[0]);
				}
			}

			// Add the new cloud object to the list of managed objects
			ServerCloudObject sco = new ServerCloudObject();
			sco.setCloudObjectClass(clazz);
			sco.setId(uuid);
			sco.setObject(migratedCloudObject);
			sco.setState(CloudObjectState.IDLE);
			touchServerCloudObject(sco); 
			cloudObjects.put(uuid, sco);
		} catch (Exception e) {
			logException(e);
			throw new JCloudScaleException(e,
					"Unable to deploy migrated CloudObject to new host");
		}
	}

	@Override
	public void removeCloudObject(String id) {
		
		UUID theId = UUID.fromString(id);

		if (!this.cloudObjects.containsKey(theId))
			throw new JCloudScaleException(
					"Cloud object does not exist in server cache: " + id);
		
		touchServerCloudObject(this.cloudObjects.get(theId));
		this.cloudObjects.remove(theId);

		if (this.cloudObjectToClassloaderMap.containsKey(theId))
		{
			this.cloudObjectToClassloaderMap.remove(theId);
		}
	}

//	@Override
//	public long getCloudObjectSize(String objectId) {
//		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
//			UUID cloudObjectId = UUID.fromString(objectId);
//			Object cloudObject = cloudObjects.get(cloudObjectId);
//			if (cloudObject != null)
//				return JCloudScaleServerInstrumentation.getObjectSize(cloudObject);
//		} catch (Exception ex) {
//			log.severe("Error calculating cloud object size: " + ex.getMessage());
//			logException(ex);
//		}
//		return -1L;
//	}

	@SuppressWarnings("all")
	private void logException(Throwable e) {
		StackTraceElement[] trace = e.getStackTrace();
		StringBuilder sb = new StringBuilder();
		sb.append("Exception Trace\n");
		sb.append(e.getClass().getName() + ": ");
		if (e.getMessage() != null)
			sb.append(e.getMessage() + "\n");
		for (StackTraceElement el : trace) {
			sb.append("\tat " + el.toString());
			sb.append("\n");
		}
		this.log.severe(sb.toString());
	}

}
