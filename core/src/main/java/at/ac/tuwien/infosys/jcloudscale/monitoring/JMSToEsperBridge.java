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
package at.ac.tuwien.infosys.jcloudscale.monitoring;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.Event;

import com.espertech.esper.client.EPServiceProvider;

public class JMSToEsperBridge {
	
	private EPServiceProvider esper;
	private IMQWrapper mq; 
	private Logger log;
	
	public JMSToEsperBridge(EPServiceProvider esper) {
		this.esper = esper;
		log = JCloudScaleConfiguration.getLogger(this);
	}
	
	public void initializeBridge() {
		
		try {
			mq = JCloudScaleConfiguration.createMQWrapper(DeliveryMode.NON_PERSISTENT);
			
			// TODO Changed to Topic because MigrationSubsystem also listens for monitoring events
//			mq.createQueueConsumer(JCloudScaleConfiguration.getConfiguration()
//					.common().monitoring().getQueueName());//DestinationMap.MONITORING_QUEUE_NAME);
			mq.createTopicConsumer(JCloudScaleConfiguration.getConfiguration()
					.common().monitoring().getQueueName());
			
			mq.registerListener(new MonitoringListener());
		} catch (NamingException e) {
			e.printStackTrace();
			log.severe("Execption while initializing monitoring listener: "+e.getMessage());
		} catch (JMSException e) {
			e.printStackTrace();
			log.severe("Execption while initializing monitoring listener: "+e.getMessage());
		}
	}
	
	public void closeBridge() {
		
		try {
			mq.disconnect();
		} catch (NamingException e) {
			e.printStackTrace();
			log.severe("Execption while initializing monitoring listener: "+e.getMessage());
		} catch (JMSException e) {
			e.printStackTrace();
			log.severe("Execption while initializing monitoring listener: "+e.getMessage());
		}
		
	}
	
	private class MonitoringListener implements MessageListener {

		@Override
		public void onMessage(Message msg) {
			
			if(!(msg instanceof ObjectMessage)) {
				log.severe("Received invalid message in monitoring queue: "+(msg.getClass().getName()));
				return;
			}
			
			Serializable payload = null;
			try {
				payload = ((ObjectMessage)msg).getObject();
			} catch (JMSException e) {
				e.printStackTrace();
			}
			
			if(!(payload instanceof Event)) {
				log.severe("Received invalid message payload in monitoring queue: "+(payload.getClass().getName()));
				return;
			}
			
			esper.getEPRuntime().sendEvent(payload);
			
			log.finer("Published new esper message "+payload.toString());
			
		}
		
	}
	
}
