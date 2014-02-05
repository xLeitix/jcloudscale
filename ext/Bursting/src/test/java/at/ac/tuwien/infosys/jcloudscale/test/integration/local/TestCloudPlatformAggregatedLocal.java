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
package at.ac.tuwien.infosys.jcloudscale.test.integration.local;

import org.junit.Before;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.test.integration.base.TestAggregatedCloudPlatform;
import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import at.ac.tuwien.infosys.jcloudscale.vm.bursting.AggregatedCloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.bursting.AggregatedVirtualHostPool;
import at.ac.tuwien.infosys.jcloudscale.vm.localVm.LocalCloudPlatformConfiguration;

public class TestCloudPlatformAggregatedLocal extends TestAggregatedCloudPlatform 
{
	@Before
	public void setup() throws Exception 
	{
		JCloudScaleConfiguration cfg = ConfigurationHelper.createDefaultTestConfiguration()
														.with(new AggregatedScalingPolicy())
														 .build();
		
		CloudPlatformConfiguration cloudPlatform = cfg.server().cloudPlatform();
		LocalCloudPlatformConfiguration secondCloudPlatform = new LocalCloudPlatformConfiguration();
		secondCloudPlatform.setClasspath("*;lib/*;lib/");
		secondCloudPlatform.setStartupDirectory("target/");
		secondCloudPlatform.addCustomJVMArgs("-showversion");
		secondCloudPlatform.setJavaHeapSizeMB(50);
		
		AggregatedCloudPlatformConfiguration cloudPlatformConfig = new AggregatedCloudPlatformConfiguration(cloudPlatform, secondCloudPlatform);
		cfg.server().setCloudPlatform(cloudPlatformConfig);
		
		JCloudScaleClient.setConfiguration(cfg);
		
		JCloudScaleClient.getClient();
		cs = CloudManager.getInstance();
		hostPool =  (AggregatedVirtualHostPool)cs.getHostPool();
	}
}
