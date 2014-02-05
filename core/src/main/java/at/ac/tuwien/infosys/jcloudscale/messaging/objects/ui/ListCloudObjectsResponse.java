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

import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.dto.CloudObject;


public class ListCloudObjectsResponse extends MessageObject {
	 
	private static final long serialVersionUID = 1L;
	
	private List<CloudObject> objects = new LinkedList<CloudObject>();

	public List<CloudObject> getObjects() {
		return objects;
	}

	public void setObjects(List<CloudObject> objects) {
		this.objects = objects;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Cloud Objects (");
		sb.append(objects.size());
		sb.append("):\n");
		for(CloudObject co : objects) {
			sb.append("\t");
			sb.append(co.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
	
}
