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

import java.io.PrintStream;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.RedirectedOutputObject;

public class SysoutputReceiver {
	
	private IMQWrapper mq; 
	private String messagePattern = "--%s-- ";
	private boolean err_initialized = false;
	private boolean out_initialized = false;
	
	public SysoutputReceiver(){
	
		try {
			mq = JCloudScaleConfiguration.createMQWrapper();
			mq.createQueueConsumer(JCloudScaleConfiguration.getConfiguration()
					.server().logging().getOutputRedirectQueue());
			mq.registerListener(new ServerOutputListener());
			
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
		
	}
	
	private synchronized void writeToStream(String string, PrintStream stream) {
		stream.print(string);
	}
	
	public void close() {
		try 
		{
			mq.disconnect();
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
	
	private class ServerOutputListener implements MessageListener {

		@Override
		public void onMessage(Message message) {
			
			if(message instanceof ObjectMessage) {
				
				ObjectMessage m = (ObjectMessage) message;
				
				try {
					
					if(!(m.getObject() instanceof RedirectedOutputObject))
						return;
					
//					// throw away old messages
//					if(m.getJMSTimestamp() < System.currentTimeMillis() - OUTPUT_TIMEOUT)
//						return;
					
					RedirectedOutputObject output = (RedirectedOutputObject) m.getObject();

					// if we receive the first message, we need to explicitly print a prefix
					// (for each subsequent message we just replace each newline character)
					if(output.isErr()) {
						if(!err_initialized){
							writeToStream(getPrefix(output.getId(), output.getSource()), System.err);
							err_initialized = true;
						} 
					} else {
						if(!out_initialized){
							writeToStream(getPrefix(output.getId(), output.getSource()), System.out);
							out_initialized = true;
						} 
					}
							
					// here we mangle the received string a bit to generate the correct text to write
					// to the stream. most importantly, we add the source as 'meta info' in the style
					// --IP-- text
					String text = output.getText();
					String amendedString = text.replaceAll(output.getLineSeparator(),
							output.getLineSeparator()+getPrefix(output.getId(), output.getSource()));
					// amendedString = convertLineEndings(output.getLineSeparator(), amendedString);
					
					// write to the correct stream using the synchronized method from the parent class
					// (remember, we are still in an async message handler) 
					if(output.isErr())
						writeToStream(amendedString, System.err);
					else
						writeToStream(amendedString, System.out);
					
				} catch (JMSException e) {
					e.printStackTrace();
				}
				
			}
			
		}
		
		private String getPrefix(UUID id, String source) {
			return String.format(messagePattern, source);
		}
		
//		private String convertLineEndings(String origSeparator, String string) {
//			return string.replaceAll(origSeparator, System.lineSeparator());
//		}
	}
}
