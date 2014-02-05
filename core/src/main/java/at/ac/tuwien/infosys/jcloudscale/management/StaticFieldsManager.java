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
package at.ac.tuwien.infosys.jcloudscale.management;

import java.io.Closeable;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;

public class StaticFieldsManager implements Closeable 
{
	private Logger log;
	private IMQWrapper mqForWrite; 
	private IMQWrapper mqForRead;

	private StaticFieldReadRequestHandler readHandler;
	private StaticFieldWriteRequestHandler writeHandler;
	
	public StaticFieldsManager()
			throws NamingException, JMSException 
	{
		this.log = JCloudScaleConfiguration.getLogger(this);
		start();
	}
	
	private void start() throws NamingException, JMSException 
	{
		mqForWrite = JCloudScaleConfiguration.createMQWrapper();
		mqForWrite.createQueueConsumer(
			JCloudScaleConfiguration.getConfiguration().server().getStaticFieldWriteRequestQueueName());
		mqForWrite.createTopicProducer(
				JCloudScaleConfiguration.getConfiguration().server().getStaticFieldWriteResponseTopicName());
		
		writeHandler = new StaticFieldWriteRequestHandler(mqForWrite);
		mqForWrite.registerListener(writeHandler);
		log.info("Started to listen for updates to static fields");
		
		mqForRead = JCloudScaleConfiguration.createMQWrapper();
		mqForRead.createQueueConsumer(
				JCloudScaleConfiguration.getConfiguration().server().getStaticFieldReadRequestQueueName());
		readHandler = new StaticFieldReadRequestHandler();
		mqForRead.registerListener(readHandler);
		log.info("Started to listen for requests for static field values");
	}
	
	@Override
	public void close() 
	{
		if(mqForWrite != null)
		{
			mqForWrite.close();
			mqForWrite = null;
			
			log.info("Stopped to listen for updates of static fields");
		}
		if(mqForRead != null)
		{
			mqForRead.close();
			mqForRead = null;
			
			log.info("Stopped to listen for requests for static field values");
		}

		if(readHandler != null)
		{
			readHandler.close();
			readHandler = null;
		}
		
		if(writeHandler != null)
		{
			writeHandler.close();
			writeHandler = null;
		}
	}
	
}
