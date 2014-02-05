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
package at.ac.tuwien.infosys.jcloudscale.vm.localVm;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ReturnObject;
import at.ac.tuwien.infosys.jcloudscale.vm.VirtualHostProxy;

public class ClientReturnListener implements MessageListener, Closeable
{
	private VirtualHostProxy client;
	protected Logger log;
	protected ExecutorService threadpool;
	
	public ClientReturnListener(VirtualHostProxy client) {
		this.client = client;
		log = JCloudScaleConfiguration.getLogger(this);
		threadpool = Executors.newCachedThreadPool();
	}

	@Override
	public void close()
	{
		if(threadpool != null)
		{
			threadpool.shutdown();
			try 
			{
				if(!threadpool.awaitTermination(10, TimeUnit.SECONDS))//waiting 10 seconds to shutdown.
					threadpool.shutdownNow();
			} 
			catch (InterruptedException e) 
			{
			}
			
			threadpool = null;
		}
	}
	
	@Override
	public void onMessage(Message msg) {
		
		threadpool.execute(new MessageHandler(msg));
		
	}
	
	protected class MessageHandler implements Runnable {

		private Message msg;
		
		public MessageHandler(Message msg) {
			this.msg = msg;
		}
		
		@Override
		public void run() {
			
			
			if(!(msg instanceof ObjectMessage)) {
				throw new JCloudScaleException("Processed unknown message from return queue: "+msg.toString());
			} 
			
			try {
				ReturnObject o = (ReturnObject) ((ObjectMessage)msg).getObject();
				UUID corrId = UUID.fromString(msg.getJMSCorrelationID());
				client.setResult(corrId, o);
				client.unlock(corrId);
			} catch (JMSException e) {
				log.severe(e.getMessage());
				throw new JCloudScaleException(e);
			}
			
		}

	}
}
