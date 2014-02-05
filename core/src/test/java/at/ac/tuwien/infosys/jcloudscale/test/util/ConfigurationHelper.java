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
import at.ac.tuwien.infosys.jcloudscale.policy.sample.HostPerObjectScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.policy.sample.SingleHostScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.test.testevents.EventDrivenPolicyTestEvent;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.MyCustomEvent;
import at.ac.tuwien.infosys.jcloudscale.vm.ec2.EC2CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.localVm.LocalCloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.openstack.OpenstackCloudPlatformConfiguration;

public class ConfigurationHelper
{
    private static final String SERVER_ADDRESS_CONFIG =  "mq.address";
    private static final String OPENSTACK_CONFIG_FILE = "openstack.props";
    private static final String EC2_CONFIG_FILE = "aws.props";

    private static JCloudScaleConfigurationBuilder applyCommonConfiguration(JCloudScaleConfigurationBuilder builder)
    {
        builder .with(new HostPerObjectScalingPolicy())
                .withLogging(Level.INFO)
                .withMonitoring(true)
                .withInvocationTimeout(2*60*1000)//2 minutes
                .withMonitoringEvents(MyCustomEvent.class, EventDrivenPolicyTestEvent.class);
        
        builder.build().common().setKeepAliveIntervalInSec(15);
        
        return builder;
    }

    public static JCloudScaleConfigurationBuilder createDefaultTestConfiguration()
    {
    	
    	LocalCloudPlatformConfiguration cp = new LocalCloudPlatformConfiguration()
    	.withStartupDirectory("target/")
    	.withClasspath("*;lib/*;lib/");
    	cp.addCustomJVMArgs("-Djava.library.path=classes");
	
    	return applyCommonConfiguration(new JCloudScaleConfigurationBuilder(cp));


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
        
        
        OpenstackCloudPlatformConfiguration cloud =  new OpenstackCloudPlatformConfiguration(openstackProperties);
        return applyCommonConfiguration(new JCloudScaleConfigurationBuilder(cloud)
        	.withMQServerHostname(serverAddress));

    }

    public static JCloudScaleConfiguration createDefaultAmazonTestConfiguration() throws FileNotFoundException, IOException, URISyntaxException
    {
        Properties ec2Properties = new Properties();
        try(InputStream propertiesStream = ClassLoader.getSystemResourceAsStream(EC2_CONFIG_FILE))
        {
            ec2Properties.load(propertiesStream);
        }

        String serverAddress = ec2Properties.getProperty(SERVER_ADDRESS_CONFIG);

        if(serverAddress == null || serverAddress.length() == 0)
            throw new RuntimeException("Could not read ActiveMQ server address from the property \""+
                    SERVER_ADDRESS_CONFIG+"\" from the config file "+EC2_CONFIG_FILE);

        return applyCommonConfiguration(
                new JCloudScaleConfigurationBuilder(
                        new EC2CloudPlatformConfiguration()
                        .withAwsConfigFile(ClassLoader.getSystemResource(EC2_CONFIG_FILE).getPath())
                        .withAwsEndpoint("ec2.eu-west-1.amazonaws.com")
                        .withInstanceType("t1.micro")
                        .withSshKey("aws_pl")
                        )
                .withMQServerHostname(serverAddress))
                .with(new SingleHostScalingPolicy())
                .build();
    }
}
