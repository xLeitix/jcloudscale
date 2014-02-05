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

import java.io.Serializable;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.activemq.transport.InactivityIOException;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;

/**
 * Helper that wraps necessary method calls that come up if we want to do a similar task to the jcloudscale code
 */
public class MessageQueueHelper 
{
	private static final Logger log = Logger.getLogger(MessageQueueHelper.class.getName());
	
	public static Context createContext(String address)
	{
		Properties props = new Properties();
		props.setProperty(Context.INITIAL_CONTEXT_FACTORY,"org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		String providerUrl = "tcp://"+address;
			
		props.setProperty(Context.PROVIDER_URL,providerUrl);
		try 
		{
			return new InitialContext(props);
		} catch (NamingException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static Connection establishConnection(Context context, String address) 
	{
		final int sleepTime = 5000;
		
		ConnectionFactory cf = null;
		try 
		{
			cf = (ConnectionFactory) context.lookup("ConnectionFactory");
		} 
		catch (NamingException e1) 
		{
			e1.printStackTrace();
		}
		
		while(true)
		{
			try
			{
				Connection connection = cf.createConnection();
				connection.start();
				
				if(log != null)
					log.info("Connection to "+address+" established.");
				
				return connection;
			}
			catch(JMSException ex)
			{
				if(!(ex.getCause() instanceof ConnectException) && !(ex.getCause() instanceof InactivityIOException) 
						&& !(ex.getCause() instanceof SocketException) && !(ex.getCause() instanceof SocketTimeoutException))
				{
					ex.printStackTrace();
					throw new RuntimeException("Failed to connect to "+address, ex);
				}
				
				if(log != null)
					log.warning("Failed to connect to MQ server "+address+". Trying to reconnect to the message queue server in "+sleepTime+" sec...");
				
				try 
				{
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) 
				{
					throw new JCloudScaleException(ex, "Failed to connect to MQ server and interrupted Exception occured.");
				}
			}
		}
	}
	
	public static boolean sendMessage(Session session, MessageProducer producer, Serializable message) 
	{
		return sendMessage(session, producer, message, null);
	}
	
	public static boolean sendMessage(Session session, MessageProducer producer, Serializable message, String hostId) 
	{
		try
		{
			Message replyMessage = session.createObjectMessage(message);
			
			if(hostId != null)
				replyMessage.setStringProperty(OpenstackConfiguration.getJmsSelector(), hostId);
			
			producer.send(replyMessage);
			return true;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
	}
	
	public static String createMessageSelector(String hostId)
	{
		return OpenstackConfiguration.getJmsSelector() + " = '"+hostId+"'";
	}
}
