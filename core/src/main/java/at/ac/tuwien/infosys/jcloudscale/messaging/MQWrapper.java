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

import java.io.EOFException;
import java.io.Serializable;
import java.net.SocketException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import javax.naming.NamingException;

import org.apache.activemq.ConnectionFailedException;
import org.apache.activemq.transport.InactivityIOException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;
import at.ac.tuwien.infosys.jcloudscale.utility.CancellationToken;

public class MQWrapper implements IMQWrapper
{
	private static final Map<String, JMSConnectionHolder> connections = new ConcurrentHashMap<String, JMSConnectionHolder>();
	private static final Object syncRoot = new Object();
	
	private Session session;
	private MessageProducer producer;
	private Destination producerDestination;
	private MessageConsumer consumer;
	private Destination consumerDestination;
	private String address;
	private OneWayThreadPoolWrapper oneWaySender;
	private MessageQueueConfiguration cfg;
	private int deliveryMode;
	
	//--------------------------------------------------------------
	
	protected MQWrapper(MessageQueueConfiguration configuration, int deliveryMode) throws NamingException, JMSException
	{
		this.cfg = configuration;
		this.deliveryMode = deliveryMode;
		
		oneWaySender = new OneWayThreadPoolWrapper(cfg.asyncSendThreads, cfg.asyncSendShutdownTimeout);
		
		connect(configuration.serverAddress);
	}
	
	@Override
	public boolean configurationEquals(MessageQueueConfiguration newConfiguration) 
	{
		boolean equals = cfg.asyncSendThreads == newConfiguration.asyncSendThreads && 
			cfg.asyncSendShutdownTimeout == newConfiguration.asyncSendShutdownTimeout &&
			cfg.serverAddress.equals(newConfiguration.serverAddress) &&
			cfg.requestTimeout == newConfiguration.requestTimeout;
		
		if(!equals)
			return equals;
		
		//in addition we have to check if connection holder feels that this config is different.
		JMSConnectionHolder connectionHolder = connections.get(address);
		return connectionHolder == null || connectionHolder.configurationEquals(newConfiguration);
	}
	//--------------------------------------------------------------
	
	private static class OneWayThreadPoolWrapper
	{
		private ExecutorService threadPool;
		private int threadPoolThreads;
		private long threadPoolShutdownTimeout;
		
		public OneWayThreadPoolWrapper(int threadPoolThreads, long threadPoolShutdownTimeout)
		{
			this.threadPoolThreads = threadPoolThreads;
			this.threadPoolShutdownTimeout = threadPoolShutdownTimeout;
		}
		
		private synchronized ExecutorService getThreadPool()
		{
			if(threadPool == null)
				threadPool = Executors.newFixedThreadPool(threadPoolThreads);
			
			return threadPool;
		}
		
		/**
		 * Add a task to execute on threadPool
		 */
		public void add(Runnable task)
		{
			getThreadPool().execute(task);
		}
		
		/**
		 * Awaits all current tasks to be executed for a timeout and shutsdown thread pool.
		 */
		public synchronized void shutdown()
		{
			if(threadPool == null)
				return;
			
			threadPool.shutdown();
			try 
			{
				if(!threadPool.awaitTermination(this.threadPoolShutdownTimeout, TimeUnit.MILLISECONDS))
					threadPool.shutdownNow();
			} 
			catch (InterruptedException e) 
			{
				threadPool.shutdownNow();
			}
			
			threadPool = null;
		}
	}
	
	private class OneWayMessageSendTask implements Runnable
	{
		private Message msg;
		private long timeout;
		private long initTime;
		
		public OneWayMessageSendTask(Message msg, long timeout)
		{
			initTime = System.currentTimeMillis();
			this.timeout = timeout;
			this.msg = msg;
		}
		
