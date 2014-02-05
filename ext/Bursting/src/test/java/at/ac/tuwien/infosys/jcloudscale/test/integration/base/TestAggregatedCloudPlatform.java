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
package at.ac.tuwien.infosys.jcloudscale.test.integration.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.SerializableCloudObject;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject1;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;
import at.ac.tuwien.infosys.jcloudscale.vm.bursting.AggregatedVirtualHostPool;
 
public abstract class TestAggregatedCloudPlatform 
{
	protected AggregatedVirtualHostPool hostPool;
	protected static CloudManager cs = null;
	
	@After
    @JCloudScaleShutdown
    public void tearDown() throws Exception {
    }
	
	@Test
	public void testMultipleCloudDeployment() 
	{
		assertEquals(2, hostPool.getVirtualHostSubPools().size());
		assertEquals(0, hostPool.getCloudObjects().size());
		
		TestCloudObject1 obj1 = new TestCloudObject1();
		TestCloudObject1 obj2 = new TestCloudObject1();
		
		assertEquals(2, hostPool.getVirtualHostSubPools().size());
		assertEquals(2, hostPool.getCloudObjects().size());
		assertEquals(2, hostPool.getHostsCount());
		
		assertEquals(1, hostPool.getVirtualHostSubPools().get(0).getHostsCount());
		assertEquals(1, hostPool.getVirtualHostSubPools().get(0).getCloudObjects().size());

		assertEquals(1, hostPool.getVirtualHostSubPools().get(1).getHostsCount());
		assertEquals(1, hostPool.getVirtualHostSubPools().get(1).getCloudObjects().size());
		
		if(!obj1.multiplyAndConvert(123, 456).equals(obj2.multiplyAndConvert(456, 123)))
			throw new RuntimeException("Failed to compare two numbers! Deployment to multiple host pools fails!");
		
		obj1.killMeSoftly();
		obj2.killMeSoftly();
		
		assertEquals(2, hostPool.getVirtualHostSubPools().size());
		assertEquals(0, hostPool.getCloudObjects().size());
		assertEquals(2, hostPool.getHostsCount());
		
		assertEquals(1, hostPool.getVirtualHostSubPools().get(0).getHostsCount());
		assertEquals(0, hostPool.getVirtualHostSubPools().get(0).getCloudObjects().size());
		
		assertEquals(1, hostPool.getVirtualHostSubPools().get(1).getHostsCount());
		assertEquals(0, hostPool.getVirtualHostSubPools().get(1).getCloudObjects().size());
	}
	
	@Test
	public void testMigrateBetweenTwoClouds() throws InterruptedException 
	{
		assertEquals(2, hostPool.getVirtualHostSubPools().size());
		assertEquals(0, hostPool.getCloudObjects().size());
		
		SerializableCloudObject obj1 = new SerializableCloudObject();
		SerializableCloudObject obj2 = new SerializableCloudObject();
		ClientCloudObject cco1 = cs.getClientCloudObject(obj1);
		ClientCloudObject cco2 = cs.getClientCloudObject(obj2);
		
		assertEquals(2, cs.countCloudObjects());
		
		IHost managingHost1 = cs.getHost(cco1.getId());
		IHost managingHost2 = cs.getHost(cco2.getId());
		
		assertNotEquals(managingHost1.getId(), managingHost2.getId());
		cs.getHostPool().migrateObject(cco1.getId(), managingHost2);
		
		assertEquals(0, managingHost1.getCloudObjectsCount());
		assertEquals(2, managingHost2.getCloudObjectsCount());

		cco1 = cs.getClientCloudObject(obj1);
		managingHost1 = cs.getHost(cco1.getId());
		assertEquals(managingHost1.getId(), managingHost2.getId());
		
		
	}
	
	protected static class AggregatedScalingPolicy extends AbstractScalingPolicy
	{
		AtomicInteger lastObjectNumber = new AtomicInteger();
		
		public AggregatedScalingPolicy() {}

		@Override
		public synchronized IHost selectHost(ClientCloudObject newCloudObject,
				IHostPool hostPool) 
		{
			if(!(hostPool instanceof AggregatedVirtualHostPool))
				throw new RuntimeException("Unexpected Host Pool! Expecting AggregatedVirtualHostPool");
			
			AggregatedVirtualHostPool aggregatedHostPool = (AggregatedVirtualHostPool)hostPool;
			
			int objNumber = lastObjectNumber.getAndIncrement();
			int nextPoolId = objNumber % aggregatedHostPool.getVirtualHostSubPools().size();
			IHostPool subPool = aggregatedHostPool.getVirtualHostSubPools().get(nextPoolId);
			
			if(subPool.getHostsCount() > 0)
				return subPool.getHosts().iterator().next();
			else
				return subPool.startNewHost();
		}

		@Override
		public boolean scaleDown(IHost scaledHost, IHostPool hostPool) 
		{
			return false;
		}
		
	}
}
