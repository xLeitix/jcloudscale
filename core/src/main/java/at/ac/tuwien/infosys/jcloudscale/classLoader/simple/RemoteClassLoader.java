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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.classLoader.simple.dto.ClassRequest;
import at.ac.tuwien.infosys.jcloudscale.classLoader.simple.dto.ClassResponse;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.TimeoutException;

public class RemoteClassLoader extends ClassLoader implements Closeable
{
	private Logger log;
	private IMQWrapper mq;
	private UUID id = UUID.randomUUID();
	boolean localFirst = true; 

	RemoteClassLoader(SimpleRemoteClassLoaderConfiguration configuration)
	{
		this.log = JCloudScaleConfiguration.getLogger(this);
		localFirst = configuration.localFirst;
		
		try
		{
			this.mq = JCloudScaleConfiguration.createMQWrapper();
			this.mq.createTopicConsumer(configuration.responseQueue, "JMSCorrelationID = '"+id.toString()+"'");
			this.mq.createQueueProducer(configuration.requestQueue);
		} 
		catch (JMSException | NamingException e) 
		{
			log.severe("Failed to connect to message queue from RemoteClassLoader:" + e.toString());
			throw new JCloudScaleException(e, "Failed to connect to message queue from RemoteClassLoader");
		}
	}
	
	@Override
	public void close()
	{
		mq.close();
	}

	@Override
	protected synchronized Class<?> findClass(String name) throws ClassNotFoundException
	{
		Class<?> clazz = null;

		byte data[] = loadClassData(name);

		if (data != null)
			clazz = defineClass(name, data, 0, data.length);

		if (clazz == null)
			throw new ClassNotFoundException(name);

		return clazz;
	}

	private byte[] loadClassData(String className)
	{
		long start = System.nanoTime();
		try
		{
//			TextMessage requestMessage = session.createTextMessage(className);
//			
//			requestMessage.setJMSReplyTo(classReceiveQueue);
//			classRequester.send(requestMessage);
//			ObjectMessage msg = (ObjectMessage) classFetcher.receive();
//
//			return (byte[]) msg.getObject();
			return ((ClassResponse)mq.requestResponse(new ClassRequest(className), id)).bytecode;

		} catch (JMSException | TimeoutException e)
		{
			log.severe("Failed to load class Data: " + e.toString());
			e.printStackTrace();
			return null;
		} 
		finally
		{
			String msg = "REMOTE CLASS LOADER: loading of " + className + " took " + 
										(System.nanoTime() - start) / 1000000 + "ms.";
			log.fine(msg);
		}

	}

	@Override
	public InputStream getResourceAsStream(String name) 
	{
		@SuppressWarnings("resource")
		InputStream stream = localFirst ? super.getResourceAsStream(name) : getRemoteResourceAsStream(name);
		return stream != null ? stream : (localFirst ? getRemoteResourceAsStream(name) : super.getResourceAsStream(name));
	}

	/**
	 * Returns an input stream for reading the specified remote resource.<br/>
	 *
	 * @param name the resource name
	 * @return an input stream for reading the resource, or {@code null} if the resource could not be found
	 */
	protected InputStream getRemoteResourceAsStream(String name) {
		try {
			ClassRequest request = new ClassRequest(name);
			ClassResponse response = (ClassResponse) mq.requestResponse(request, id);

			if (response.bytecode != null) {
				return new ByteArrayInputStream(response.bytecode);
			}
		} catch (JMSException | TimeoutException e) {
			throw new JCloudScaleException(e);
		}
		return null;
	}
}
