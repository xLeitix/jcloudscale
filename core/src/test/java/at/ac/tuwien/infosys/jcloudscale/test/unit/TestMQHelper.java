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
package at.ac.tuwien.infosys.jcloudscale.test.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.UUID;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.naming.NamingException;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.ActiveMQHelper;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.MQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.TimeoutException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ListServersRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ListServersResponse;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.dto.Server;
import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;

public class TestMQHelper {
	
	private MQWrapper mq;
	private ActiveMQHelper helper;
	
	@Before
	public void setup() throws Exception 
	{
		JCloudScaleConfiguration cfg = ConfigurationHelper.createDefaultTestConfiguration().build();
		
		cfg.common().communication().setRequestTimeout(10000);
		cfg.common().communication().setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		
		JCloudScaleClient.setConfiguration(cfg);
		
		helper = new ActiveMQHelper(); 
		helper.start();
		
		mq = (MQWrapper)JCloudScaleConfiguration.createMQWrapper(cfg);
	}

	@After
	public void tearDown() throws Exception 
	{
		mq.close();
		helper.close();
	}
		
	@Test
	public void testCreateProducer() throws NamingException, JMSException {
		mq.createQueueProducer("tmp");
	}
	
	@Test
	public void testCreateConsumer() throws NamingException, JMSException {
		mq.createQueueConsumer("tmp");
	}
	
	@Test
	public void testCorrectDestinationTypes() throws NamingException, JMSException 
	{
		mq.createQueueConsumer("tmp");
		assertEquals(ActiveMQQueue.class, getConsumerDestinationType());
		mq.createTopicConsumer("tmp");
		assertEquals(ActiveMQTopic.class, getConsumerDestinationType());
	}
	
	@Test
	public void testSimpleRequestResponseInvocation() throws NamingException, JMSException, TimeoutException 
	{ 
		try
		{
			mq.createQueueProducer("tmpOut");
			mq.createQueueConsumer("tmpIn");
			
			MessageListener listener = new MessageListener() {
				
				IMQWrapper responder = JCloudScaleConfiguration.createMQWrapper();
				
				@Override
				public void onMessage(Message msg) {
					try 
					{
						Destination dest;
						dest = msg.getJMSReplyTo();
						responder.respond(new ListServersResponse(), dest);
					} catch (JMSException e) {
						e.printStackTrace();
					} 
					finally
					{
						responder.close();
					}
				}
			};
			IMQWrapper serverSim = JCloudScaleConfiguration.createMQWrapper();
			try
			{
				serverSim.createQueueConsumer("tmpOut");
				serverSim.registerListener(listener);
				
				MessageObject resp = mq.requestResponse(new ListServersRequest());
				
				assertNotNull(resp);
			}
			finally
			{
				serverSim.disconnect();
			}
		}
		finally
		{
			mq.disconnect();
		}
	}
	
	@Test
	public void testTopic() throws NamingException, JMSException, TimeoutException { 
		
		try {
			mq.createTopicProducer("tmpIn");
			mq.createTopicConsumer("tmpOut");
			
			MessageListener listener = new MessageListener() {
				
				IMQWrapper responder = JCloudScaleConfiguration.createMQWrapper();
				
				@Override
				public void onMessage(Message msg) {
					try {
						Destination dest;
						dest = msg.getJMSReplyTo();
						responder.respond(new ListServersResponse(), dest);
					} catch (JMSException e) 
					{
						e.printStackTrace();
					}
					finally
					{
						responder.close();
					}
				}
			};
			IMQWrapper serverSim = JCloudScaleConfiguration.createMQWrapper();
			try
			{
				serverSim.createTopicConsumer("tmpIn");
				serverSim.registerListener(listener);
				
				MessageObject resp = mq.requestResponse(new ListServersRequest());
				
				assertNotNull(resp);
			}
			finally
			{
				serverSim.disconnect();
			}
		}
		finally
		{
			mq.disconnect();
		}
	}
	
