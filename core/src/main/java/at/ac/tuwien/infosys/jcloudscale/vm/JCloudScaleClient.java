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
package at.ac.tuwien.infosys.jcloudscale.vm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleConfigurationProvider;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfigurationBuilder;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.logging.LogReceiver;
import at.ac.tuwien.infosys.jcloudscale.logging.SysoutputReceiver;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.management.JCloudScaleReferenceManager;
import at.ac.tuwien.infosys.jcloudscale.messaging.MQWrapper;
import at.ac.tuwien.infosys.jcloudscale.monitoring.EventCorrelationEngine;
import at.ac.tuwien.infosys.jcloudscale.server.messaging.MonitoringMQHelper;

/**
 * The main JCloudScale class in the client-side code. 
 */
public class JCloudScaleClient implements Closeable 
{
	/**
	 * In case configuration was not specified explicitly, JCloudScale will check this property
	 * in System Properties to load configuration. If it won't be specified as well, default configuration will be used.
	 */
	public static final String JCLOUDSCALE_CONFIGURATION_PROPERTY = "jcloudscale.configuration";
	
	//------------------------------------------------------------------
	private static JCloudScaleClient instance = null;
	private static volatile JCloudScaleConfiguration configuration = null;
	
	private LogReceiver serverLogReceiver;
	private SysoutputReceiver serverOutputReceiver;
	private Closeable classProvider;
	private Closeable riakPublisher;
	private Closeable mqPublisher;
	private AutoCloseable mqServer;
	
	private Map<UUID, VirtualHostProxy> hostProxies = new HashMap<>();
	private Logger log;
	
	private JCloudScaleClient()
	{
		this.log = JCloudScaleConfiguration.getLogger(this);

		// Ensuring that Message Queue Is running
		if(getConfiguration().common().communication().getStartMessageQueueServerAutomatically())
		{
			try 
			{
				mqServer = getConfiguration().server()
							.cloudPlatform().ensureCommunicationServerRunning();
			} catch (Exception ex) 
			{
				log.warning("Failed to ensure that Message Queue is running: "+ex);
			}
		}
		
		this.serverLogReceiver = new LogReceiver();
		this.serverOutputReceiver = new SysoutputReceiver();
		this.classProvider = getConfiguration().common()
							.classLoader()
							.createClassProvider();
		
		// Publisher for MQ Server
		if(getConfiguration().common().communication().startMulticastPublisher())
			try 
			{
				mqPublisher = getConfiguration().common().communication().createServerPublisher();
			}
			catch (IOException e) 
			{
				log.warning("Failed to start Message Queue Multicast Publisher. "+ e);
			}
	}
	
	/**
	 * Gets an instance of the JCloudScaleClient that manages all JCloudScale code on client.
	 * @return A singleton instance of the <b>JCloudScaleClient</b> class.
	 */
	public static synchronized JCloudScaleClient getClient()
	{
		if(instance == null)
			instance = new JCloudScaleClient();
		
		return instance;
	}
	
	/**
	 * Closes and cleansup all resources managed by the JCloudScale on the client-side.
	 */
	public static synchronized void closeClient() throws IOException
	{
		//TODO: consider if we need it here.
		CloudManager.closeCloudManager();
		MonitoringMQHelper.closeInstance();
		EventCorrelationEngine.closeInstance();
		JCloudScaleReferenceManager.closeInstance();
		
		if(instance == null)
			return;
		
		instance.close();
		instance = null;
	}
	
	//-----------------------------------------------------------------
	
	/**
	 * Gets the client-instance of the JCloudScale Configuration.
	 * @return The instance of the current JCloudScale configuration.
	 */
	public static JCloudScaleConfiguration getConfiguration()
	{
		if(configuration == null)
			ensureConfigurationSpecified();
		
		return configuration;
	}
	
	/**
	 * Sets the JCloudScale configuration to be used during the application runtime.
	 * <b>WARNING:</b> to ensure that the configuration is same for all components, 
	 * configuration has to be specified prior to any interaction with JCloudScale.  
	 * @param cfg An instance of the <b>JCloudScaleConfiguration</b> class that declares 
	 * the behavior and preferences of the JCloudScale.
	 */
	public static synchronized void setConfiguration(JCloudScaleConfiguration cfg)
	{
		if(configuration != null && instance != null)
		{	//we already have configuration and running instance. The old one was used, let's inform user. 
			JCloudScaleConfiguration.getLogger(cfg, JCloudScaleClient.class).warning("JCloudScale configuration redefinition: Replacing configuration instance. Some components might be still using the previous version of the configuration.");
		}
		
		configuration = cfg;
		JCloudScaleConfiguration.setServerContext(false);
	}
	
	//-------------------------------------------------------------------
	
	public synchronized void addProxy(UUID id, VirtualHostProxy proxy)
	{
		//TODO: this method holds references to HostProxy to be able to close them all on shutdown. Like a safe-plan. 
		// Consider if this is actually required, as they are closed somewhere else. 
		
		if(hostProxies.containsKey(id))
			hostProxies.get(id).close();
		
		hostProxies.put(id, proxy);
	}
	
