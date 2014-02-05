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

import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.vm.VirtualHostPool;

/**
 * The {@link MigrationExecutor} actually migrates {@link CloudObject}s from one
 * Cloud host to another. The class implements the {@link IMigrationTrigger}
 * interface and thus is by default used as callback class for the rules engine.
 * <p>
 * The {@link MigrationExecutor} utilizes a planner engine to choose an
 * appropriate {@link CloudObject} for migration and a {@link IVirtualHost}
 * cloud host as migration target.
 */
public class MigrationExecutor {

	private Logger log;
	private VirtualHostPool sourceHostpool;
	private VirtualHostPool targetHostpool;
	
	// =========================================================================
	// Constructor
	// =========================================================================

	public MigrationExecutor(VirtualHostPool sourceHostpool, VirtualHostPool targetHostpool)
			throws JCloudScaleException {

		this.log = JCloudScaleConfiguration.getLogger(this);
		this.sourceHostpool = sourceHostpool;
		this.targetHostpool = targetHostpool;

	}
	
	public UUID migrateObjectToHost(UUID cloudObjectId, IMigrationEnabledVirtualHost destinationHost, MigrationReason reason) {
		
		boolean successful = true;
		MigrationDescriptor md = null;
		IMigrationEnabledVirtualHost sourceHost = null;

		ReentrantReadWriteLock coLock = null;
		
		try {

			// Create migration info
			md = new MigrationDescriptor(UUID.randomUUID(),
					System.currentTimeMillis(), cloudObjectId, null, destinationHost.getId(), reason);
			log.info("Starting 'objectToHost' migration (migrationId=" + md.getId()
					+ ", reasonCode="
					+ ((reason != null) ? reason.getCode() : null) + ")");

			// Lock cloudObject
			try {
				coLock = sourceHostpool.getCOLock(cloudObjectId);
				coLock.writeLock().lock();
			} catch (Exception e) {
				md.setFailMsg("Unable to retrieve object lock for cloudObject/" + cloudObjectId);
				throw e;
			}

			// update CO state
			// hostpool.findManagingHost(cloudObjectId).getCloudObjectById(cloudObjectId).beginMigration();
			
			// Retrieve origin host
			try {
				sourceHost = (IMigrationEnabledVirtualHost) sourceHostpool.findManagingHost(cloudObjectId);
				md.setSourceHost(sourceHost.getId());
			} catch (ClassCastException cce) {
				md.setFailMsg("Source host/" + sourceHostpool.findManagingHost(cloudObjectId).getId()
						+ " doesn't implement " + IMigrationEnabledVirtualHost.class);
				throw cce;
			} catch (Exception e) {
				md.setFailMsg("Can't find managing cloud host for cloudObject id=" + cloudObjectId);
				throw e;
			}

			// Update migration info
			Class<?> cloudObjectType = sourceHost.getCloudObjectType(md.getCloudObjectId());
			md.setCloudObjectClazz(cloudObjectType);

			md.setDestinationHost(destinationHost.getId());
			log.info("Migration/" + md.getId() + ": will migrate cloudObject/" + cloudObjectId
					+ " to host/" + md.getDestinationHost());

			migrate(cloudObjectId, cloudObjectType, sourceHost, destinationHost, coLock);
			
			// Destroy old cloud object 
			sourceHostpool.removeCOFromPool(sourceHost, cloudObjectId, coLock);
			
			// and update cloud object's mappings
			targetHostpool.addNewMapping(destinationHost, cloudObjectId);
			
		} catch (Exception e) {
			e.printStackTrace();
			successful = false;
			log.severe("Aborting migration (id=" + md.getId() + ")");
			if (md.getFailMsg() == null) {
				md.setFailMsg(parseStackTrace(e).toString());
			}
			// TODO: that seems like a bad pattern
			if(e instanceof JCloudScaleException)
				throw (JCloudScaleException)e;
			else
				throw new JCloudScaleException(e);
		} finally {
			if (md != null) {
				md.setSuccessful(successful);
				md.setEndTime(System.currentTimeMillis());
			}
			
			// update CO state
			// hostpool.findManagingHost(cloudObjectId).getCloudObjectById(cloudObjectId).endMigration();
			
			if (coLock != null) {
				coLock.writeLock().unlock();
			}
		}

		return md.getId();
		
	}
	
	// =========================================================================
	// Internal methods
	// =========================================================================

	// Converts an exception's stack trace to a string, so that it can be stored
	// in a MigrationDescriptor
	private StringBuilder parseStackTrace(Exception e) {
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
		return sb;
	}

	// Performs the actual migration of the cloud object
	private void migrate(UUID cloudObjectId, Class<?> cloudObjectType, IMigrationEnabledVirtualHost sourceHost,
			IMigrationEnabledVirtualHost destinationHost,
			ReentrantReadWriteLock coLock) throws Exception {
		// Check if write lock is already set; set if not
		if (!coLock.isWriteLockedByCurrentThread()) {
			coLock.writeLock().lock();
		}
		
		// If sourceHost == destinationHost we must do nothing!
		if (sourceHost.getId().equals(destinationHost.getId()))
			return;

		// Invocation of @PreMigrate methods + resetting of transient fields and
		// JCloudScale-specific fields is done on the source host
		byte[] packedCloudObject = sourceHost.serializeToMigrate(cloudObjectId);
		
		log.info("Received packed cloud object from host "+sourceHost.getId().toString());
		
		// Initialization of transient and JCloudScale-specific fields and
		// invocation of @PostMigrate methods is done on the destination host
		destinationHost.deployMigratedCloudObject(cloudObjectId, cloudObjectType, packedCloudObject,
				sourceHost.getProxyObject(cloudObjectId), CloudManager.getInstance().getRefQueue());

		log.info("Deployed cloud object at new host "+destinationHost.getId().toString());
		
	}

}
