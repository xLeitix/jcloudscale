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
package at.ac.tuwien.infosys.jcloudscale.sample.service.openstack;

import java.io.File;
import java.util.logging.Logger;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Configuration that defines where message queues are and how application should communicate with them.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class OpenstackConfiguration 
{
	private static final String CONFIG_FILE = "os.properties";
	
	private String mqAddress = "localhost:60000";
	
	private String clientQueue = "JST_CLIENT";
	private String workerTopic = "JST_WORKERS";
	private String workerDiscoveryTopic = "JST_WORKERS_DISCOVERY";
	private int discoveryInterval = 300;//in ms
	private String jmsSelector = "Host_ID";
	
	private String couchdbHost = "127.0.0.1";
	private int couchdbPort = 5984;
	private String couchDatabase = "testresults";
	
	private static class SingletonHolder
	{
		public static final OpenstackConfiguration instance = loadConfiguration();
	}
	
	private static OpenstackConfiguration loadConfiguration()
	{
		Logger log = Logger.getLogger(OpenstackConfiguration.class.getName());
		
		File cfgFile = new File(CONFIG_FILE);
		if(!cfgFile.exists())
		{
			log.info("CONFIG: File "+cfgFile.getAbsolutePath() +" was not found, using defaults.");
			
			OpenstackConfiguration config = new OpenstackConfiguration();
			//config.save(cfgFile);
			return config;
		}
		
		log.info("CONFIG: Loading configuration from "+CONFIG_FILE);
		
		try
		{
			return JAXB.unmarshal(cfgFile, OpenstackConfiguration.class);
		}
		catch(Exception ex)
		{
			log.severe("Failed to read properties from config file: "+ex);
			ex.printStackTrace();
			return new OpenstackConfiguration();
		}
	}
	
	protected static OpenstackConfiguration getInstance()
	{
		return SingletonHolder.instance;
	}
	
	public void save(File file)
	{
		try
		{
			JAXB.marshal(this, file);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static String getMessageQueueAddress()
	{
		return getInstance().mqAddress;
	}

	public static String getClientQueue() {
		return getInstance().clientQueue;
	}

	public static String getWorkerTopic() {
		return getInstance().workerTopic;
	}
	

	public static String getWorkerDiscoveryTopic() {
		return getInstance().workerDiscoveryTopic;
	}

	public static int getDisocverInterval() {
		return getInstance().discoveryInterval;
	}

	public static String getJmsSelector() {
		return getInstance().jmsSelector;
	}

	public static String getCouchdbHostname() {
		return getInstance().couchdbHost;
	}

	public static int getCouchdbPort() 
	{
		return getInstance().couchdbPort;
	}

	public static String getCouchdbDatabase() 
	{
		return getInstance().couchDatabase;
	}
}
