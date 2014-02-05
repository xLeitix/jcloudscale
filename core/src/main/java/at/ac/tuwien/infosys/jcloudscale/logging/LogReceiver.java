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
package at.ac.tuwien.infosys.jcloudscale.logging;

import java.util.Hashtable;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.LogObject;

public class LogReceiver {
	
	private IMQWrapper mq; 
	private Hashtable<String, Logger> serverLoggers = 
			new Hashtable<String, Logger>();
	private long messageTimeout = 2000;
	
	public LogReceiver(){
		messageTimeout = JCloudScaleConfiguration.getConfiguration()
							.server().logging().getLogMessageTimeout();
		try {
			mq = JCloudScaleConfiguration.createMQWrapper();
			mq.createQueueConsumer(JCloudScaleConfiguration.getConfiguration()
								.server().logging().getLoggingQueueName());
			
			mq.registerListener(new LogServerListener());
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
		
	}
	
	public void close() {
		try {
			mq.disconnect();
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
	
	private class LogServerListener implements MessageListener {

		@Override
		public void onMessage(Message message) {
			
			if(message instanceof ObjectMessage) {
				
				ObjectMessage m = (ObjectMessage) message;
				
				try {
					
					LogObject lo = (LogObject) m.getObject(); 
					
					// throw away old messages
					if(m.getJMSTimestamp() < System.currentTimeMillis() - messageTimeout)
						return;
				
					Logger thisLogger = null;
					String loggerName = lo.getRecord().getLoggerName();
					if(serverLoggers.containsKey(loggerName))
						thisLogger = serverLoggers.get(loggerName);
					else 
					{
						thisLogger = JCloudScaleConfiguration.getLogger(loggerName);
						serverLoggers.put(loggerName, thisLogger);
					}
					
					// TODO: note that this will ignore any sort of logger redirection - 
					// no matter what is configured in logging.properties, the output
					// always goes to (and only to) System.err ...
					// TODO - I don't know if by default printing a log if we don't have a filter
					// is a good idea - probably not, but it is a start I guess :)
					if(thisLogger.getFilter() == null || thisLogger.getFilter().isLoggable(lo.getRecord()))
						System.err.print(lo.getFormatted());
					
				} catch (JMSException e) {
					e.printStackTrace();
				}
				
			}
			
		}
		
	}
}
