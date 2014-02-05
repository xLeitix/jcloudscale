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
package at.ac.tuwien.infosys.jcloudscale.test.unit;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.ActiveMQHelper;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.IsAliveObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;
import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import at.ac.tuwien.infosys.jcloudscale.vm.bursting.AggregatedMessageQueueConfiguration;

public class TestAggregatedMQWrapper 
{
	private static final int PORT1 = 60000;
	private static final int PORT2 = 60001;
	
	private static final String FROM_AGGREGATED_TOPIC = "from_aggregated_topic";
	private static final String TO_AGGREGATED_QUEUE = "to_aggregated_queue";
	private static final String FROM_AGGREGATED_QUEUE = "from_aggregated_queue";
	
	private ActiveMQHelper server;
	private IMQWrapper aggregatedWrapper, wrapper1, wrapper2;
	
	
	@Before
	public void setup() throws Exception 
	{
		//
		// setting up configuration
		//
		JCloudScaleConfiguration cfg = ConfigurationHelper.createDefaultTestConfiguration().build();
		JCloudScaleClient.setConfiguration(cfg);
		
		MessageQueueConfiguration mq1Config = cfg.common().communication();
		mq1Config.setRequestTimeout(1000);
		mq1Config.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		mq1Config.setServerPort(PORT1);
		
		MessageQueueConfiguration mq2Config = new MessageQueueConfiguration();
		mq2Config.setRequestTimeout(1000);
		mq2Config.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		mq2Config.setServerPort(PORT2);
		cfg.common().setCommunicationConfiguration(mq2Config);
		
		MessageQueueConfiguration mqAggregated = new AggregatedMessageQueueConfiguration(mq1Config, mq2Config);
		cfg.common().setCommunicationConfiguration(mqAggregated);
		
		//
		// starting server
		//
		server = new ActiveMQHelper(mq1Config);
		server.addConfiguration(mq2Config);
		server.start();
		
		//
		// Creating wrappers.
		//
		wrapper1 = mq1Config.newWrapper();
		wrapper2 = mq2Config.newWrapper();
		aggregatedWrapper = mqAggregated.newWrapper();
	}

	@After
	public void tearDown() throws Exception 
	{
		wrapper1.close();
		wrapper2.close();
		aggregatedWrapper.close();
		
		JCloudScaleClient.closeClient();
		JCloudScaleClient.setConfiguration(null);
		server.close();
	}
	
	/**
	 * Sends messages to two queues and ensures they are delivered.
	 * Tests oneWay method on aggregated wrapper.
	 */
	@Test
	public void testPingQueue() throws Exception
	{
		//
		// Configuring
		//
		final CountDownLatch countDown = new CountDownLatch(2);
		MessageListener listener = new MessageListener() {
			@Override
			public void onMessage(Message message) 
			{
				countDown.countDown();
			}
		};
		wrapper1.createQueueConsumer(FROM_AGGREGATED_QUEUE);
		wrapper1.registerListener(listener);
		
		wrapper2.createQueueConsumer(FROM_AGGREGATED_QUEUE);
		wrapper2.registerListener(listener);
		
		//
		// Sending
		//
		aggregatedWrapper.createQueueProducer(FROM_AGGREGATED_QUEUE);
		aggregatedWrapper.oneway(null);
		
		if(!countDown.await(1, TimeUnit.SECONDS))
			throw new RuntimeException("Message Was not Delivered!");
	}
	
	/**
	 * Sends messages to two queues and waits for response.
	 * Tests: oneWay, registerlistener.
	 */
	@Test
	public void testPingPong() throws Exception
	{
		//
		// Configuring
		//
		final CountDownLatch countDown = new CountDownLatch(2);
		
		wrapper1.createQueueConsumer(FROM_AGGREGATED_QUEUE);
		wrapper1.createQueueProducer(TO_AGGREGATED_QUEUE);
		wrapper1.registerListener(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				try {
					wrapper1.oneway((MessageObject)((ObjectMessage)message).getObject());
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		});
		
		wrapper2.createQueueConsumer(FROM_AGGREGATED_QUEUE);
		wrapper2.createQueueProducer(TO_AGGREGATED_QUEUE);
		wrapper2.registerListener(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				try {
					wrapper2.oneway((MessageObject)((ObjectMessage)message).getObject());
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		});
		
		aggregatedWrapper.createQueueConsumer(TO_AGGREGATED_QUEUE);
		aggregatedWrapper.createQueueProducer(FROM_AGGREGATED_QUEUE);
		aggregatedWrapper.registerListener(new MessageListener() {
			@Override
			public void onMessage(Message message) 
			{
				countDown.countDown();
			}
		});
		
		//
		// Sending
		//
		aggregatedWrapper.oneway(new IsAliveObject());
		
		if(!countDown.await(1, TimeUnit.SECONDS))
			throw new RuntimeException("Message Was not Delivered!");
	}
	
