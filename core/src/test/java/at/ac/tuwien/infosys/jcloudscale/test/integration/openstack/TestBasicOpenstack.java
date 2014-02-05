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
package at.ac.tuwien.infosys.jcloudscale.test.integration.openstack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject1;
import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHostPool;

public class TestBasicOpenstack extends at.ac.tuwien.infosys.jcloudscale.test.integration.base.TestBasic
{
	private static OpenstackHelper staticInstanceManager;
	
	@BeforeClass
	public static void setup() throws Exception 
	{
		//
		// Initializing JCloudScale
		//
		JCloudScaleConfiguration cfg = ConfigurationHelper.createDefaultCloudTestConfiguration().build();

		JCloudScaleClient.setConfiguration(cfg);
		
		//
		// Starting "static" hosts.
		//
		staticInstanceManager = new OpenstackHelper(cfg);
		staticInstanceManager.startStaticInstances();
		
		cs = CloudManager.getInstance();
	}
	
	@Test
	public void testStartLargerHost() throws Exception
	{
		//altering scaling policy configuration to call some methods on cloudObject prior to work
		JCloudScaleConfiguration config = JCloudScaleConfiguration.getConfiguration();
		final AbstractScalingPolicy oldPolicy = config.common().scalingPolicy();
		config.common().setScalingPolicy(new AbstractScalingPolicy() {
			@Override
			public IHost selectHost(ClientCloudObject newCloudObject, IHostPool hostPool) {
				System.out.println("Starting new host");
				return hostPool.startNewHost("m1.small");
			}
			@Override
			public boolean scaleDown(IHost scaledHost, IHostPool hostPool) {
				return oldPolicy.scaleDown(scaledHost, hostPool);
			}
		});
		
		IVirtualHost host = null;
		
		try
		{
			TestCloudObject1 co = new TestCloudObject1();
			UUID coId = cs.getClientCloudObject(co).getId();
			host = cs.getHost(coId);
			assertEquals("m1.small", host.getDeclaredInstanceSize());
			assertTrue(staticInstanceManager.verifyWithOpenstack(host, "m1.small"));
		}
		finally
		{
			//restoring previous scaling policy
			config.common().setScalingPolicy(oldPolicy);
		}
	}
	
	@AfterClass
	@JCloudScaleShutdown
	public static void tearDown() throws Exception 
	{
		staticInstanceManager.shudownStaticInstances();
	}
	
	@After
	public void cleanup() throws Exception
	{
		IVirtualHostPool pool = cs.getHostPool();
		for(IHost host : pool.getHosts())
			pool.shutdownHost(host.getId());
	}

}
