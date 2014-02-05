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

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;

public class AggregatedMessageQueueConfiguration extends MessageQueueConfiguration {

	private static final long serialVersionUID = 1L;

	private MessageQueueConfiguration[] mqConfigurations;
	private MessageQueueConfiguration appropriateServerMqConfig;
	
	public AggregatedMessageQueueConfiguration(MessageQueueConfiguration... configurations)
	{
		this.mqConfigurations = configurations;
	}
	
	
	public MessageQueueConfiguration[] getMqConfigurations() {
		return mqConfigurations;
	}


	@Override
	public IMQWrapper newWrapper() throws NamingException, JMSException 
	{
		if(JCloudScaleConfiguration.isServerContext())
		{
			return getAppropriateServerMQConfig().newWrapper();
		}
		else
			return new AggregatedMQWrapper(this);
	}
	
	private synchronized MessageQueueConfiguration getAppropriateServerMQConfig()
	{
		if(appropriateServerMqConfig != null)
			return appropriateServerMqConfig;
		
		Logger log = JCloudScaleConfiguration.getLogger(this);
		
		try
		{
			// trying to discover message queue.
			int attempts = 3;
			while(attempts-- > 0)
			{
				log.info("Aggregated MQConfiguration is Trying to discover message queue again to detect which sub-configuration to use...");
				
				if(super.tryDiscoverMQServer())
				{
					attempts = 1;//just to avoid case when we discovered on last attempt.
					break;
				}
			}
			
			if(attempts > 0)
				log.info("Appropriate Message Queue Discovered Successfully: "+super.serverAddress +":"+super.serverPort);
			else
			{
				log.severe("Failed to discover Message Queue within allowed set of attempts!");
				throw new JCloudScaleException("Failed to discover Message Queue within allowed set of attempts!");
			}
			
			for(MessageQueueConfiguration cfg : this.mqConfigurations)
				if(cfg.getServerAddress().equals(super.serverAddress) && cfg.getServerPort() == super.serverPort)
				{
					log.info("Appropriate Sub-Configuration Detected.");
					appropriateServerMqConfig = cfg;
					return cfg;
				}
		}
		catch(Exception ex)
		{
			log.severe("Exception occured while trying to discover mq server: "+ex);
		}
		
		return null;
	}
	
	@Override
	public String getServerAddress() {
		throw new RuntimeException("Not allowed operation for aggregated Message Queue Configuration");
	}
	
	@Override
	public void setServerAddress(String serverAddress) {
		throw new RuntimeException("Not allowed operation for aggregated Message Queue Configuration");
	}

	@Override
	public int getServerPort() {
		throw new RuntimeException("Not allowed operation for aggregated Message Queue Configuration");
	}
	
	@Override
	public void setServerPort(int serverPort) {
		throw new RuntimeException("Not allowed operation for aggregated Message Queue Configuration");
	}

	@Override
	public boolean startMulticastPublisher() {
		return false;
	}

	@Override
	public Closeable createServerPublisher() throws IOException {
		throw new RuntimeException("Not allowed operation for aggregated Message Queue Configuration");
	}
	
}