		@Override
		public void run() 
		{
		    long currentTime = System.currentTimeMillis();
			if(initTime + timeout > currentTime)
			{//we're within timeout.
				try 
				{
					//retrySend(msg, timeout);//just using same timeout. if needed actually lost time :(System.currentTimeMillis() - initTime)
					retryAction(new SendMessageAction(msg), false, timeout);
				} 
				catch (JMSException e) 
				{
				        JCloudScaleConfiguration.getLogger(MQWrapper.class).severe("Failed to send one-way message : "+e);
					e.printStackTrace();
				}
			}
			else
			{//we are dropping the message.
			    try {
                                Serializable content = ((ObjectMessage)msg).getObject();
                                String clazz = content != null ? content.getClass().getName() : "NULL";
                                JCloudScaleConfiguration.getLogger(MQWrapper.class)
                                                        .warning("Dropping the message of type "+clazz+" because timeout "
                                                                +timeout+"ms is violated: "+(currentTime - initTime)+"ms.");
			    } catch (JMSException e) {
		                    e.printStackTrace();
		              }
			}
		}
		
	}
	
	//--------------------------------------------------------------
	private Connection getConnection(String address, boolean isReconnect) throws JMSException, NamingException
	{
		synchronized (syncRoot) 
		{
			JMSConnectionHolder holder = connections.get(address);
			
			if(holder == null)
			{
				holder = new JMSConnectionHolder(this.cfg, address);
				connections.put(address, holder);
			}
			else
			{
				// we won't check if configuration changed on reconnect.
				// Otherwise we might face the problem that 2 mq wrappers will offer their configuration all the time.
				if(!isReconnect && !holder.configurationEquals(this.cfg))
					holder.replaceConfiguration(this.cfg);
			}
				
			return holder.getConnection(this);
		}
	}
	
	private Session createSession(String address, boolean isReconnect) throws JMSException, NamingException 
	{
		Connection connection = getConnection(address, isReconnect);
		
		if(connection == null)
			throw new JCloudScaleException("Failed to aquire connection to the "+address);
		
		while(true)
		{
			try
			{
				return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			}
			catch(ConnectionFailedException e)
			{
				try
				{
					connection.close();
				}
				catch(JMSException ex)
				{
					//we have to try closing. This releases resources and closes all sessions and listeners.
				}
				
				connection.start();
			}
		}
	}
	
	private void closeConnection(String address) throws JMSException, NamingException
	{
		synchronized (syncRoot) 
		{
			JMSConnectionHolder holder = connections.get(address);
			
			if(holder == null)
				return;
			
			holder.removeRegisteredWrapper(this);
			
			if(holder.getRegisteredWrappersCount() == 0)
			{
				connections.remove(address);
				holder.close();
			}
		}
	}
	
	private Context getContext(String address)
	{
		JMSConnectionHolder holder = connections.get(address);
		
		if(holder == null)
			throw new IllegalStateException("Cannot provide context before opening connection. Open connection first.");
		
		return holder.getContext();
	}
	
	void reconnect() throws JMSException, NamingException
	{
		Session oldSession = session;
		
		//
		// Creating new objects.
		//
		if(session != null)
		{
			session = createSession(address, true);
		}
		
		if(producer != null)
		{
			int mode = producer.getDeliveryMode();
			long ttl = producer.getTimeToLive();
			
			producer = session.createProducer(producerDestination);
			producer.setDeliveryMode(mode);
			producer.setTimeToLive(ttl);
		}
		
		if(consumer != null)
		{
			String messageSelector = consumer.getMessageSelector();
			MessageListener listener = consumer.getMessageListener();
			
			if(messageSelector == null)
				consumer = session.createConsumer(consumerDestination);
			else
				consumer = session.createConsumer(consumerDestination, messageSelector);
			
			if(listener != null)
				consumer.setMessageListener(listener);
		}
		
		//
		// closing old session
		//
		try
		{
			if(oldSession != null)
				oldSession.close();//there is no need to close producers and consumers of a closed session.
		}
		catch(JMSException ex)
		{}
	}
	
