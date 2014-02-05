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
package at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring;

import java.util.UUID;

public abstract class ObjectEvent extends PredefinedEvent {

	private static final long serialVersionUID = 7890;
	
	protected UUID hostId;
	protected UUID objectId;
	protected Class<?> objectType;

	public Class<?> getObjectType() {
		return objectType;
	}

	public void setObjectType(Class<?> objectType) {
		this.objectType = objectType;
	}

	public UUID getObjectId() {
		return objectId;
	}

	public void setObjectId(UUID objectId) {
		this.objectId = objectId;
	}
	
	public UUID getHostId() {
		return hostId;
	}

	public void setHostId(UUID hostId) {
		this.hostId = hostId;
	}
	
}
