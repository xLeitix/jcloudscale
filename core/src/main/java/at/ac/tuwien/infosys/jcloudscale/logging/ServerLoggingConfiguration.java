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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.server.ServerLogFormatter;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class ServerLoggingConfiguration extends ClientLoggingConfiguration 
{
	private static final long serialVersionUID = 1L;

	private String loggingQueueName = "CS_ServerLogging";
	
	private boolean redirectStdIoToClientConsole = true;
	private boolean redirectStdErrToClientConsole = true;
	private String outputRedirectQueue = "CS_Systemoutput";
	
	
	public ServerLoggingConfiguration()
	{
		parentLoggerName = "at.ac.tuwien.infosys.jcloudscale.server";
		setFormatter(ServerLogFormatter.class);
	}

	public boolean redirectStdIoToClientConsole() {
		return redirectStdIoToClientConsole;
	}
	
	public boolean redirectStdErrToClientConsole() {
		return redirectStdErrToClientConsole;
	}

	public String getLoggingQueueName() {
		return loggingQueueName;
	}
	
	public String getOutputRedirectQueue()
	{
		return outputRedirectQueue;
	}

	public void setLoggingQueueName(String loggingQueueName) {
		this.loggingQueueName = loggingQueueName;
	}
	public void setRedirectStdIoToClientConsole(boolean redirectStdIoToClientConsole) {
		this.redirectStdIoToClientConsole = redirectStdIoToClientConsole;
	}
	public void setRedirectStdErrToClientConsole(
			boolean redirectStdErrToClientConsole) {
		this.redirectStdErrToClientConsole = redirectStdErrToClientConsole;
	}
	public void setOutputRedirectQueue(String outputRedirectQueue) {
		this.outputRedirectQueue = outputRedirectQueue;
	}
	
	/**
	 * Checks if the registered handlers contain ConsoleHandler and in case they do, recreates it and replaces for parent logger.
	 * This method is necessary as ConsoleHandler captures output stream, what makes logging redirection problematic.
	 */
	public synchronized void recreateConsoleHandler() 
	{
		//instead of all that, now it is possible to just recreate parent logger. TODO: needs to be checked.
		this.parentLogger = null;
		getParentLogger();
		
//		// searching for console handler.
//		Handler consoleHandler = null;
//		for(Handler handler : getHandlers())
//			if(handler instanceof ConsoleHandler)
//			{
//				consoleHandler = handler;
//				break;
//			}
//		
//		if(consoleHandler == null)
//			return;//no console handler found.
//		
//		// creating new console handler
//		Handler newConsoleHandler =new ConsoleHandler();
//		newConsoleHandler.setLevel(Level.ALL);
//		
//		Formatter formatter = getFormatter();
//		
//		if(formatter != null)
//			newConsoleHandler.setFormatter(formatter);
//		
//		// fixing parent logger.
//		if(this.parentLogger == null)
//			return;//parent logger was not created yet. it will be created correctly when needed.
//		
//		this.parentLogger.removeHandler(consoleHandler);
//		this.parentLogger.addHandler(newConsoleHandler);
	}
}
