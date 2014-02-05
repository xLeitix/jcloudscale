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

import java.net.ConnectException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.activemq.transport.InactivityIOException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;

public class JMSConnectionHolder  implements ExceptionListener
{
	private Connection connection;
	private Context context;
	private final List<MQWrapper> registeredWrappers = new ArrayList<MQWrapper>();
	private MessageQueueConfiguration cfg;
	private String address;
	
	JMSConnectionHolder(MessageQueueConfiguration configuration, String address) throws JMSException, NamingException
	{
		this.cfg = configuration;
		this.address = address;
		
		this.context = createContext(address, cfg.serverPort, cfg.messageSizeLimit);
		this.connection = createConnection(context, this, cfg.connectionTimeout, cfg.reconnectIntervalLimit, JCloudScaleConfiguration.getLogger(this));
	}
	
	Context getContext()
	{
		return context;
	}
	
	boolean configurationEquals(MessageQueueConfiguration newCfg)
	{
		return cfg.serverPort == newCfg.serverPort &&
				cfg.messageSizeLimit == newCfg.messageSizeLimit &&
				cfg.connectionTimeout == newCfg.connectionTimeout &&
				cfg.reconnectIntervalLimit == newCfg.reconnectIntervalLimit;
	}
	
	void replaceConfiguration(MessageQueueConfiguration newCfg)
	{
		if(!configurationEquals(newCfg))
		{
			this.cfg = newCfg;
			
			// re-creating context with new parameters.
			try 
			{
				Context oldContext = this.context;
				
				this.context = createContext(this.address, cfg.serverPort, cfg.messageSizeLimit);
				
				if(oldContext != null)
					oldContext.close();
				
			} catch (NamingException e) 
			{//we don't want to throw here. let's just use old context if we fail. 
				JCloudScaleConfiguration.getLogger(this).severe("Failed to re-create context with new configuration parameters: "+e);
			}
			
			reconnect();
		}
	}
	
	synchronized Connection getConnection(MQWrapper wrapper)
	{
		if(!registeredWrappers.contains(wrapper))
			registeredWrappers.add(wrapper);
		
		return connection;
	}
	
	synchronized void removeRegisteredWrapper(MQWrapper wrapper)
	{
		registeredWrappers.remove(wrapper);
	}
	
	synchronized int getRegisteredWrappersCount()
	{
		return registeredWrappers.size();
	}

	public synchronized void close() throws JMSException, NamingException 
	{
		if(connection != null)
			connection.close();
		connection = null;
		
		if(context != null)
			context.close();
		context = null;
		
		registeredWrappers.clear();
	}

	@Override
	public synchronized void onException(JMSException ex) 
	{
		JCloudScaleConfiguration.getLogger(this)
			.info("Connection Exception occured on connection to "+getProviderUrl(context)+" : "+ex.toString());
		
		reconnect();
	}
	
	//-------- STATIC METHODS ------------------------------
	
