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
package at.ac.tuwien.infosys.jcloudscale.messaging;

import java.io.Closeable;
import java.util.UUID;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;
import at.ac.tuwien.infosys.jcloudscale.utility.CancellationToken;

public interface IMQWrapper extends Closeable
{
	public void disconnect() throws JMSException, NamingException;
	
	@Override
	public void close();
	
	public boolean configurationEquals(MessageQueueConfiguration newConfiguration);
	
	public void createQueueProducer(String destName) throws NamingException, JMSException;
	
	public void createTopicProducer(String destName) throws NamingException, JMSException;
	
	public void createQueueConsumer(String destName) throws NamingException, JMSException;
	
	public void createTopicConsumer(String destName, String messageSelector) throws NamingException, JMSException;
	
	public void createTopicConsumer(String destName) throws NamingException, JMSException;
	
	public void registerListener(MessageListener listener) throws JMSException;
	
	public MessageObject requestResponseToCSHost(MessageObject obj, UUID correlationId, UUID hostId) throws JMSException, TimeoutException;
	
	public MessageObject requestResponseToCSHost(MessageObject obj, UUID correlationId, UUID hostId, CancellationToken cancelToken) throws JMSException, TimeoutException;
	
	public MessageObject requestResponse(MessageObject obj, UUID correlationId) throws JMSException, TimeoutException;
	
	public MessageObject requestResponse(MessageObject obj, UUID correlationId, CancellationToken cancelToken) throws JMSException, TimeoutException;
	
	public void oneway(MessageObject obj) throws JMSException;
	
	public void oneway(MessageObject obj, UUID correlationId) throws JMSException;
	
	public void onewayToCSHost(MessageObject obj, UUID correlationId, UUID hostId) throws JMSException;
	
	public void respond(MessageObject obj, Destination dest, UUID correlationId) throws JMSException;
	
	public void respond(MessageObject obj, Destination dest) throws JMSException;
	
	
}