	/**
	 * Sends 2 different messages to two topics and waits for response.
	 * Tests: requestResponse, registerlistener.
	 */
	@Test
	public void testPingPongTopic() throws Exception
	{
		//
		// Configuring
		//
		UUID wrapper1Id = UUID.randomUUID();
		wrapper1.createTopicConsumer(FROM_AGGREGATED_TOPIC, "JMSCorrelationID = '"+wrapper1Id+"'");
		wrapper1.createQueueProducer(TO_AGGREGATED_QUEUE);
		wrapper1.registerListener(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				try {
					wrapper1.oneway((MessageObject)((ObjectMessage)message).getObject());
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		});
		
		UUID wrapper2Id = UUID.randomUUID();
		wrapper2.createTopicConsumer(FROM_AGGREGATED_TOPIC, "JMSCorrelationID = '"+wrapper2Id+"'");
		wrapper2.createQueueProducer(TO_AGGREGATED_QUEUE);
		wrapper2.registerListener(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				try {
					wrapper2.oneway((MessageObject)((ObjectMessage)message).getObject());
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		});
		
		aggregatedWrapper.createQueueConsumer(TO_AGGREGATED_QUEUE);
		aggregatedWrapper.createTopicProducer(FROM_AGGREGATED_TOPIC);
		
		//
		// Sending
		//
		MessageObject obj = new IsAliveObject();
		if(!obj.getClass().getName().equals(
				aggregatedWrapper.requestResponse(obj, wrapper1Id).getClass().getName()))
			throw new RuntimeException("Failed to send or the answer is not valid!");
		
		if(!obj.getClass().getName().equals(
				aggregatedWrapper.requestResponse(obj, wrapper2Id).getClass().getName()))
			throw new RuntimeException("Failed to send or the answer is not valid!");
	}
	
	/**
	 * Simple wrappers send message and aggregated wrapper responses with correct id.
	 * Tests: requestResponse from server, registerlistener.
	 */
	@Test
	public void testPongPingTopic() throws Exception
	{
		//
		// Configuring
		//
		UUID wrapper1Id = UUID.randomUUID();
		wrapper1.createTopicConsumer(FROM_AGGREGATED_TOPIC, "JMSCorrelationID = '"+wrapper1Id+"'");
		wrapper1.createQueueProducer(TO_AGGREGATED_QUEUE);
		
		UUID wrapper2Id = UUID.randomUUID();
		wrapper2.createTopicConsumer(FROM_AGGREGATED_TOPIC, "JMSCorrelationID = '"+wrapper2Id+"'");
		wrapper2.createQueueProducer(TO_AGGREGATED_QUEUE);
		
		aggregatedWrapper.createQueueConsumer(TO_AGGREGATED_QUEUE);
		aggregatedWrapper.createTopicProducer(FROM_AGGREGATED_TOPIC);
		aggregatedWrapper.registerListener(new MessageListener() {
			@Override
			public void onMessage(Message message) 
			{
				try
				{
					IsAliveObject obj = (IsAliveObject)((ObjectMessage)message).getObject();
					aggregatedWrapper.respond(obj, message.getJMSReplyTo(), UUID.fromString(obj.getId()));
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
				}
			}
		});
		//
		// Sending
		//
		IsAliveObject obj = new IsAliveObject();
		obj.setId(wrapper1Id.toString());
		if(!obj.getClass().getName().equals(
				wrapper1.requestResponse(obj, null).getClass().getName()))
			throw new RuntimeException("Failed to send or the answer is not valid!");
		
		obj.setId(wrapper2Id.toString());
		if(!obj.getClass().getName().equals(
				wrapper2.requestResponse(obj, null).getClass().getName()))
			throw new RuntimeException("Failed to send or the answer is not valid!");
	}
}
