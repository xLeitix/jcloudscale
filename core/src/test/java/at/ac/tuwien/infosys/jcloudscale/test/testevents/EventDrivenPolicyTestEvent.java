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
package at.ac.tuwien.infosys.jcloudscale.test.testevents;

import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.Event;

public class EventDrivenPolicyTestEvent extends Event {
	
	private static final long serialVersionUID = 1L;
	
	private int value;

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
	
}
