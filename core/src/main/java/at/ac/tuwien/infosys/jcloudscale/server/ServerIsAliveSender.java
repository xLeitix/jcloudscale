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
package at.ac.tuwien.infosys.jcloudscale.server;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.IConfigurationChangedListener;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.IsAliveObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.IsDeadObject;

public class ServerIsAliveSender implements IConfigurationChangedListener 
{
	private UUID id;
	private String ip;
	private long period;
	private IMQWrapper mq; 
	private Timer timer;
	private Logger log;
	
	public ServerIsAliveSender(UUID id, String ip) throws NamingException, JMSException 
	{
		this.id = id;
		this.ip = ip;
		this.period = JCloudScaleConfiguration.getConfiguration().server().getIsAliveInterval();
		this.log = JCloudScaleConfiguration.getLogger(this);
		
		// XXX AbstractJCloudScaleServerRunner
		// JCloudScaleServerRunner.getInstance().registerConfigurationChangeListner(this);
		AbstractJCloudScaleServerRunner.getInstance().registerConfigurationChangeListner(this);
		
		createMQWrapper(JCloudScaleConfiguration.getConfiguration());
		startTimer();
	}
	
	private void createMQWrapper(JCloudScaleConfiguration cfg) throws NamingException, JMSException 
	{
		IMQWrapper wrapper = JCloudScaleConfiguration.createMQWrapper(cfg, DeliveryMode.NON_PERSISTENT);
		wrapper.createQueueProducer(cfg.server().getInitializationQueueName());
		wrapper.createTopicConsumer(cfg.server().getStaticHostsQueueName());
		wrapper.registerListener(new StaticHostRequestListener());
		
		IMQWrapper oldWrapper = mq;
		
		this.mq = wrapper;

		if(oldWrapper != null)
			oldWrapper.close();
	}

	private void startTimer() 
	{
		if(timer != null)
			timer.cancel();
		
		timer = new Timer();
		timer.schedule(new MessageSender(), 0, period);
	}

	public void stopSendingIsAliveMessages() throws JMSException, NamingException {
		
		IsDeadObject obj = new IsDeadObject();
		obj.setId(id.toString());
		
		try {
			mq.oneway(obj);
		} catch (JMSException e) {
			log.severe(e.getMessage());
			e.printStackTrace();
		} finally {
			timer.cancel();
			mq.disconnect();
		}
		
	}
	
	@Override
	public void onConfigurationChange(JCloudScaleConfiguration newConfiguration) 
	{
		this.log = JCloudScaleConfiguration.getLogger(newConfiguration, this);
		
		if(!this.mq.configurationEquals(newConfiguration.common().communication()) ||
				!newConfiguration.server().getInitializationQueueName().equals(
					JCloudScaleConfiguration.getConfiguration().server().getInitializationQueueName()))
		{
			try 
			{
				createMQWrapper(newConfiguration);
			} catch (NamingException | JMSException e) 
			{
				e.printStackTrace();
			}
		}
		
		if(this.period != newConfiguration.server().getIsAliveInterval())
			startTimer();
	}
	
	private void sendIsAliveMessage() {
		
		IsAliveObject obj = new IsAliveObject();
		obj.setId(id.toString());
		obj.setIp(ip);
		try {
			log.fine(id +": Sending IsAliveMessage.");
			mq.oneway(obj);
		} catch (JMSException e) {
			log.severe(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private class MessageSender extends TimerTask {

		@Override
		public void run() {
			sendIsAliveMessage();
		}
		
	}
	
	private class StaticHostRequestListener implements MessageListener {

		@Override
		public void onMessage(Message message) {
			log.info("Was asked to send isalive message. Sending.");
			sendIsAliveMessage();
		}

		
	}
}