	private interface IAction
	{
	        String getName();
		Message action() throws JMSException;
	}
	
	private class SendMessageAction implements IAction
	{
		private Message msg;
		public SendMessageAction(Message msg)
		{
			this.msg = msg;
		}
		
		@Override
                public String getName()
		{
		    String type = msg != null ? msg.getClass().getName() : "NULL MESSAGE";
		    
		    if(msg != null && msg instanceof ObjectMessage)
		    {
                        try 
                        {
                            Serializable obj = ((ObjectMessage)msg).getObject();
                            
        		    if(obj != null)
        		        type = obj.getClass().getName();
        		    else
        		        type = "NULL OBJECT";
                        } catch (JMSException e) { }
		    }
		    
		    return "Sending message "+type;
		}
		
		@Override
		public Message action() throws JMSException 
		{
			producer.send(msg);
			return null;
		}
	}
	
	private Message retryAction(IAction action, boolean expectAnswer, long timeout) throws JMSException
	{
		return retryAction(action, expectAnswer, timeout, null);
	}
	
	private Message retryAction(IAction action, boolean expectAnswer, long timeout, CancellationToken cancelToken) throws JMSException
	{
		long startTime = System.currentTimeMillis();
		while(true)
		{
			try
			{
				if(CancellationToken.isCancelled(cancelToken))
					return null;
				
				Message reply = action.action();
				
				if(expectAnswer)
					if(reply != null || System.currentTimeMillis() - startTime > timeout)
						return reply;
					else
						continue;
				else
					return null;
			}
			catch(JMSException ex)
			{
			        JCloudScaleConfiguration.getLogger(this).warning("Failed to perform action \""+action.getName()+"\":"+
			                    ex+". Will retry soon.");
				if(!connectionClosedException(ex))
						throw ex;
				
				if(System.currentTimeMillis() - startTime > timeout)
					throw ex;
				
				try 
				{
					Thread.sleep(500);
				} 
				catch (InterruptedException e) 
				{
					throw ex;
				}
			}
		}
	}
	
	private boolean connectionClosedException(JMSException ex) 
	{
		if(ex == null)
			return false;
		
		Throwable cause = ex.getCause();
		
		if(cause == null)
			return false;
		
		return cause instanceof SocketException || 
			   cause instanceof EOFException || 
			   cause instanceof InactivityIOException;
	}
	
	//--------------------------------------------------------------
	
	private void connect(String address) throws NamingException, JMSException {
		
		if(this.address != null)
		{
			closeConnection(this.address);
		}
		
		this.address = address;
		session = createSession(address, false);
	}

	@Override
	public void disconnect() throws JMSException, NamingException {
		
		oneWaySender.shutdown();
		
		if(session == null)
			throw new InvalidMQWrapperStateException("Tried to disconnect wrapper which is not connected");
		
		if(producer != null)
		{
			producer.close();
			producerDestination = null;
			producer = null;
		}
		
		if(consumer != null)
		{
			consumer.close();
			consumerDestination = null;
			consumer = null;
		}
		
		session.close();
		session = null;
		
		closeConnection(address);
	}
	
	@Override
	public void close()
	{
		if(session != null)
			try	
			{
				disconnect();
			}
			catch(JMSException | NamingException ex)
			{
			    JCloudScaleConfiguration.getLogger(this).warning("Failed to close MQWrapper: "+ex);
			}
	}
	
	@Override
	public void createQueueProducer(String destName) throws NamingException, JMSException
	{
		createQueueProducer(destName, this.deliveryMode, this.cfg.requestTimeout);
	}
	
	private void createQueueProducer(String destName, int mode, long ttl)
			throws NamingException, JMSException {
		
		if(session == null)
			throw new InvalidMQWrapperStateException("Tried to create producer with disconnected wrapper");
		
		producerDestination = 
				(Destination) getContext(address).lookup("dynamicQueues/"+destName);
		producer = session.createProducer(producerDestination);
		producer.setDeliveryMode(mode);
		producer.setTimeToLive(ttl);
	}
	
