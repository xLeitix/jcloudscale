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

import java.io.Serializable;
import java.util.UUID;

/**
 * A {@link MigrationDescriptor} stores meta data about each migration executed
 * - successful or not. Migration descriptions are stored and managed by the
 * {@link MigrationStatistics} class
 */
public class MigrationDescriptor implements Serializable {

	private static final long serialVersionUID = 1L;

	// =========================================================================
	// Statistics
	// =========================================================================

	private UUID id;
	private boolean successful;
	private String failMsg;
	private long startTime;
	private long endTime;
	private Class<?> cloudObjectClazz;
	private UUID cloudObjectId;
	private UUID sourceHost;
	private UUID destinationHost;
	private MigrationReason reason;

	// =========================================================================
	// Constructor
	// =========================================================================

	public MigrationDescriptor() {
	}

	public MigrationDescriptor(UUID id, long startTime, UUID cloudObjectId,
			UUID sourceHost, UUID destinationHost, MigrationReason reason) {
		super();
		this.id = id;
		this.startTime = startTime;
		this.cloudObjectId = cloudObjectId;
		this.sourceHost = sourceHost;
		this.destinationHost = destinationHost;
		this.reason = reason;
	}

	public MigrationDescriptor(UUID id, boolean successful, String failMsg,
			long startTime,
			long endTime, Class<?> cloudObjectClazz, UUID cloudObjectId,
			UUID sourceHost, UUID destinationHost, MigrationReason reason) {
		super();
		this.id = id;
		this.successful = successful;
		this.failMsg = failMsg;
		this.startTime = startTime;
		this.endTime = endTime;
		this.cloudObjectClazz = cloudObjectClazz;
		this.cloudObjectId = cloudObjectId;
		this.sourceHost = sourceHost;
		this.destinationHost = destinationHost;
		this.reason = reason;
	}

	// =========================================================================
	// Getter and Setter
	// =========================================================================

	/**
	 * Getter method. A {@link MigrationDescriptor}'s id uniquely identifies
	 * that descriptor
	 * 
	 * @return The unique id
	 */
	public UUID getId() {
		return id;
	}

	/**
	 * Setter method
	 * 
	 * @param id
	 *            A unique id for this {@link MigrationDescriptor}
	 */
	public void setId(UUID id) {
		this.id = id;
	}

	/**
	 * Getter method. Indicates if the migration, described by this
	 * {@link MigrationDescriptor}, was successfully executed.
	 * 
	 * @return <code>true</code> if the described migration succeeded otherwise
	 *         <code>false</code>
	 */
	public boolean isSuccessful() {
		return successful;
	}

	/**
	 * Setter method.
	 * 
	 * @param successful
	 *            <code>true</code> or <code>false</code>
	 */
	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

	/**
	 * Getter method. The fail message contains a textual description of the
	 * reason the migration failed
	 * 
	 * @return A string containing the fail reason
	 */
	public String getFailMsg() {
		return failMsg;
	}

	/**
	 * Setter method
	 * 
	 * @param failMsg
	 *            A string containing the migration's fail reason
	 */
	public void setFailMsg(String failMsg) {
		this.failMsg = failMsg;
	}

	/**
	 * Getter method. The time (in millis) the migration described was
	 * initiated.
	 * 
	 * @return The starting time of the migration (in millis)
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * Setter method.
	 * 
	 * @param startTime
	 *            The starting time of the migration (in millis)
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * Getter method. The time (in millis) the migration described was finished.
	 * 
	 * @return The end time of the migration (in millis)
	 */
	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	/**
	 * Getter method. The {@link Class} of the cloud object that was migrated
	 * during the migration described took place
	 * 
	 * @return The class of the migrated cloud object
	 */
	public Class<?> getCloudObjectClazz() {
		return cloudObjectClazz;
	}

	/**
	 * Setter method.
	 * 
	 * @param cloudObjectClazz
	 *            The class of the migrated cloud object
	 */
	public void setCloudObjectClazz(Class<?> cloudObjectClazz) {
		this.cloudObjectClazz = cloudObjectClazz;
	}

	/**
	 * Getter method. The unique id of the cloud object that was migrated during
	 * the migration described took place
	 * 
	 * @return The unique id of the cloud object migrated
	 */
	public UUID getCloudObjectId() {
		return cloudObjectId;
	}

	/**
	 * Setter method.
	 * 
	 * @param cloudObjectId
	 *            The unique id of the cloud object migrated
	 */
	public void setCloudObjectId(UUID cloudObjectId) {
		this.cloudObjectId = cloudObjectId;
	}

	/**
	 * Getter method. The unique id of the source cloud host of the migration.
	 * The source cloud host of a migration is the host from which the cloud
	 * object is migrated away.
	 * 
	 * @return The unique id of the source cloud host
	 */
	public UUID getSourceHost() {
		return sourceHost;
	}

	/**
	 * Setter method.
	 * 
	 * @param sourceHost
	 *            The unique id of the source cloud host
	 */
	public void setSourceHost(UUID sourceHost) {
		this.sourceHost = sourceHost;
	}

	/**
	 * Getter method. The unique id if the destination cloud host of the
	 * migration. The destination cloud host of a migration is the host to which
	 * the cloud object is migrated to.
	 * 
	 * @return The unique id of the destination cloud host
	 */
	public UUID getDestinationHost() {
		return destinationHost;
	}

	/**
	 * Setter method
	 * 
	 * @param destinationHost
	 *            The unique id of the destination cloud host
	 */
	public void setDestinationHost(UUID destinationHost) {
		this.destinationHost = destinationHost;
	}

	/**
	 * Getter method. The {@link MigrationReason} is an optional parameter. It
	 * can be handed over to the migration method invoked and will be attached
	 * to the resulting {@link MigrationDescriptor} of that migration. Hence the
	 * {@link MigrationReason} can be used to give additional information why
	 * this migration was initiated at all.
	 * 
	 * @return The {@link MigrationReason} for this migration. Can be
	 *         <code>null</code>
	 */
	public MigrationReason getReason() {
		return reason;
	}

	/**
	 * Setter method.
	 * 
	 * @param reason
	 *            The {@link MigrationReason} for this migration. Can be
	 *            <code>null</code>
	 */
	public void setReason(MigrationReason reason) {
		this.reason = reason;
	}

}
