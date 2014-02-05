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
package at.ac.tuwien.infosys.jcloudscale.vm.bursting;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHostPool;
import at.ac.tuwien.infosys.jcloudscale.vm.IdManager;

public class AggregatedCloudPlatformConfiguration extends
		CloudPlatformConfiguration {

	private static final long serialVersionUID = 1L;
	private CloudPlatformConfiguration[] cloudPlatforms;

	
	/**
	 * Creates new instance of aggregated cloud platform configuration for the single message queue scenario.
	 * @param cloudPlatforms The set of cloud platforms that will communicate through the single message queue.
	 */
	public AggregatedCloudPlatformConfiguration(CloudPlatformConfiguration... cloudPlatforms)
	{
		this(null, cloudPlatforms);
	}
	
	/**
	 * Creates the new instance of the aggregated cloud platform configuration for the multiple message queue scenario.
	 * Assumes that each cloud platform has its own message queue configuration 
	 * and they are stored within aggregated message queue configuration in the same order as cloud platforms are provided.
	 * @param messageQueueConfiguration An instance of the aggregated message queue configuration that contains 
	 * a list of message queue configurations for each cloud platform.
	 * 
	 * @param cloudPlatforms The list of cloud platforms that should be aggregated within this aggregated message queue configuration.
	 */
	public AggregatedCloudPlatformConfiguration(AggregatedMessageQueueConfiguration messageQueueConfiguration, CloudPlatformConfiguration... cloudPlatforms)
	{
		this.cloudPlatforms = cloudPlatforms;
		
		//
		// Setting appropriate MessageQueueConfiguration for each cloud platform
		//
		if(messageQueueConfiguration == null)
			return;//everyone will use default one
		
		if(this.cloudPlatforms.length != messageQueueConfiguration.getMqConfigurations().length)
			throw new JCloudScaleException("The amount of Message Queue Configurations is not equal to amount of cloud platforms.");
		
		for(int i=0;i<cloudPlatforms.length;++i)
			this.cloudPlatforms[i].setMessageQueueConfiguration(messageQueueConfiguration.getMqConfigurations()[i]);
	}
	
	public CloudPlatformConfiguration[] getCloudPlatforms() {
		return cloudPlatforms;
	}

	@Override
	public IVirtualHost getVirtualHost(IdManager idManager) 
	{
		throw new RuntimeException("Not Implementable");
	}

	@Override
	public IVirtualHostPool getVirtualHostPool() {
		return new AggregatedVirtualHostPool(this);
	}

	@Override
	public AutoCloseable ensureCommunicationServerRunning() throws Exception 
	{
		final List<AutoCloseable> messageQueues = new ArrayList<>();
		for(CloudPlatformConfiguration cfg : cloudPlatforms)
		{
			try
			{
				messageQueues.add(cfg.ensureCommunicationServerRunning());
			}
			catch(Exception ex)
			{
				
			}
		}
		
		return new AutoCloseable()
		{
			@Override
			public void close() throws Exception {
				for(AutoCloseable mq : messageQueues)
					if(mq != null)
						mq.close();
			}
		};
	}

	@Override
	protected AutoCloseable startMessageQueueServer(MessageQueueConfiguration communicationConfiguration)throws Exception {
		return null;
	}
}

//
// + !client has to listen for updates on all servers in all queues! 
//LogReceiver, SysoutputReceiver, classProvider, configurationProvider, 
//IdManager, JCloudScaleReferenceManager, EventCorrelationEngine, MonitoringMQHelper
//...
//
