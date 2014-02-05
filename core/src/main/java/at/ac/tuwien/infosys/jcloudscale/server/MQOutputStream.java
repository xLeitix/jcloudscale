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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.RedirectedOutputObject;

public class MQOutputStream extends ByteArrayOutputStream
{
	private IMQWrapper mq; 
	private boolean isErr;
	private String sourceAddress;
	private UUID id;
	
	public MQOutputStream(boolean isErr, String sourceAddress, UUID id)
			throws NamingException, JMSException, IOException {
		
		this.isErr = isErr;
		this.sourceAddress = sourceAddress;
		this.id = id;
		
		mq = JCloudScaleConfiguration.createMQWrapper(DeliveryMode.NON_PERSISTENT);
		mq.createQueueProducer(JCloudScaleConfiguration.getConfiguration().server().logging().getOutputRedirectQueue());//, DeliveryMode.PERSISTENT,10000);
	}
	
	@Override
	public void flush() 
	{
		//TODO: if some message would come in between next statements, we're likely to lose it.
		String logString = this.toString();
		this.reset();
		
		RedirectedOutputObject obj = new RedirectedOutputObject();
		obj.setText(logString);
		obj.setErr(isErr);
		obj.setSource(sourceAddress);
		obj.setId(id);
		obj.setLineSeparator(System.lineSeparator());
		
		try 
		{
			mq.oneway(obj);
		} 
		catch (JMSException e) 
		{
			// TODO: can't print exception here, because it would likely lead to infinite loop
		}
	}
	
	@Override
	public void close() 
	{
		mq.close();
		mq = null;
	}
}
