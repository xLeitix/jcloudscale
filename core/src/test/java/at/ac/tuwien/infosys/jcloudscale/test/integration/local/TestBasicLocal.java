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
package at.ac.tuwien.infosys.jcloudscale.test.integration.local;

import org.junit.After;
import org.junit.Before;

import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;

public class TestBasicLocal extends at.ac.tuwien.infosys.jcloudscale.test.integration.base.TestBasic {
	
	@Before 
	public void setup() throws Exception 
	{
		JCloudScaleClient.setConfiguration(ConfigurationHelper.createDefaultTestConfiguration()
				.withMonitoring(true)//someone has to check what happens when monitoring is disabled.
				.build());
		
		cs = CloudManager.getInstance();
	}
	
	@After
	@JCloudScaleShutdown
    public void tearDown() throws Exception {
    }
}
