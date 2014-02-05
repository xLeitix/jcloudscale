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
package at.ac.tuwien.infosys.jcloudscale.test.testobject;

import javax.jms.JMSException;

import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.DestructCloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.EventSink;
import at.ac.tuwien.infosys.jcloudscale.monitoring.IEventSink;
import at.ac.tuwien.infosys.jcloudscale.test.testevents.EventDrivenPolicyTestEvent;

@CloudObject
public class TestCloudObjectWithEvent {
	
	@EventSink
	private IEventSink sink;
	
	@DestructCloudObject
	public void killMeSoftlyAndSendEvent() throws JMSException{
		
		EventDrivenPolicyTestEvent myEvent = new EventDrivenPolicyTestEvent();
		myEvent.setValue(100);
		sink.trigger(myEvent);
		
	}
	
}