	@Override
	public void close() throws IOException
	{
		this.serverLogReceiver.close();
		this.serverOutputReceiver.close();
		
		if(classProvider != null)
			this.classProvider.close();
		
		if(this.riakPublisher != null)
			this.riakPublisher.close();
		
		if(this.mqPublisher != null)
			this.mqPublisher.close();
		
		//closing all connections we have. Just to avoid bugs with not closed connection.
		if(MQWrapper.shutdownAllConnections())
			log.warning("JCloudScaleClient found that there are some MQ connections not closed. Consider fixing this.");
		
		if(mqServer != null)
		{
			try
			{
				mqServer.close();
			}
			catch(Exception ex)
			{
				log.warning("Failed to close Message Queue Server: "+ex);
			}
		}

		//just the cleanup, in case we forgot to close something.
		for(VirtualHostProxy proxy : hostProxies.values())
			proxy.close();
		
	}
	
	//-------------------------LOADING CONFIGURATION FROM PROPERTIES-----------------------
	
	private static synchronized void ensureConfigurationSpecified()
	{
		if(configuration != null)
			return;

		String configurationLocation = System.getProperty(JCLOUDSCALE_CONFIGURATION_PROPERTY);
		if(configurationLocation != null)
		{
			//checking if this is file
			File configFile = new File(configurationLocation);
			
			if(configFile.exists() && configFile.isFile())
				setConfiguration(loadConfigurationFromFile(configFile));
			else
			{	// checking if this is the class.
				try
				{
					Class<?> clazz = Class.forName(configurationLocation);
					setConfiguration(loadConfigurationFromClass(clazz));
				}
				catch(ClassNotFoundException ex)
				{// no, we could not detect what is this. We have to fail.
					throw new JCloudScaleException(
							String.format("Failed to load configuration from %s: " +
							"neither file \"%s\" nor class \"%s\" exist.",
							configurationLocation, configFile.getAbsolutePath(), configurationLocation));
				}
			}
			JCloudScaleConfiguration.getLogger(configuration, JCloudScaleClient.class)
						.info("JCloudScale successfully loaded configuration from "+configurationLocation);
		}
		else
		{
			JCloudScaleClient.setConfiguration(new JCloudScaleConfigurationBuilder().build());
			JCloudScaleConfiguration.getLogger(configuration, JCloudScaleClient.class)
					.info("No configuration specified; JCloudScale is using default configuration.");
		}
	}
	
	private static JCloudScaleConfiguration loadConfigurationFromClass(Class<?> clazz) 
	{
		final Class<? extends Annotation> annotationClass = JCloudScaleConfigurationProvider.class;
		try
		{
			Method[] methods = clazz.getDeclaredMethods();
			for(Method method : methods)
			{
				if(!method.isAnnotationPresent(annotationClass))
					continue;
				
				// checking return type
				if(!method.getReturnType().equals(JCloudScaleConfiguration.class))
					throw new JCloudScaleException(String.format(
							"The method %s of class %s is annotated with %s " +
							"annotation, but return type is %s instead of %s",
							method.getName(), clazz.getName(), 
							annotationClass.getName(),
							method.getReturnType().getName(), 
							JCloudScaleConfiguration.class.getName()));

				// checking if it is static
				if(!Modifier.isStatic(method.getModifiers()))
					throw new JCloudScaleException(String.format(
							"The method %s of class %s is annotated with %s " +
							"annotation, but is not static.",
							method.getName(), clazz.getName(),
							annotationClass.getName()));
				
				//checking parameters
				if(method.getParameterTypes().length > 0)
					throw new JCloudScaleException(String.format(
							"The method %s of class %s is annotated with %s " +
							"annotation, but has input parameters. Cannot get configuration from method with parameters.",
							method.getName(), clazz.getName(),
							annotationClass.getName()));
				
				if(!method.isAccessible())
					method.setAccessible(true);
				
				try
				{
					return (JCloudScaleConfiguration)method.invoke(null);
				}
				catch(Exception ex)
				{
					throw new JCloudScaleException(ex, String.format("Failed to invoke method %s from class %s to retrieve configuration.", method.getName(), clazz.getName()));
				}
			}
			
			throw new JCloudScaleException(String.format("The class %s is specified in %s property, " +
					"but has no static methods annotated with %s",
					clazz.getName(), JCLOUDSCALE_CONFIGURATION_PROPERTY,
					annotationClass.getName()));
		}
		catch(Exception ex)
		{
			throw new JCloudScaleException(ex, "Failed to load configuration from the class "+clazz.getName());
		}
	}

	private static JCloudScaleConfiguration loadConfigurationFromFile(File configFile) 
	{
		try
		{
			return JCloudScaleConfiguration.load(configFile);
		}
		catch(Exception ex)
		{
			throw new JCloudScaleException(ex, "Failed to load configuration from the file "+configFile.getAbsolutePath());
		}
	}
}
