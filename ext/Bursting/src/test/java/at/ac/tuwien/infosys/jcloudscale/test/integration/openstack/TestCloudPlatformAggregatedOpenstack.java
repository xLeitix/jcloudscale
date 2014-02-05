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
package at.ac.tuwien.infosys.jcloudscale.test.integration.openstack;

import org.junit.Before;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.test.integration.base.TestAggregatedCloudPlatform;
import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import at.ac.tuwien.infosys.jcloudscale.vm.bursting.AggregatedCloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.bursting.AggregatedMessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.bursting.AggregatedVirtualHostPool;

public class TestCloudPlatformAggregatedOpenstack extends TestAggregatedCloudPlatform
{
    @Before
    public void setup() throws Exception
    {
        // getting local configuration items
        JCloudScaleConfiguration cfg = ConfigurationHelper.createDefaultTestConfiguration().build();
        CloudPlatformConfiguration localCloudPlatform = cfg.server().cloudPlatform();
        MessageQueueConfiguration localMessageQueue = cfg.common().communication();

        // getting openstack configuration items
        cfg = ConfigurationHelper.createDefaultCloudTestConfiguration()
                .with(new AggregatedScalingPolicy()).build();

        CloudPlatformConfiguration openstackCloudPlatform = cfg.server().cloudPlatform();
        MessageQueueConfiguration openstackMessageQueue = cfg.common().communication();

        // changing configuration
        AggregatedMessageQueueConfiguration mqConfig = new AggregatedMessageQueueConfiguration(localMessageQueue, openstackMessageQueue);
        cfg.server().setCloudPlatform(new AggregatedCloudPlatformConfiguration(mqConfig, localCloudPlatform, openstackCloudPlatform));
        cfg.common().setCommunicationConfiguration(mqConfig);

        JCloudScaleClient.setConfiguration(cfg);

        JCloudScaleClient.getClient();
        cs = CloudManager.getInstance();
        hostPool =  (AggregatedVirtualHostPool)cs.getHostPool();
    }
}
