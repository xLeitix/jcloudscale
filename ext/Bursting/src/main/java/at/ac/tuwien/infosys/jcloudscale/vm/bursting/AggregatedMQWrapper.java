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
package at.ac.tuwien.infosys.jcloudscale.vm.bursting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.TimeoutException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;
import at.ac.tuwien.infosys.jcloudscale.utility.CancellationToken;

/**
 * @author RST
 * Aggregated IMQWrapper allows to do the same operations on the set of mq wrappers 
 * connected to different message queue servers.
 */
public class AggregatedMQWrapper implements IMQWrapper 
{
	private Map<IMQWrapper, String> wrappers = new HashMap<>();
	private Logger log = JCloudScaleConfiguration.getLogger(this);
	private ExecutorService threadPool;
	
	AggregatedMQWrapper(AggregatedMessageQueueConfiguration configuration)
			throws NamingException, JMSException 
	{
		threadPool = Executors.newCachedThreadPool();
		
		for(MessageQueueConfiguration cfg: configuration.getMqConfigurations())
			try
			{
				wrappers.put(cfg.newWrapper(), cfg.getServerAddress());
			}
			catch(Exception ex)
			{
				log.severe("Failed to construct MQWrapper for "+cfg.getServerAddress()+": "+ex);
			}
	}

	private static interface IMQAction
	{
		public MessageObject action(IMQWrapper wrapper) throws Exception;
	}
	
	private class MQActionApplier implements Runnable
	{
		private IMQAction action;
		private IMQWrapper wrapper;
		private Exception ex;
		private CountDownLatch countDown;
		private MessageObject result;
		private CancellationToken token;
		
		public MQActionApplier(CountDownLatch countDown, IMQWrapper wrapper, IMQAction action, CancellationToken token)
		{
			this.countDown = countDown;
			this.action = action;
			this.wrapper = wrapper;
			this.token = token;
		}
		
		@Override
		public void run() 
		{
			try 
			{
				result = this.action.action(wrapper);
				if(result != null)
				{
					//we have to cancel everything, we got result.
					if(token != null)
						token.cancel();
				}
				
			} catch (Exception e) 
			{
				ex = new Exception("Exception while executing task "+ action +" on wrapper to "+ wrappers.get(wrapper)+e, e);
			}
			finally
			{
				countDown.countDown();
			}
		}
		
		public Exception getException()
		{
			return ex;
		}
		
		public MessageObject getResult()
		{
			return result;
		}
	}
	
	private MessageObject forAll(IMQAction action)
	{
		return forAll(action, null);
	}
	private MessageObject forAll(IMQAction action, CancellationToken token)
	{
		CountDownLatch countDown = new CountDownLatch(this.wrappers.size());
		List<MQActionApplier> appliers = new ArrayList<>();
		
		// creating appliers
		for(IMQWrapper wrapper : this.wrappers.keySet())
			appliers.add(new MQActionApplier(countDown, wrapper, action, token));
		
		// running appliers
		for(MQActionApplier applier : appliers)
			threadPool.execute(applier);
		
		// waiting for completion.
		try {
			countDown.await();
		} catch (InterruptedException e) 
		{
			log.severe("Exception while awaiting for method execution: "+e);
		}
		
		// analyzing exceptions:
		MessageObject result = null;
		for(MQActionApplier applier : appliers)
		{
			if(applier.getException() != null)
			{
				log.severe("Exception while executing action: "+applier.getException());
				applier.getException().printStackTrace();
			}
			if(applier.getResult() != null)
				result = applier.getResult();
		}
		
		return result;
	}
	
	@Override
	public void disconnect() throws JMSException, NamingException 
	{
		close();
	}

	@Override
	public void close() 
	{
		if(threadPool == null)
			return;
		
		threadPool.shutdown();
		
		log.info("Aggregated MQ: shutting down.");
		
		try 
		{
			if(!this.threadPool.awaitTermination(10, TimeUnit.SECONDS))
				threadPool.shutdownNow();
		} catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
		
		for(IMQWrapper wrapper : this.wrappers.keySet())
			wrapper.close();
		
		threadPool = null;
		this.wrappers = null;
	}

