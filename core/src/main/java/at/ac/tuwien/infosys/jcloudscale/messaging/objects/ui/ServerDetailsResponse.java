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
package at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui;

import java.util.List;

import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.dto.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.dto.Server;
import at.ac.tuwien.infosys.jcloudscale.monitoring.CPUUsage;
import at.ac.tuwien.infosys.jcloudscale.monitoring.RAMUsage;


public class ServerDetailsResponse extends MessageObject {
	 
	private static final long serialVersionUID = 1L;
	
	private Server server;
	private List<CloudObject> objects;
	private CPUUsage currLoad;
	private RAMUsage currRam;
	
	public Server getServer() {
		return server;
	}
	public void setServer(Server server) {
		this.server = server;
	}
	public List<CloudObject> getObjects() {
		return objects;
	}
	public void setObjects(List<CloudObject> objects) {
		this.objects = objects;
	}
	public CPUUsage getCurrLoad() {
		return currLoad;
	}
	public void setCurrLoad(CPUUsage currLoad) {
		this.currLoad = currLoad;
	}
	public RAMUsage getCurrRam() {
		return currRam;
	}
	public void setCurrRam(RAMUsage currRam) {
		this.currRam = currRam;
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append("Server Details:\n");
		sb.append("\t");
		sb.append(server.toString());
		sb.append("\n");
		sb.append("Cloud Objects (");
		sb.append(objects.size());
		sb.append("):\n");
		for(CloudObject co : objects) {
			sb.append("\t");
			sb.append(co.toString());
			sb.append("\n");
		}
		sb.append("Load Data:\n");
		sb.append("\t Current CPU = "+currLoad);
		sb.append("\n");
		sb.append("\t Current RAM = "+currRam);
		sb.append("\n");
		return sb.toString();
		
	}
	
}
