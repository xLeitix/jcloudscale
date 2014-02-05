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

import at.ac.tuwien.infosys.jcloudscale.IJCloudScaleServer;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ObjectDestroyedEvent;
import at.ac.tuwien.infosys.jcloudscale.server.aspects.eventing.ObjectLifecycleEventAspect;

/**
 * Add migration-specific methods to {@link IJCloudScaleServer}
 * <p>
 * IJCloudScaleServerMigration is JCloudScaleServer-side
 * 
 * @see IVirtualHostMigration
 */
public interface IMigrationEnabledJCloudScaleHost extends IJCloudScaleServer {

	/**
	 * Prepares a cloud object for migration. After successful preparation the
	 * cloud object is serialized and returned to the caller as byte array.
	 * 
	 * @param objectId
	 *            The cloud object to prepare/migrate
	 * @return The serialized cloud object as byte array
	 */
	public byte[] serializeToMigrate(String objectId);

	/**
	 * Deploys a previously serialized cloud object. Deployment includes
	 * deserialization, initialization, preparation and registration of the
	 * cloud object's id at the cloud host
	 * 
	 * @param objectId
	 *            The id of the cloud object
	 * @param classname
	 *            Classname of the serialized cloud object
	 * @param serializedCloudObject
	 *            The serialized cloud object
	 */
	public void deployMigratedCloudObject(String objectId, String classname,
			byte[] serializedCloudObject);

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
	public void removeCloudObject(String id);

	/**
	 * Returns the size of the cloud object. The returned value is JVM-specific,
	 * thus only values calculated using the same JVM-implementation can be
	 * compared with each other.
	 * 
	 * @param objectId
	 *            The id of the cloud object, which size should be calculated
	 * @return The size
	 */
	// public long getCloudObjectSize(String objectId);

}
