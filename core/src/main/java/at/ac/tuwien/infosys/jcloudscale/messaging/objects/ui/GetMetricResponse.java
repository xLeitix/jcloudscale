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

import java.io.Serializable;

import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;

 
public class GetMetricResponse extends MessageObject {

	private static final long serialVersionUID = 1L;
	
	private Serializable value;
	private String metricName;

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public Serializable getValue() {
		return value;
	}

	public void setValue(Serializable value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return String.format("Name: %s / Value: %s", metricName, value.toString());
	}
}