	@Test
	public void testTopicWithMessageSelector() throws NamingException, JMSException, TimeoutException { 
		
		final UUID corrId = UUID.randomUUID();
		
		try{
		mq.createTopicProducer("tmpIn");
		mq.createTopicConsumer("tmpOut", "JMSCorrelationID = '"+corrId.toString()+"'");
		
		MessageListener listener = new MessageListener() {
			
			IMQWrapper responder = JCloudScaleConfiguration.createMQWrapper();
			
			@Override
			public void onMessage(Message msg) {
				try {
					Destination dest;
					dest = msg.getJMSReplyTo();
					ListServersResponse dummyServer = new ListServersResponse();
					dummyServer.setServers(new LinkedList<Server>());
					dummyServer.getServers().add(new Server());
					
					// the first response should not be received because of the selector
					responder.respond(new ListServersResponse(), dest, UUID.randomUUID());
					// but the second should
					responder.respond(dummyServer, dest, corrId);
					
				} catch (JMSException e) {
					e.printStackTrace();
				}
				finally
				{
					responder.close();
				}
			}
		};
		IMQWrapper serverSim = JCloudScaleConfiguration.createMQWrapper();
		try
		{
			serverSim.createTopicConsumer("tmpIn");
			serverSim.registerListener(listener);
			
			MessageObject resp = mq.requestResponse(new ListServersRequest());
			
			assertNotNull(resp);
			assertTrue(resp instanceof ListServersResponse);
			assertEquals(1, ((ListServersResponse)resp).getServers().size());
		}
		finally
		{
			serverSim.disconnect();
		}
		}
		finally
		{
			mq.disconnect();
		}
	}
	
	@Test
	public void testTopicWithHostSelector() throws NamingException, JMSException, TimeoutException { 
		
		final UUID hostId = UUID.randomUUID();
		
		try
		{
			mq.createTopicProducer("tmpIn");
			mq.createTopicConsumer("tmpOut", "CS_HostId = '"+hostId.toString()+"'");
			
			MessageListener listener = new MessageListener() {
				
				IMQWrapper responder = JCloudScaleConfiguration.createMQWrapper();
				
				@Override
				public void onMessage(Message msg) {
					try 
					{
						Destination dest;
						dest = msg.getJMSReplyTo();
						ListServersResponse dummyServer = new ListServersResponse();
						dummyServer.setServers(new LinkedList<Server>());
						dummyServer.getServers().add(new Server());
						
						// the first response should not be received because of the selector
						responder.respond(new ListServersResponse(), dest, UUID.randomUUID());
						// but the second should
						responder.createTopicProducer("tmpOut");
						responder.onewayToCSHost(dummyServer, null, hostId);
						
					} catch (JMSException e) {
						e.printStackTrace();
					} catch (NamingException e) {
						e.printStackTrace();
					}
					finally
					{
						responder.close();
					}
				}
			};
			IMQWrapper serverSim = JCloudScaleConfiguration.createMQWrapper();
			try
			{
				serverSim.createTopicConsumer("tmpIn");
				serverSim.registerListener(listener);
				
				MessageObject resp = mq.requestResponse(new ListServersRequest());
				
				assertNotNull(resp);
				assertTrue(resp instanceof ListServersResponse);
				assertEquals(1, ((ListServersResponse)resp).getServers().size());
			}
			finally
			{
				serverSim.disconnect();
			}
		}
		finally
		{
			mq.disconnect();
		}
	}
	
	@Test(expected=TimeoutException.class)
	public void testTimeout() throws NamingException, JMSException, TimeoutException 
	{ 
		try
		{
			mq.createQueueProducer("tmpOut");
			mq.createQueueConsumer("tmpIn");
			
			MessageListener listener = new MessageListener() {
				
				@Override
				public void onMessage(Message msg) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			IMQWrapper serverSim = JCloudScaleConfiguration.createMQWrapper();
			try
			{
				serverSim.createQueueConsumer("tmpOut");
				serverSim.registerListener(listener);
				
				mq.requestResponse(new ListServersRequest());
			}
			finally
			{
				serverSim.disconnect();
			}
		}
		finally
		{
			mq.disconnect();
		}
	}
	
	private Class<? extends Destination> getConsumerDestinationType() {
		
		try {
			
			Field field = mq.getClass().getDeclaredField("consumerDestination");
			field.setAccessible(true);
			return (Class<? extends Destination>) field.get(mq).getClass();
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Reflection Error in Test");
		}
	}
	
}