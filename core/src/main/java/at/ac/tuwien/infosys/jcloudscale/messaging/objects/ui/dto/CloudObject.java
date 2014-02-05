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
package at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.dto;

import java.io.Serializable;
import java.util.List;


public class CloudObject implements Serializable {
	 
	private static final long serialVersionUID = 1L;
	
	private String id;
	private String host;
	private String type;
	private String state;
	private List<String> executingMethods;
	
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public List<String> getExecutingMethods() {
		return executingMethods;
	}
	public void setExecutingMethods(List<String> executingMethods) {
		this.executingMethods = executingMethods;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id);
		sb.append(" (");
		sb.append(type);
		sb.append(") Deployed at: ");
		sb.append(host);
		sb.append(" Current State: ");
		sb.append(state);
		sb.append(" ");
		if(executingMethods != null && executingMethods.size() > 0) {
			if(executingMethods.size() == 1) {
				sb.append("("+executingMethods.get(0)+")");
			}
			else {
				sb.append("(");
				for(String s : executingMethods) {
					sb.append(s);
					sb.append(" ");
				}
				sb.append(")");
			}
		}
		return sb.toString();
	}
	
}
