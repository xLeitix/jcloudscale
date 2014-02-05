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
package at.ac.tuwien.infosys.jcloudscale.sample.service.jcloudscale;

import java.util.logging.Level;

import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleConfigurationProvider;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfigurationBuilder;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.OpenstackConfiguration;

/**
 * Specifies the JCloudScale configuration that will be used during this run.
 */
public class JCloudScaleConfig {
	
	/**
	 * This method will be called by jcloudscale whenever application configuration will be required.
	 */
	@JCloudScaleConfigurationProvider
	public static JCloudScaleConfiguration configure() {
		
		return localConfig();
		
	}
	
	private static JCloudScaleConfiguration localConfig() {
		
		//
		// Detecting the message queue address that should be used.
		//
		String mqAddress = OpenstackConfiguration.getMessageQueueAddress();
		int port = Integer.parseInt(mqAddress.substring(mqAddress.indexOf(':')+1));
		mqAddress = mqAddress.replace(":"+Integer.toString(port), "");

		//
		// Defining cloud management parameters. 
		// They are not needed for this tests as we are running on static configuration.
		//
		final String username = "";
		final String password = "";
		final String tenantname = "";
		final String imageName = "JCloudScale_JST";
		final String identityUrl = "http://openstack.infosys.tuwien.ac.at:5000/v2.0";
		
		//
		// Creating configuration instance with required parameters.
		//
		JCloudScaleConfiguration config = JCloudScaleConfigurationBuilder
				.createOpenStackConfigurationBuilder(new ScalingPolicy(), Level.SEVERE, 
									mqAddress, port, false, 
									identityUrl, 
									tenantname, imageName, username, password)
									.build();

		return config;
		
	}
	
}
