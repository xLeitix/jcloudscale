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
package at.ac.tuwien.infosys.jcloudscale.classLoader.simple;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import org.springframework.core.io.ClassPathResource;

import at.ac.tuwien.infosys.jcloudscale.classLoader.RemoteClassLoaderUtils;
import at.ac.tuwien.infosys.jcloudscale.classLoader.simple.dto.ClassRequest;
import at.ac.tuwien.infosys.jcloudscale.classLoader.simple.dto.ClassResponse;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;

public class RemoteClassProvider implements MessageListener, Closeable
{
	private Logger log;
	private IMQWrapper mq; 
	
	/**
	 * Creates new instance of RemoteClassProvider that satisfies class dependency
	 * requests from the cloud instances.
	 */
	RemoteClassProvider(SimpleRemoteClassLoaderConfiguration configuration)
	{
		this.log = JCloudScaleConfiguration.getLogger(this);

		try 
		{
			this.mq = JCloudScaleConfiguration.createMQWrapper();
			this.mq.createQueueConsumer(configuration.requestQueue);
			this.mq.registerListener(this);
		} 
		catch (JMSException | NamingException e) 
		{
			log.severe("Failed to connect to message queue from RemoteClassProvider:" + e.toString());
			throw new JCloudScaleException(e, "Failed to connect to message queue from RemoteClassProvider");
		}	
	}
	
	@Override
	public void onMessage(Message message)
	{
		String className = null;
		
		try
		{
			className = ((ClassRequest)((ObjectMessage)message).getObject()).className;
			
			log.fine("Message received:" + className);
			
			// we expect sender to specify the query he's waiting response into the JMSReplyTo
			Destination destination = message.getJMSReplyTo();
			UUID correlationId = UUID.fromString(message.getJMSCorrelationID());

			// preparing class bytecode.
			byte[] bytecode = null;

			try
			{
				String classPath = className.replace('.', '/') + ".class";//TODO: use utils.
				InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(classPath);

				// If the class cannot be found, attempt to find a resource with the requested name
				if (is == null) {
					try {
						ClassPathResource resource = new ClassPathResource(className);
						if (resource.exists()) {
							is = resource.getInputStream();
						}
					} catch (IOException e) {
						// Ignore
					}
				}
				
				if(is == null)
					log.severe(String.format("Requested class %s was not found.", className));
				else
					bytecode = RemoteClassLoaderUtils.getByteArray(is);

			} catch (Exception ex)
			{
				log.severe("Failed to provide required class "+className+": "+ ex.toString());
				ex.printStackTrace();
			}
			finally
			{
				mq.respond(new ClassResponse(bytecode), destination, correlationId);
			}

		} catch (JMSException e)
		{
			log.severe("Failed to process message ("+ className +"): "+ e.toString());
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws IOException 
	{
		if(this.mq != null)
		{
			mq.close();
			mq = null;
		}
	}
}
