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
package at.ac.tuwien.infosys.jcloudscale.sample.sentiment;

import java.util.HashMap;
import java.util.UUID;

import com.espertech.esper.epl.generated.EsperEPL2GrammarParser.concatenationExpr_return;

import at.ac.tuwien.infosys.jcloudscale.monitoring.EventCorrelationEngine;
import at.ac.tuwien.infosys.jcloudscale.monitoring.MonitoringMetric;
import at.ac.tuwien.infosys.jcloudscale.policy.IScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHostPool;

public class SentimentScalingPolicy implements IScalingPolicy {

	private Object lock = new Object();

	@Override
	public IHost selectHost(ClientCloudObject newCloudObject, IVirtualHostPool hostPool) 
	{

		System.out.println("Starting to select host");
		
		IHost selected = null;
		
		// lock the policy so that we do not concurrently fire up a bunch of hosts
		synchronized (lock) {
			
			System.out.println("-------------------------------------------------------------");

			// for each host, check if the avg processing time for the host is below 50ms
			// (unused hosts we are always using)
			for (IHost host : hostPool.getHosts()) {
				if(host.getId() == null)
					continue;
				double procTimeForHost = getProcessingTimeForHost(host.getId().toString());
				System.out.println(String.format(
					"Host %s (%s) has reported average processing time %f",
					host.getId().toString(), host.getIpAddress(), procTimeForHost
				));
				if((procTimeForHost != -1 && procTimeForHost < 50) || host.getCloudObjectsCount() == 0) {
					selected = host;
					break;
				}
			}
			System.out.println("-------------------------------------------------------------");
			if(selected == null)
				System.out.println("Found no suitable host, scaling up");
			else
				System.out.println("Using host "+selected.getId().toString());
			System.out.println("-------------------------------------------------------------");

		}
			
		// no suitable host found, start a new one (and register monitoring for the host)
		// (and start a reserve instance)
		if(selected == null) {
			selected = hostPool.startNewHost();
			register(selected .getId().toString());
		}
		return selected;
	}

	@Override
	public boolean scaleDown(IHost host, IVirtualHostPool hostPool) {

		// we scale down iff
		// - it is online
		// - the host is currently unused
		// - this is none of our static instances, which we never tear down
		// - there is at least one other unused host

		boolean result = true;
		
		synchronized (lock) {

			System.out.println("-------------------------------------------------------------");
			System.out.println("Checking whether to scale down host "+host.getId().toString());
			
			if(!host.isOnline()) {
				result = false;
				System.out.println("Not scaling down. Host is offline");
			}

			if(host.getCloudObjectsCount() > 0) {
				result = false;
				System.out.println("Not scaling down. Host is in use");
			}

			if(!otherUnusedHost(hostPool, host.getId())) {
				result = false;
				System.out.println("Not scaling down. Host is the last unused host");
			}
		}
		if(result) {
			System.out.println("Scaling down host "+host.getId().toString());
			unregister(host.getId().toString());
		}
		return result;
	}

	private boolean otherUnusedHost(IVirtualHostPool hostPool, UUID id) {

		for(IHost host : hostPool.getHosts()) {
			if(!host.getId().equals(id) && host.getCloudObjectsCount() == 0)
				return true;
		}

		return false;

	}

	private void register(String serverId) {
		// here we register a new event that monitors a specific host
		// note that this is based on the custom events that we trigger in TwitterSentimentAnalyzer.java 
		MonitoringMetric metric = new MonitoringMetric();
		metric.setName("AvgTweetProcessingTimeMetric_"+serverId);
		metric.setEpl(
				String.format(
						"select avg(duration) as avg_dur from ClassificationDurationEvent(hostId=\"%s\").win:time(180 sec)",
						serverId)
				);
		EventCorrelationEngine.getInstance().registerMetric(metric);
		
		// register callback
		
	}

	private void unregister(String serverId) {
		EventCorrelationEngine.getInstance().unregisterMetric("AvgTweetProcessingTimeMetric_"+serverId);
	}

	private double getProcessingTimeForHost(String serverId) {
		// get the latest value for this metric from our monitoring database
		Object value = 
				EventCorrelationEngine.getInstance().getMetricsDatabase().getLastValue("AvgTweetProcessingTimeMetric_"+serverId);
		if(value == null)
			return -1;
		else
			if (((HashMap) value).get("avg_dur") == null)
				return 0;
			else
				return (Double) ((HashMap) value).get("avg_dur");
	}
}
