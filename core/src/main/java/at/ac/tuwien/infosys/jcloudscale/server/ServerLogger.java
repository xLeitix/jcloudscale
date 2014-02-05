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

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.LogObject;

public class ServerLogger extends Handler {

	private IMQWrapper mq; 
	private boolean successfullyInit = false;
	
	public ServerLogger() {
		
		String formatterAsString = LogManager.getLogManager().getProperty(this.getClass().getName()+".formatter");
		if(formatterAsString != null) {
			try {
				@SuppressWarnings("unchecked")
                Class<Formatter> formatter = (Class<Formatter>) Class.forName(formatterAsString);
				setFormatter(formatter.newInstance());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		try {
			mq = JCloudScaleConfiguration.createMQWrapper(DeliveryMode.NON_PERSISTENT);
			mq.createQueueProducer(JCloudScaleConfiguration.getConfiguration().server().logging().getLoggingQueueName());//, DeliveryMode.PERSISTENT,30000);
			successfullyInit = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void close() throws SecurityException {
		try {
			mq.disconnect();
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void flush() {}

	@Override
	public void publish(LogRecord record) {
		
		Formatter formatter = getFormatter();
		if(formatter == null)
			formatter = new ServerLogFormatter();
		
		if(!successfullyInit) {
			System.err.println(formatter.format(record));
			return;
		}
		
		try {
			LogObject log = new LogObject();
			log.setFormatted(formatter.format(record));
			log.setRecord(record);
			mq.oneway(log);
			System.err.println(formatter.format(record));
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
