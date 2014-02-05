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

import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.Event;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.StateEvent;

/**
 * Describes the reason why a migration request was triggered.
 * <p>
 * The rules engine can create a MigrationReason object to describe why a
 * migration request was triggered. Each method defined in
 * {@link IMigrationTrigger} accepts an optional {@link MigrationReason} object
 * as parameter which will than be stored inside the migration's
 * {@link MigrationDescriptor}.
 */
public class MigrationReason implements Serializable {

	private static final long serialVersionUID = 1L;

	// =========================================================================
	// Statistics
	// =========================================================================

	protected String code;
	protected String description;
	protected Event event;

	// =========================================================================
	// Constructors
	// =========================================================================

	public MigrationReason() {
	}

	public MigrationReason(String code, String description, Event event) {
		super();
		this.code = code;
		this.description = description;
		this.event = event;
	}

	@Override
	public String toString() {
		String eId = (event != null && event.getEventId() != null) ? event
				.getEventId().toString() : "null";
		return "(Code=" + code + ", eventId=" + eId + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof MigrationReason) {
			MigrationReason clone = (MigrationReason) obj;
			if ((code == null && clone.getCode() == null) || code.equals(clone.getCode()))
				if ((description == null && clone.getDescription() == null)
						|| description.equals(clone.getDescription()))
					if (event == null && clone.getEvent() == null)
						return true;
					else if (event.getEventId() == null && clone.getEvent().getEventId() == null)
						return true;
					else if (event.getEventId().equals(clone.getEvent().getEventId()))
						return true;
		}
		return false;
	}

	// =========================================================================
	// Getter and Setter
	// =========================================================================

	/**
	 * Getter method. A custom tailored code to identify the reason for the
	 * migration triggered.
	 * 
	 * @return The code of the migration's reason
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Setter method.
	 * 
	 * @param code
	 *            The code of the migration's reason
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Getter method. A custom, more elaborate description why the migration was
	 * triggered.
	 * 
	 * @return A description of the migration's reason
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Setter method.
	 * 
	 * @param description
	 *            A description of the migration's reason
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Getter method. The {@link Event} that eventually triggered the migration.
	 * 
	 * @return An event, either a subclass of {@link StateEvent} or
	 *         {@link ObjectMigrationEvent}
	 */
	public Event getEvent() {
		return event;
	}

	/**
	 * Setter method.
	 * 
	 * @param event
	 *            An event
	 */
	public void setEvent(Event event) {
		this.event = event;
	}

}
