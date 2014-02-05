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
package at.ac.tuwien.infosys.jcloudscale.test.integration.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import javax.jms.JMSException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.monitoring.EventCorrelationEngine;
import at.ac.tuwien.infosys.jcloudscale.monitoring.MonitoringMetric;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractEventDrivenScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObjectWithEvent;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class TestEventDrivenScalingPolicy {

	protected static CloudManager cs = null;
	
	@Test 
	public void testTriggerEventDrivenPolicy() throws JMSException, InterruptedException {
		
		JCloudScaleConfiguration config = JCloudScaleClient.getConfiguration();
		final AbstractScalingPolicy oldPolicy = config.common().scalingPolicy();
		final List<Integer> receivedInts = new LinkedList<>(); 
		
		config.common().setScalingPolicy(new AbstractEventDrivenScalingPolicy() {
			
			private AbstractScalingPolicy policy = oldPolicy;
			
			@Override
			public IHost selectHost(ClientCloudObject newCloudObject,
					IHostPool hostPool) {
				return policy.selectHost(newCloudObject, hostPool);
			}
			
			@Override
			public boolean scaleDown(IHost scaledHost, IHostPool hostPool) {
				return policy.scaleDown(scaledHost, hostPool);
			}
			
			@Override
			public void onEvent(IHostPool hostPool, Object triggeringValue,
					long timestamp) {
				
				assertNotNull(hostPool);
				assertEquals(100, triggeringValue);
				receivedInts.add((Integer)triggeringValue);
				
			}
		});
		
		MonitoringMetric metric = new MonitoringMetric();
		metric.setName("TestMetric");
		metric.setEpl("select value from EventDrivenPolicyTestEvent");
		metric.setResultField("value");
		EventCorrelationEngine.getInstance().registerMetric(metric);
		
		EventCorrelationEngine.getInstance().getMetricsDatabase().registerEventDrivenScalingPolicy("TestMetric",
			(AbstractEventDrivenScalingPolicy)config.common().scalingPolicy());
		
		TestCloudObjectWithEvent co = new TestCloudObjectWithEvent();
		co.killMeSoftlyAndSendEvent();
		
		Thread.sleep(2000);
		
		int firstVal = receivedInts.get(0);
		assertEquals(100, firstVal);
		assertTrue(receivedInts.size() == 1);
		
		config.common().setScalingPolicy(oldPolicy);
		
	}
}