	@Override
	public void createTopicProducer(String destName) throws NamingException, JMSException
	{
		createTopicProducer(destName, this.deliveryMode, this.cfg.requestTimeout);
	}
	
	private void createTopicProducer(String destName, int mode, long ttl)
			throws NamingException, JMSException {
		
		if(session == null)
			throw new InvalidMQWrapperStateException("Tried to create producer with disconnected wrapper");
		
		producerDestination =
				(Destination) getContext(address).lookup("dynamicTopics/"+destName);
		producer = session.createProducer(producerDestination);
		producer.setDeliveryMode(mode);
		producer.setTimeToLive(ttl);
	}
	
	@Override
	public void createQueueConsumer(String destName)
			throws NamingException, JMSException {
		
		if(session == null)
			throw new InvalidMQWrapperStateException("Tried to create consumer with disconnected wrapper");
		
		consumerDestination =
				(Destination) getContext(address).lookup("dynamicQueues/"+destName);
		
		consumer = session.createConsumer(consumerDestination);
	}
	
	@Override
	public void createTopicConsumer(String destName, String messageSelector)
			throws NamingException, JMSException {
		
		if(session == null)
			throw new InvalidMQWrapperStateException("Tried to create consumer with disconnected wrapper");
		
		consumerDestination =
				(Destination) getContext(address).lookup("dynamicTopics/"+destName);
		
		if(messageSelector == null)
			consumer = session.createConsumer(consumerDestination);
		else
			consumer = session.createConsumer(consumerDestination, messageSelector);//TODO: why not to make all that key-value assignment here?
	}
	
	@Override
	public void createTopicConsumer(String destName) throws NamingException, JMSException 
	{
		
		createTopicConsumer(destName, null);
	}
	
	@Override
	public void registerListener(MessageListener listener) throws JMSException {
		
		if(consumer == null)
			throw new InvalidMQWrapperStateException("Tried to register listener, but no consumer found");
		
		consumer.setMessageListener(listener);
	}
	
	@Override
	public MessageObject requestResponseToCSHost(MessageObject obj, UUID correlationId, UUID hostId) throws JMSException, TimeoutException
	{
		return requestResponse(obj, correlationId, hostId, null);
	}
	
	@Override
	public MessageObject requestResponseToCSHost(MessageObject obj, UUID correlationId, UUID hostId, CancellationToken cancelToken) throws JMSException, TimeoutException
	{
		return requestResponse(obj, correlationId, hostId, cancelToken);
	}
	
	@Override
	public MessageObject requestResponse(MessageObject obj, UUID correlationId) throws JMSException, TimeoutException
	{
		return requestResponse(obj, correlationId, null, null);
	}
	
	@Override
	public MessageObject requestResponse(MessageObject obj, UUID correlationId, CancellationToken cancelToken) throws JMSException, TimeoutException
	{
		return requestResponse(obj, correlationId, null, cancelToken);
	}
	
	public MessageObject requestResponse(MessageObject obj) throws JMSException, TimeoutException
	{
		return requestResponse(obj, null, null, null);
	}

