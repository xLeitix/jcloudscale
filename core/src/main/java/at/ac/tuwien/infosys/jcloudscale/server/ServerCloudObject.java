/*
   Copyright 2014 Philipp Leitner 

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import at.ac.tuwien.infosys.jcloudscale.vm.CloudObjectState;

public class ServerCloudObject {
	
	private UUID id;
	private CloudObjectState state = CloudObjectState.IDLE;
	private Object object;
	private List<String> executingMethods = new ArrayList<String>();
	private Class<?> cloudObjectClass;
	private long lastTouched;
	
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public CloudObjectState getState() {
		return state;
	}
	public void setState(CloudObjectState state) {
		this.state = state;
	}
	public Object getObject() {
		return object;
	}
	public void setObject(Object object) {
		this.object = object;
	}
	public List<String> getExecutingMethods() {
		return executingMethods;
	}
	public void setExecutingMethods(List<String> executingMethods) {
		this.executingMethods = executingMethods;
	}
	public Class<?> getCloudObjectClass() {
		return cloudObjectClass;
	}
	public void setCloudObjectClass(Class<?> cloudObjectClass) {
		this.cloudObjectClass = cloudObjectClass;
	}
	public long getLastTouched() {
		return lastTouched;
	}
	public void setLastTouched(long lastTouched) {
		this.lastTouched = lastTouched;
	}
	
}
