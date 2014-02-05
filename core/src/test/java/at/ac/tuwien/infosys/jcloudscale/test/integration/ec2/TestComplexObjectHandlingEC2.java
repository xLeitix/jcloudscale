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
package at.ac.tuwien.infosys.jcloudscale.test.integration.ec2;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHostPool;

//UNSET ignore if you actually want to run EC2 tests
//by default we don't do it because, hey, it's not free :)
//
//(you will also need a correct aws.props
//file in the classpath and an ActiveMQ server)
// @Ignore
public class TestComplexObjectHandlingEC2 extends at.ac.tuwien.infosys.jcloudscale.test.integration.base.TestComplexObjectHandling
{
	private static EC2Helper staticInstanceManager;
	
	@BeforeClass 
	public static void setup() throws Exception
	{
		//
		// Initializing JCloudScale
		//
		JCloudScaleConfiguration cfg = ConfigurationHelper.createDefaultAmazonTestConfiguration();

		JCloudScaleClient.setConfiguration(cfg);
		
		//
		// Starting "static" hosts.
		//
		staticInstanceManager = new EC2Helper(cfg);
		staticInstanceManager.startStaticInstances();

		cs = CloudManager.getInstance();
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
