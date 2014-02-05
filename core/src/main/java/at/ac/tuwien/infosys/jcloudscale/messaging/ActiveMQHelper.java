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
package at.ac.tuwien.infosys.jcloudscale.messaging;

import java.io.IOException;
import java.net.BindException;

import org.apache.activemq.broker.BrokerService;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;

public class ActiveMQHelper implements AutoCloseable {
	
	private BrokerService broker;
	
	public ActiveMQHelper() throws Exception 
	{
		this(JCloudScaleConfiguration.getConfiguration().common().communication());
	}
	
	public ActiveMQHelper(MessageQueueConfiguration communicationConfiguration) throws Exception
	{
		broker = new BrokerService();
		broker.setPersistent(false);
		
		addConfiguration(communicationConfiguration);
	}
	
	public void addConfiguration(MessageQueueConfiguration config) throws Exception 
	{
		broker.addConnector("tcp://"+config.serverAddress+":"+config.serverPort);
	}

	public void start() throws Exception 
	{
		try
		{
			if(broker.isStarted())
				return;
			
			broker.start();
		}
		catch(IOException ex)
		{
			//this might be completely fine. Possibliy MQ is running already. Consider better solution?
			if(ex.getCause() == null || !(ex.getCause() instanceof BindException))
				throw ex;
		}
	}
	
	@Override
	public void close() throws Exception	
	{
		if(broker != null) {
			broker.stop();
			broker = null;
		}
	}
	
	public boolean isRunning() {
		return (broker != null && broker.isStarted());
	}
	
}
