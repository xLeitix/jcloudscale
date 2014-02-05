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
package at.ac.tuwien.infosys.jcloudscale.migration;

import java.lang.ref.ReferenceQueue;
import java.util.UUID;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ObjectDestroyedEvent;
import at.ac.tuwien.infosys.jcloudscale.server.aspects.eventing.ObjectLifecycleEventAspect;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;

/**
 * Adds migration-specific methods to {@link IVirtualHost}
 * <p>
 * IVirtualHostMigration is JCloudScaleClient-side
 * 
 * @see IMigrationEnabledJCloudScaleHost
 */
public interface IMigrationEnabledVirtualHost extends IVirtualHost {

	/**
	 * Prepares a cloud object for migration. After successful preparation the
	 * cloud object is serialized and returned to the caller as byte array.
	 * 
	 * @param cloudObjectId
	 *            The cloud object to prepare/migrate
	 * @return The serialized cloud object as byte array
	 * @throws JCloudScaleException
	 *             If an error occurs during preparation and/or serialization
	 */
	public byte[] serializeToMigrate(UUID cloudObjectId) throws JCloudScaleException;

	/**
	 * Deploys a previously serialized cloud object. Deployment includes
	 * deserialization, initialization, preparation and registration of the
	 * cloud object's id at the cloud host
	 * 
	 * @param cloudObjectId
	 *            The id of the cloud object
	 * @param cloudObjectType
	 *            The implementation class of the cloud object
	 * @param serializedCloudObject
	 *            The serialized cloud object
	 * @param proxy
	 *            The cloud objects proxy object
	 * @throws JCloudScaleException
	 *             If an error occurs during deployment
	 */
	public void deployMigratedCloudObject(UUID cloudObjectId, Class<?> cloudObjectType,
			byte[] serializedCloudObject, Object proxy, ReferenceQueue<Object> queue)
			throws JCloudScaleException;

	/**
	 * Destroys the remaining cloud object on the source host after a successful
	 * migration. This method is basically only a wrapper for
	 * {@link #destroyCloudObject(String)} to prevent
	 * {@link ObjectLifecycleEventAspect} from issuing a
	 * {@link ObjectDestroyedEvent}.
	 * 
	 * @param id
	 *            The id of the migrated cloud object
	 */
	public void removeCloudObject(UUID cloudObjectId);

	/**
	 * Returns the size of the cloud object. The returned value is JVM-specific,
	 * thus only values calculated using the same JVM-implementation can be
	 * compared with each other.
	 * 
	 * @param cloudObjectId
	 *            The id of the cloud object, which size should be calculated
	 * @return The size
	 */
	public long getCloudObjectSize(UUID cloudObjectId);

	/**
	 * Getter for the proxy object of the cloud object
	 * 
	 * @param cloudObjectId
	 *            The id of the cloud object
	 * @return The proxy object for the cloud object
	 */
	public Object getProxyObject(UUID cloudObjectId);

}