	private static Context createContext(String address, int serverPort, long messageSizeLimit) throws NamingException
	{
		Properties props = new Properties();
		props.setProperty(Context.INITIAL_CONTEXT_FACTORY,"org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		
		String providerUrl = "tcp://"+address+":"+serverPort;
		if(messageSizeLimit > 0)
			providerUrl+="?wireFormat.maxFrameSize="+messageSizeLimit;
			
		props.setProperty(Context.PROVIDER_URL,providerUrl);
		return new InitialContext(props);
	}
	
	private static Connection createConnection(Context context, ExceptionListener exceptionListener, long connectionTimeout, long reconnectIntervalLimit, Logger log) throws NamingException, JMSException
	{
		ConnectionFactory cf = (ConnectionFactory) context.lookup("ConnectionFactory");
		
		int sleepTime = 1000;//we start with 1 sec.
		long connectionStartTime = System.currentTimeMillis();
		
		while(true)
		{
			try
			{
				Connection connection = cf.createConnection();
				connection.setExceptionListener(exceptionListener);
				connection.start();
				
				if(log != null)
					log.info("Connection to "+getProviderUrl(context)+" established.");
				
				return connection;
			}
			catch(JMSException ex)
			{
				if(!(ex.getCause() instanceof ConnectException) && !(ex.getCause() instanceof InactivityIOException) && !(ex.getCause() instanceof SocketException))
					throw ex;
				
				if(System.currentTimeMillis() - connectionStartTime > connectionTimeout)
					 throw new JCloudScaleException(ex, "Failed to connect to message queue within timeout.");
				else
					if(log != null)
						log.warning("Failed to connect to MQ server "+getProviderUrl(context)+". Trying to reconnect to the message queue server in "+sleepTime/1000+" sec...");
				
				try 
				{
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) 
				{
					throw new JCloudScaleException(ex, "Failed to connect to MQ server and interrupted Exception occured.");
				}

				// this is arithmetically selected value to have nice time increasing curve and lots of probes at the beginning.
				sleepTime = (int)Math.min(sleepTime * 1.8, reconnectIntervalLimit);
			}
		}
	}
	
	/**
	 * Checks if the MessageQueue is available on specified address and port.
	 * @return <b>true</b> if server is available. Otherwise -- <b>false</b>.
	 * @throws NamingException
	 * @throws JMSException
	 * if connection failed from any reasons except the mq is not running on that host/port.
	 */
	public static boolean isMessageQueueServerAvailable(String hostname, int port) throws NamingException, JMSException
	{
		Context context = null;
		Connection connection = null;
		try
		{
			context = JMSConnectionHolder.createContext(hostname, port, 0);
			connection = JMSConnectionHolder.createConnection(context, null, 
													0, 0, null);//we will not retry inside the method, we were asked only to check. 
			
			//we have to create session to avoid some warnings about aborted connection.
			connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
			
			return true;
		}
		catch(JCloudScaleException ex)
		{
			return false;//failed to connect goes here.
		}
		finally
		{
			if(connection != null)
				connection.close();
			
			if(context != null)
				context.close();
		}
	}
	
	/**
//	 * Checks if the MessageQueue is available on the predefined address and port.
//	 * @return <b>true</b> if server is available. Otherwise -- <b>false</b>.
//	 * @throws NamingException
//	 * @throws JMSException
//	 * if connection failed from any reasons except the mq is not running on that host/port.
//	 */
//	public static boolean isMessageQueueServerAvailable() throws NamingException, JMSException
//	{
//		MessageQueueConfiguration cfg = JCloudScaleConfiguration.getConfiguration().common().communication();
//		return isMessageQueueServerAvailable(cfg.serverAddress, cfg.serverPort);
//	}
	
	//----------------PRIVATE METHODS--------------

	private static String getProviderUrl(Context context)
	{
		try
		{
			return context.getEnvironment().get(Context.PROVIDER_URL).toString();
		}
		catch(NamingException ex)
		{
			return "";
		}
	}
	
	private void reconnect() 
	{
		//
		// Creating new connection
		//
		Connection oldConnection = connection;
		try 
		{
			connection = createConnection(context, this, 
						cfg.connectionTimeout, cfg.reconnectIntervalLimit, JCloudScaleConfiguration.getLogger(this));
		} 
		catch (NamingException | JMSException e) 
		{
			JCloudScaleConfiguration.getLogger(this)
					.severe("Failed to create new connection: "+e.toString());
		}
		
		//
		// Reconnecting all subscribed wrappers.
		//
		for(MQWrapper wrapper : registeredWrappers)
		{
			try
			{
				wrapper.reconnect();
			}
			catch(JMSException | NamingException e)
			{
				JCloudScaleConfiguration.getLogger(this)
						.severe("Wrapper "+wrapper+"failed to reconect. Exception occured: "+e);
			}
		}
		//
		// Closing old connection. Just to release resources.
		//
		try 
		{
		    if(oldConnection != null)
			oldConnection.close();
		} catch (JMSException e) 
		{
		}
	}
}