	private MessageObject requestResponse(MessageObject obj, UUID correlationId, UUID hostId, CancellationToken cancelToken)
			throws JMSException, TimeoutException {
		
		if(producer == null)
			throw new InvalidMQWrapperStateException("Tried to invoke request/response operation, but no producer found");
		
		if(consumer == null)
			throw new InvalidMQWrapperStateException("Tried to invoke request/response operation, but no consumer found");
		
		if(CancellationToken.isCancelled(cancelToken))
			return null;
		
		ObjectMessage om = session.createObjectMessage(obj);
		UUID correlationIdToUse = null;
		if(correlationId != null)
			correlationIdToUse = correlationId;
		else
			correlationIdToUse = UUID.randomUUID();
		om.setJMSCorrelationID(correlationIdToUse.toString());
		om.setJMSReplyTo(consumerDestination);
		
		if(hostId != null)
			om.setStringProperty("CS_HostId", hostId.toString());
		
		//retrySend(om, this.cfg.retryTimeout, cancelToken);
		retryAction(new SendMessageAction(om), false, this.cfg.retryTimeout, cancelToken);
		
		if(CancellationToken.isCancelled(cancelToken))
			return null;
		
		ObjectMessage reply = (ObjectMessage)retryAction(new IAction() {
		        @Override
                        public String getName(){return "Receiving answer within Request/Response operation";}
			@Override
			public Message action() throws JMSException 
			{
				//anyways we are iterating until requestTimeout reached. 
				//this way we get much smaller interval between cancellation checks.
				return consumer.receive(cfg.requestTimeout/100);
			}
		}, true, this.cfg.requestTimeout, cancelToken);
		
		if(CancellationToken.isCancelled(cancelToken))
			return null;
		
		if(reply == null)
			throw new TimeoutException();
		
		return (MessageObject) reply.getObject();
	}
	
	@Override
	public void oneway(MessageObject obj)
			throws JMSException {
		
		oneway(obj, null);
		
	}
	
	@Override
	public void oneway(MessageObject obj, UUID correlationId)
			throws JMSException {
		
		if(producer == null)
			throw new InvalidMQWrapperStateException("Tried to invoke oneway operation, but no producer found");
		
		ObjectMessage om = session.createObjectMessage(obj);
		
		if(correlationId != null)
			om.setJMSCorrelationID(correlationId.toString());
		
		oneWaySender.add(new OneWayMessageSendTask(om, this.cfg.retryTimeout));
	}
	
	@Override
	public void onewayToCSHost(MessageObject obj, UUID correlationId, UUID hostId)
			throws JMSException {
		
		if(producer == null)
			throw new InvalidMQWrapperStateException("Tried to invoke oneway operation, but no producer found");
		
		ObjectMessage om = session.createObjectMessage(obj);
		
		if(correlationId != null)
			om.setJMSCorrelationID(correlationId.toString());
		
		if(hostId != null)
			om.setStringProperty("CS_HostId", hostId.toString());
		
		oneWaySender.add(new OneWayMessageSendTask(om, this.cfg.retryTimeout));
	}
	
	@Override
	public void respond(final MessageObject obj, final Destination dest, final UUID correlationId) throws JMSException 
	{
		if(session == null)
			throw new InvalidMQWrapperStateException("Tried to respond to message, but wrapper is not connected");
		
		retryAction(new IAction() {
		        @Override
                        public String getName(){ return "Respond to the specified destination";}
			@Override
			public Message action() throws JMSException 
			{
				MessageProducer tmpProducer = session.createProducer(dest);
				try
				{
					ObjectMessage om = session.createObjectMessage(obj);
					
					if(correlationId != null)
						om.setJMSCorrelationID(correlationId.toString());
					
					tmpProducer.send(om);
					return null;
				}
				finally
				{
					tmpProducer.close();
				}
			}
		}, false, this.cfg.retryTimeout);
	}
	
	@Override
	public void respond(MessageObject obj, Destination dest) throws JMSException {
		respond(obj, dest, null);
	}

	/**
	 * Shutdowns all known connections. This method is more like a safeguard/shutdown hook.
	 * @return <b>true</b> if there were some connections that needed to shutdown. Otherwise, <b>false</b>.
	 */
	public static boolean shutdownAllConnections() 
	{
		if(connections.size() == 0)
			return false;
		
		for(JMSConnectionHolder holder : connections.values())
			try 
			{
				holder.close();
			} catch (JMSException | NamingException e) 
			{//let's just close...
				e.printStackTrace();
			}
		
		return true;
	}
}
