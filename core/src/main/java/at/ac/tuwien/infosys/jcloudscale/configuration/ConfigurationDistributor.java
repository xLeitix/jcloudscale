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
package at.ac.tuwien.infosys.jcloudscale.configuration;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.TimeoutException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;
import at.ac.tuwien.infosys.jcloudscale.server.AbstractJCloudScaleServerRunner;

public class ConfigurationDistributor
{
	//TODO: should we create some helper method to not copy this string everywhere?
	private static String createSelector(UUID id) 
	{
		return "JMSCorrelationID = '"+id+"'";
	}
	
	public static void sendConfigurationToStaticHost(JCloudScaleConfiguration cfg, UUID serverId) throws NamingException, JMSException, TimeoutException, IOException 
	{
		try(IMQWrapper mq = JCloudScaleConfiguration.createMQWrapper())
		{
			mq.createTopicConsumer(JCloudScaleConfiguration.configurationDeliveryTopic, 
										createSelector(cfg.common().clientID()));
			mq.createTopicProducer(JCloudScaleConfiguration.configurationDeliveryTopic);
			
			// sending configuration and awaiting reply.
			ConfigurationDeliveryResponseObject response = 
					(ConfigurationDeliveryResponseObject)mq
					.requestResponse(new ConfigurationDeliveryObject(cfg), serverId);
			
			if(!response.isConfigurationAccepted)
				throw new JCloudScaleException("Configuration was not accepted by server "+serverId+": "+response.errorMessage);
		}
	}
	
	private static JCloudScaleConfiguration deserializeConfiguration(ConfigurationDeliveryObject response) throws Exception 
	{
		if(JCloudScaleConfiguration.isVersionCompatible(response.configurationVersion))
		{	//version is compatible, let's try to deserialize this configuration.
			try
			{
				return response.getConfiguration();
			}
			catch(Exception ex)
			{
				throw new Exception("Exception occured trying to deserialize configuration: "+ex, ex);
			}
		}
		else 
			throw new Exception("Configuration version is incompatible. Received \""+response.configurationVersion+"\", Expected \""+JCloudScaleConfiguration.CS_VERSION+"\"");
	}
	
	//------------------------------------------------------------------------------
	
	static class ServerConfigurationListener implements Closeable, MessageListener 
	{
		private IMQWrapper mq;
		ServerConfigurationListener(UUID id) throws NamingException, JMSException
		{
			mq = JCloudScaleConfiguration.createMQWrapper();
			mq.createTopicConsumer(JCloudScaleConfiguration.configurationDeliveryTopic, createSelector(id));
			mq.createTopicProducer(JCloudScaleConfiguration.configurationDeliveryTopic);
			mq.registerListener(this);
		}
		
		@Override
		public void onMessage(Message msg) 
		{
			if(msg == null || !(msg instanceof ObjectMessage))
				return;
			
			try 
			{
				ConfigurationDeliveryObject configuationDelvery = (ConfigurationDeliveryObject)((ObjectMessage)msg).getObject();
				
				JCloudScaleConfiguration cfg;
				try 
				{
					cfg = deserializeConfiguration(configuationDelvery);
					
					//all working services that are interested in configuration replacement will react on that.
					// XXX AbstractJCloudScaleServerRunner
					// JCloudScaleServerRunner.getInstance().setConfiguration(cfg);
					if(AbstractJCloudScaleServerRunner.getInstance() != null)
						AbstractJCloudScaleServerRunner.getInstance().setConfiguration(cfg);
					
					mq.oneway(new ConfigurationDeliveryResponseObject(true, ""), configuationDelvery.clientId);
					
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
					
					mq.oneway(new ConfigurationDeliveryResponseObject(false, e.getMessage()), configuationDelvery.clientId);
				}
			} 
			catch (JMSException e) 
			{
				e.printStackTrace();//hardly can do anything else here... send message to client if we have configuration already?
			}
		}

		@Override
		public void close() throws IOException 
		{
			if(mq != null)
			{
				mq.close();
				mq = null;
			}
		}
	}
	
	//--------------------------DTO classes-----------------------------------------
	
	private static class ConfigurationDeliveryObject extends MessageObject
	{
		private static final long serialVersionUID = -3761557206063869693L;
		
		private String configurationVersion;
		private UUID clientId;
		private byte[] serializedConfiguration;
		
		public ConfigurationDeliveryObject(JCloudScaleConfiguration cfg) throws IOException
		{
			this.configurationVersion = cfg.getVersion();
			this.clientId = cfg.common().clientID();
			//we have to clone configuration here to avoid modification to working configuration 
			//(we are modifying configuration during serialization).
			this.serializedConfiguration = cfg.clone().serialize();
		}
		
		public JCloudScaleConfiguration getConfiguration() throws ClassNotFoundException, IOException
		{
			return JCloudScaleConfiguration.deserialize(serializedConfiguration);
		}
	}
	
	private static class ConfigurationDeliveryResponseObject extends MessageObject
	{
		private static final long serialVersionUID = -1470029851256020709L;
		boolean isConfigurationAccepted;
		String errorMessage;
		
		public ConfigurationDeliveryResponseObject(boolean isConfigurationAccepted, String errorMessage)
		{
			this.isConfigurationAccepted = isConfigurationAccepted;
			this.errorMessage = errorMessage;
		}
	}
}
