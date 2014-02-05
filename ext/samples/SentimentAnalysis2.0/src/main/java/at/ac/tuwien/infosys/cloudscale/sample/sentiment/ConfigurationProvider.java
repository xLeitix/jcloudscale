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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;

import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleConfigurationProvider;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfigurationBuilder;

public class ConfigurationProvider {
	
	@JCloudScaleConfigurationProvider
	public static JCloudScaleConfiguration getConfiguration()
			throws FileNotFoundException, IOException
	{
		
		// this method delivers the configuration for jcloudscale
		JCloudScaleConfiguration cfg = JCloudScaleConfigurationBuilder
				// enable local configuration for testing ...
				 .createLocalConfigurationBuilder()
				// or Openstack configuration to actually deploy to the cloud
//				.createOpenStackConfigurationBuilder("openstack.props",
//						"128.130.172.197")
				.withGlobalLoggingLevel(Level.SEVERE)
 				.with(new SentimentScalingPolicy())
//				.with(new CPUScalingPolicy())
//				.with(new FixedNumberScalingPolicy())
				.withMonitoring(true)
 				.withMonitoringEvents(ClassificationDurationEvent.class)
				.build();
		
		// this governs how often we run the scaling-down check for each thread
		// (check every 5 minutes)
		cfg.common().setScaleDownIntervalInSec(60 * 5);
		
		// as we will get some classloading exceptions from Twitter4J (expected), we disable some loggers
		cfg.server().logging().setCustomLoggingLevel(
				"at.ac.tuwien.infosys.jcloudscale.classLoader.caching.RemoteClassLoader", Level.OFF);
		cfg.server().logging().setCustomLoggingLevel(
				"at.ac.tuwien.infosys.jcloudscale.classLoader.caching.fileCollectors.FileBasedFileCollector", Level.OFF);
		cfg.common().clientLogging().setCustomLoggingLevel(
				"at.ac.tuwien.infosys.jcloudscale.classLoader.caching.RemoteClassProvider", Level.OFF);
		cfg.common().clientLogging().setCustomLoggingLevel(
				"at.ac.tuwien.infosys.jcloudscale.classLoader.caching.fileCollectors.FileBasedFileCollector", Level.OFF);
		
		return cfg;
		
	}
	
}	