	@Override
	public boolean configurationEquals(MessageQueueConfiguration newConfiguration) 
	{
		return true;//should not matter for us, this method is used on server where we should not run.
	}

	@Override
	public void createQueueProducer(final String destName) throws NamingException,JMSException 
	{
		forAll(new IMQAction(){
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception {
				wrapper.createQueueProducer(destName);
				return null;
			}
		});
	}

	@Override
	public void createTopicProducer(final String destName) throws NamingException,JMSException 
	{
		forAll(new IMQAction(){
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception {
				wrapper.createTopicProducer(destName);
				return null;
			}
		});
	}

	@Override
	public void createQueueConsumer(final String destName) throws NamingException,JMSException 
	{
		forAll(new IMQAction(){
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception {
				wrapper.createQueueConsumer(destName);
				return null;
			}
		});
	}

	@Override
	public void createTopicConsumer(final String destName, final String messageSelector) throws NamingException, JMSException 
	{
		forAll(new IMQAction(){
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception {
				wrapper.createTopicConsumer(destName, messageSelector);
				return null;
			}
		});
	}

	@Override
	public void createTopicConsumer(final String destName) throws NamingException, JMSException 
	{
		forAll(new IMQAction(){
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception {
				wrapper.createTopicConsumer(destName);
				return null;
			}
		});
	}

	@Override
	public void registerListener(final MessageListener listener) throws JMSException 
	{
		forAll(new IMQAction(){
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception {
				wrapper.registerListener(listener);
				return null;
			}
		});
	}

	@Override
	public MessageObject requestResponseToCSHost(MessageObject obj,	UUID correlationId, UUID hostId) throws JMSException,TimeoutException 
	{
		return requestResponseToCSHost(obj, correlationId, hostId, null);
	}

	@Override
	public MessageObject requestResponse(MessageObject obj, UUID correlationId)
			throws JMSException, TimeoutException 
	{
		return requestResponse(obj, correlationId, null);
	}
	
	@Override
	public MessageObject requestResponseToCSHost(final MessageObject obj, final UUID correlationId, final UUID hostId, CancellationToken cancelToken) throws JMSException, TimeoutException 
	{
		final CancellationToken token = new CancellationToken(cancelToken);
		return forAll(new IMQAction() {
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception 
			{
				return wrapper.requestResponseToCSHost(obj, correlationId, hostId, token);
			}
		}, token);
	}

	@Override
	public MessageObject requestResponse(final MessageObject obj, final UUID correlationId,	CancellationToken cancelToken) throws JMSException,	TimeoutException 
	{
		final CancellationToken token = new CancellationToken(cancelToken);
		return forAll(new IMQAction() {
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception 
			{
				return wrapper.requestResponse(obj, correlationId, token);
			}
		}, token);
	}

	@Override
	public void oneway(final MessageObject obj) throws JMSException 
	{
		forAll(new IMQAction(){
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception {
				wrapper.oneway(obj);
				return null;
			}
		});
	}

	@Override
	public void oneway(final MessageObject obj, final UUID correlationId) throws JMSException 
	{
		forAll(new IMQAction(){
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception {
				wrapper.oneway(obj, correlationId);
				return null;
			}
		});
	}

	@Override
	public void onewayToCSHost(final MessageObject obj, final UUID correlationId, final UUID hostId) throws JMSException 
	{
		forAll(new IMQAction(){
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception {
				wrapper.onewayToCSHost(obj, correlationId, hostId);
				return null;
			}
		});
	}

	@Override
	public void respond(final MessageObject obj, final Destination dest, final UUID correlationId) throws JMSException 
	{
		forAll(new IMQAction(){
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception {
				wrapper.respond(obj, dest, correlationId);
				return null;
			}
		});
	}

	@Override
	public void respond(final MessageObject obj, final Destination dest) throws JMSException 
	{
		forAll(new IMQAction(){
			@Override
			public MessageObject action(IMQWrapper wrapper) throws Exception {
				wrapper.respond(obj, dest);
				return null;
			}
		});
	}

}
