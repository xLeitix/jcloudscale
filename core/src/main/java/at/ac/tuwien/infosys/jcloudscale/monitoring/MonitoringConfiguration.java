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
package at.ac.tuwien.infosys.jcloudscale.monitoring;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.CPUEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.Event;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ExecutionFailedEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ExecutionFinishedEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ExecutionStartedEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ObjectCreatedEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ObjectDestroyedEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.RAMEvent;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MonitoringConfiguration implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private boolean isEnabled = false;
	private String queueName = "CS_Monitoring";
	private long systemEventsInterval = 5000;//5 sec.
	private List<String> monitoredEvents = new ArrayList<String>();
	
	private boolean triggerCpuEvents = true;//TODO: should we do this like that? 
	private boolean triggerRamEvents = true;
	private boolean triggerNetworkEvents = true;
	
	public MonitoringConfiguration()
	{
		monitoredEvents.add(ExecutionStartedEvent.class.getName());
		monitoredEvents.add(ExecutionFinishedEvent.class.getName());
		monitoredEvents.add(ExecutionFailedEvent.class.getName());
		monitoredEvents.add(ObjectCreatedEvent.class.getName());
		monitoredEvents.add(ObjectDestroyedEvent.class.getName());
		monitoredEvents.add(CPUEvent.class.getName());
		monitoredEvents.add(RAMEvent.class.getName());
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	
	public String getQueueName() {
		return queueName;
	}
	
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	
	Iterable<String> getMonitoredEvents() 
	{
		return monitoredEvents;
	}
	
//	public boolean removeMonitoredEvent(Class<? extends Event> clazz)
//	{
//		return this.monitoredEvents.remove(clazz.getName());
//	}
	
	public void addMonitoredEvent(Class<? extends Event> clazz)
	{
		if(!this.monitoredEvents.contains(clazz.getName()))
			this.monitoredEvents.add(clazz.getName());
	}
	
	public long getSystemEventsInterval() {
		return systemEventsInterval;
	}

	public void setSystemEventsInterval(long systemEventsInterval) {
		this.systemEventsInterval = systemEventsInterval;
	}

	public boolean triggerCpuEvents() {
		return triggerCpuEvents;
	}

	public void setTriggerCpuEvents(boolean triggerCpuEvents) {
		this.triggerCpuEvents = triggerCpuEvents;
	}

	public boolean triggerRamEvents() {
		return triggerRamEvents;
	}

	public void setTriggerRamEvents(boolean triggerRamEvents) {
		this.triggerRamEvents = triggerRamEvents;
	}
	
	public boolean triggerNetworkEvents() {
		return triggerNetworkEvents;
	}
	
	public void setTriggerNetworkEvents(boolean triggerNetworkEvents) {
		this.triggerNetworkEvents = triggerNetworkEvents;
	}
	
}
