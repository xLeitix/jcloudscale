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
package at.ac.tuwien.infosys.jcloudscale.vm;

import java.io.Serializable;
import java.util.logging.Logger;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.JMSConnectionHolder;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.TimeoutException;

/**
 * The cloud platform configuration parent abstract class that declares methods necessary for platform management.
 */
public abstract class CloudPlatformConfiguration implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private transient MessageQueueConfiguration messageQueueConfiguration;
	
	
	/**
	 * Sets the message Queue configuration to use by instances of this cloud platform.
	 * if not defined, default message queue configuration is used.
	 * @param messageQueueConfiguration The configuration of the message queue that should be used by the 
	 * hosts in cloud platform.
	 */
	public void setMessageQueueConfiguration(MessageQueueConfiguration messageQueueConfiguration)
	{
		this.messageQueueConfiguration = messageQueueConfiguration;
	}
	
	/**
	 * Gets the instance of message queue configuration that should be used within this cloud platform.
	 * @return
	 */
	protected MessageQueueConfiguration getMessageQueueConfiguration()
	{
		return this.messageQueueConfiguration != null ? 
				messageQueueConfiguration : 
				JCloudScaleConfiguration.getConfiguration().common().communication();
	}
	
	/**
	 * Gets the new host within the declared platform.
	 * @param isStatic specifies if the host is static 
	 * (describes the instance that has longer lifecycle than application running time) 
	 * or dynamic (the host that should be shut down on application shutdown).
	 * @return The management object that allows to operate with the new host.
	 */
	public abstract IVirtualHost getVirtualHost(IdManager idManager);
	
	public abstract IVirtualHostPool getVirtualHostPool();
	
	/**
	 * Ensures that Message Queue is running on this cloud platform
	 * @return The <b>Closeable</b> object that allows to shut down the MQ server if the MQ server was started.
	 */
	public AutoCloseable ensureCommunicationServerRunning() throws Exception
	{
		//getting messageQueue configuration
		MessageQueueConfiguration communicationConfiguration = getMessageQueueConfiguration();
		
		Logger log = JCloudScaleConfiguration.getLogger(this);
		
		String hostname = communicationConfiguration.getServerAddress();
		int port = communicationConfiguration.getServerPort();
		log.fine("Connecting to message queue server on " + hostname + ":" + port);
		if(!JMSConnectionHolder.isMessageQueueServerAvailable(hostname, port))
		{//we have to start it.
			log.info("Starting message queue server on " + hostname + ":" + port);
			try
			{
				// starting mq server.
				AutoCloseable server = startMessageQueueServer(communicationConfiguration);
				
				//server is started. let's wait for it to actually start.
				log.fine("Message queue server started. Waiting to become available...");
				long timeout = JCloudScaleConfiguration.getConfiguration().server().getHostInitializationTimeout();
				long start = System.nanoTime()/1000000;
				while(!JMSConnectionHolder.isMessageQueueServerAvailable(hostname, port))
				{
					if(System.nanoTime()/1000000 - start > timeout)
					{
						server.close();//we have to try closing.
						throw new TimeoutException("Message Queue host did not start within timeout.");
					}
					
					Thread.sleep(1000);
				}
				
				log.fine("Message queue server is available. (Startup took "+(System.currentTimeMillis() - start)+" ms)");
				
				return server;
			}
			catch(Exception e)
			{
				log.severe("Failed to start message queue server on "+hostname+":"+port+" and could not connect to it : "+e);
				throw e;
			}
		}
		
		return null;//MQ is running, we do nothing.
	}

	/**
	 * Starts Message Queue Server with specified configuration and returns closeable object that allows to shut it down.
	 * @param communicationConfiguration The message queue configuration of the server that should be started.
	 * @return The <b>Closeable</b> instance that allows to shut down server if needed.
	 */
	protected abstract AutoCloseable startMessageQueueServer(MessageQueueConfiguration communicationConfiguration) throws Exception;
}
