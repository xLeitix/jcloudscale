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
package at.ac.tuwien.infosys.jcloudscale.test.util;
 
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfigurationBuilder;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.policy.sample.HostPerObjectScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.policy.sample.SingleHostScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.vm.ec2.EC2CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.localVm.LocalCloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.openstack.OpenstackCloudPlatformConfiguration;

public class ConfigurationHelper 
{
	private static final String SERVER_ADDRESS_CONFIG =  "mq.address";
	private static final String OPENSTACK_CONFIG_FILE = "openstack.props";
	
    private static JCloudScaleConfigurationBuilder applyCommonConfiguration(JCloudScaleConfigurationBuilder builder)
    {
        return builder
                .with(new HostPerObjectScalingPolicy())
                .withLogging(Level.INFO)
                .withMonitoring(true);
    }

    public static JCloudScaleConfigurationBuilder createDefaultTestConfiguration()
    {
        return applyCommonConfiguration(new JCloudScaleConfigurationBuilder(
                new LocalCloudPlatformConfiguration()
                .withStartupDirectory("target/")
                .withClasspath("*;lib/*;lib/"))
                );
    }
	
	public static JCloudScaleConfigurationBuilder createDefaultCloudTestConfiguration() throws FileNotFoundException, IOException
	{
        Properties openstackProperties = new Properties();
        try(InputStream propertiesStream = ClassLoader.getSystemResourceAsStream(OPENSTACK_CONFIG_FILE))
        {
            openstackProperties.load(propertiesStream);
        }

        String serverAddress = openstackProperties.getProperty(SERVER_ADDRESS_CONFIG);

        if(serverAddress == null || serverAddress.length() == 0)
            throw new RuntimeException("Could not read ActiveMQ server address from the property \""+
                    SERVER_ADDRESS_CONFIG+"\" from the config file "+OPENSTACK_CONFIG_FILE);

        return applyCommonConfiguration(new JCloudScaleConfigurationBuilder(
                new OpenstackCloudPlatformConfiguration(openstackProperties))
        .withMQServerHostname(serverAddress));
	}
	
	public static JCloudScaleConfiguration createDefaultAmazonTestConfiguration() throws FileNotFoundException, IOException, URISyntaxException
	{
		JCloudScaleConfiguration cfg = createDefaultTestConfiguration().build();
		EC2CloudPlatformConfiguration config = new EC2CloudPlatformConfiguration();
		config.setAwsConfigFile(ClassLoader.getSystemResource("aws.props").getPath());
		config.setAwsEndpoint("ec2.eu-west-1.amazonaws.com");
		config.setInstanceType("t1.micro");
		config.setSshKey("aws");
		MessageQueueConfiguration mqConfig = new MessageQueueConfiguration();
		mqConfig.setServerAddress("ec2-54-216-246-24.eu-west-1.compute.amazonaws.com");
		cfg.common().setCommunicationConfiguration(mqConfig);
		cfg.server().setCloudPlatform(config);
		cfg.common().setScalingPolicy(new SingleHostScalingPolicy());
		return cfg;
		
	}
}
