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
package at.ac.tuwien.infosys.jcloudscale.server.messaging;

import java.io.Closeable;
import java.util.logging.Logger;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.Event;

public class MonitoringMQHelper implements Closeable {

	private static MonitoringMQHelper instance = null;
	
	private IMQWrapper mq; 
	
	private Logger log;
	
	private MonitoringMQHelper() {
		
		log = JCloudScaleConfiguration.getLogger(this);
		
		try {
			mq = JCloudScaleConfiguration.createMQWrapper(DeliveryMode.NON_PERSISTENT);

			// TODO Changed to Topic because MigrationSubsystem also listens for monitoring events
//			mq.createQueueProducer(JCloudScaleConfiguration.getConfiguration()
//					.common().monitoring().getQueueName());//, DeliveryMode.PERSISTENT, 60000);
			mq.createTopicProducer(JCloudScaleConfiguration.getConfiguration()
					.common().monitoring().getQueueName());

		} catch (NamingException e) {
			e.printStackTrace();
			log.severe(e.getMessage());
		} catch (JMSException e) {
			e.printStackTrace();
			log.severe(e.getMessage());
		}
		
	}
	
	public synchronized static MonitoringMQHelper getInstance() {
		if(instance == null)
			instance = new MonitoringMQHelper();
		return instance;
	}
	
	public synchronized static void closeInstance()
	{
		if(instance == null)
			return;
		
		instance.close();
		instance = null;
	}

	public void sendEvent(Event event) throws JMSException {
		
		mq.oneway(event);
		
	}
	
	@Override
	public void close() 
	{
		if(this.mq != null)
		{
			this.mq.close();
			this.mq = null;
		}
	}

}
