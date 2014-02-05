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


public class Server implements Serializable {
	 
	private static final long serialVersionUID = 1L;
	
	private String serverId;
	private String ipAdd;
	private boolean isStatic;
	
	public String getServerId() {
		return serverId;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	public String getIpAdd() {
		return ipAdd;
	}
	public void setIpAdd(String ipAdd) {
		this.ipAdd = ipAdd;
	}
	public boolean isStatic() {
		return isStatic;
	}
	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(isStatic) {
			sb.append("STATIC ");
		}
		sb.append(serverId);
		sb.append(" (");
		sb.append(ipAdd);
		sb.append(")");
		return sb.toString();
	}
	
}
