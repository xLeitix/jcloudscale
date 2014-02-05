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
package at.ac.tuwien.infosys.jcloudscale.sample.service.openstack;

import java.io.Closeable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.naming.Context;

import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.commands.DiscoveryRequest;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.commands.DiscoveryResponse;

/**
 * Wrapper class that wraps other application-independent functions like host discovery and communication.
 */
public class OpenstackWrapper implements Closeable
{
	private Logger log = Logger.getLogger(OpenstackWrapper.class.getName());
	private Context context;
	private Connection connection;
	private Session session;
	private MessageProducer producer;
	private MessageConsumer consumer;
	private List<String> workers;
	
	private OpenstackWrapper()
	{
		log.info("Initializing Openstack Wrapper...");
		
		String mqAddress = OpenstackConfiguration.getMessageQueueAddress();
		context = MessageQueueHelper.createContext(mqAddress);
		connection = MessageQueueHelper.establishConnection(context, mqAddress);
		try
		{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			
			Destination destination = (Destination)context.lookup("dynamicTopics/"+OpenstackConfiguration.getWorkerTopic());
			producer = session.createProducer(destination);
			
			destination = (Destination)context.lookup("dynamicQueues/"+OpenstackConfiguration.getClientQueue());
			consumer = session.createConsumer(destination);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException("Failed to initialize message queue", ex);
		}
		
		log.info("Discovering workers...");
		workers = discoverWorkers(OpenstackConfiguration.getDisocverInterval());
		
		if(workers == null)
			log.severe("Failed to discover workers! NULL received.");
		else
			log.info("  ..... Discovered "+workers.size()+" workers ..... ");
	}
	
	public static OpenstackWrapper instance; 
	
	public static synchronized OpenstackWrapper getInstance()
	{
		if(instance == null)
			instance = new OpenstackWrapper();
		
		return instance;
	}
	
	public static synchronized void closeInstance()
	{
		if(instance != null)
		{
			instance.close();
			instance = null;
		}
	}
	
	private List<String> discoverWorkers(int disocverInterval) 
	{
		try
		{
			MessageListener oldListener = consumer.getMessageListener();
			try
			{
				final List<String> discoveredWorkers = new ArrayList<String>();
				//
				// setting listener to receive discovery responses
				//
				consumer.setMessageListener(new MessageListener() 
				{
					@Override
					public void onMessage(Message message) 
					{
						try
						{
							if(!(message instanceof ObjectMessage) || !(((ObjectMessage)message).getObject() instanceof DiscoveryResponse))
								log.warning("Unexpected message during discovery:"+message);
							
							discoveredWorkers.add(((DiscoveryResponse)((ObjectMessage)message).getObject()).getHostId());
						}
						catch(Exception ex)
						{
							log.severe("Exception while discovery: "+ex);
						}
					}
				});
				
				//
				// creating sender.
				//
				Destination destination = (Destination)context.lookup("dynamicTopics/"+OpenstackConfiguration.getWorkerDiscoveryTopic());
				MessageProducer producer = session.createProducer(destination);
				MessageQueueHelper.sendMessage(session, producer, new DiscoveryRequest());
				
				Thread.sleep(disocverInterval);
				
				return discoveredWorkers;
			}
			finally
			{
				consumer.setMessageListener(oldListener);
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}

	@Override
	public void close() 
	{
		if(this.connection != null)
		{
			try {
				this.connection.close();
			} catch (JMSException e) 
			{
				e.printStackTrace();
			}
			this.connection = null;
		}
	}

	public List<String> getWorkers()
	{
		return this.workers;
	}

	
	public void sendMessageToWorker(Serializable message, String hostId)
	{
		MessageQueueHelper.sendMessage(session, producer, message, hostId);
	}
	
	public void subscribeMessageListener(MessageListener listener)
	{
		try 
		{
			this.consumer.setMessageListener(listener);
		} 
		catch (JMSException e) 
		{
			e.printStackTrace();
		}
	}
}
