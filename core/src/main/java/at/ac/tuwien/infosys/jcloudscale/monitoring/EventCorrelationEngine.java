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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.Event;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class EventCorrelationEngine {
	
	private static EventCorrelationEngine instance;
	
	private IMetricsDatabase db;
	private EPServiceProvider esper;
	private HashMap<String, EPStatement> listeners;
	private JMSToEsperBridge bridge;
	private Logger log;
	
	private EventCorrelationEngine() 
	{
		// We just have to ensure that JCloudScaleClient is started before doing anything with JCloudScale. 
		// Required to ensure that environment (mq server?) is operating.
		JCloudScaleClient.getClient();
		
		log = JCloudScaleConfiguration.getLogger(this);
		
		this.db = MetricsDatabaseFactory.getDatabaseImpl();
		Configuration configuration = getEsperConfig();
		this.esper = EPServiceProviderManager.getDefaultProvider(configuration);
		this.listeners = new HashMap<String, EPStatement>();
		this.bridge = new JMSToEsperBridge(this.esper);
		this.bridge.initializeBridge();
	}
	
	public synchronized static EventCorrelationEngine getInstance() 
	{
		if(instance == null)
			instance = new EventCorrelationEngine();
		
		return instance;
	}
	
	public synchronized static void closeInstance() 
	{
		if(instance == null)
			return;
		
		for(String key : new HashSet<>(instance.listeners.keySet()))
			instance.unregisterMetric(key);
		try {
			instance.db.close();
		} catch (IOException e) {
			e.printStackTrace();
			instance.log.severe(e.getMessage());
		}
		instance.bridge.closeBridge();
		
		instance = null;
	}
	
	public void registerMetric(MonitoringMetric metric) {
		
		EPStatement statement = esper.getEPAdministrator().createEPL(metric.getEpl());
		statement.addListener(new CEPEventListener(metric));
		listeners.put(metric.getName(), statement);
		
	}
	
	public void unregisterMetric(String name) {
		EPStatement statement = listeners.get(name);
		
		if(statement != null)
		{
			statement.removeAllListeners();
			listeners.remove(name);
		}
	}
	
	public IMetricsDatabase getMetricsDatabase() {
		return db;
	}
	
	public void addCustomMonitoringEvent(Class<? extends Event> eventType) {
		esper.getEPAdministrator().getConfiguration().addEventType(eventType);
	}
	
	private Configuration getEsperConfig() 
	{
		Configuration configuration = new Configuration();
		
		// appears that sometimes it's better to use classloader that loaded this class instead of system one.
		ClassLoader classLoader = this.getClass().getClassLoader();//ClassLoader.getSystemClassLoader();
		
		for(String eventClassName : JCloudScaleConfiguration.getConfiguration().common().monitoring().getMonitoredEvents())
		{
			try
			{
				Class<?> clazz = classLoader.loadClass(eventClassName);
				configuration.addEventType(clazz.getSimpleName(), clazz);
//					System.out.println("   MONITORING: REGISTERED "+clazz.getName());
			}
			catch(ClassNotFoundException ex)
			{
				log.severe("Failed to load event "+eventClassName);
			}
		}
		
		return configuration;
	}
	
	private class CEPEventListener implements UpdateListener {
		
		private MonitoringMetric metric;
		
		public CEPEventListener(MonitoringMetric metric) {
			this.metric = metric;
		}
		
		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents) {
			for(EventBean event : newEvents) {
				Object val;
				try {
					val = metric.getValueFromBean(event);
				} catch (Exception e) {
					throw new JCloudScaleException(e);
				}
				Object cast = castToType(val, metric.getType());
				
				if(val != null) {
					db.addValue(metric.getName(), cast);
					log.finer("CEP engine received metric value "+cast+" for metric "+metric.getName());
				} else {
					log.info("CEP engine received null value for metric "+metric.getName());
				}
			}
		}

		private Object castToType(Object val, Class<?> type) {
			// TODO
			return val;
		}
		
	}
	
}
