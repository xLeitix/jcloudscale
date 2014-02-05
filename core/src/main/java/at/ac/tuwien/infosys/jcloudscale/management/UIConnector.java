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
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.CreateServerRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.CreateServerResponse;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.DestroyServerRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.GetMetricRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.GetMetricResponse;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ListCloudObjectsRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ListCloudObjectsResponse;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ListServersRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ListServersResponse;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ServerDetailsRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ServerDetailsResponse;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.dto.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.dto.Server;
import at.ac.tuwien.infosys.jcloudscale.monitoring.EventCorrelationEngine;
import at.ac.tuwien.infosys.jcloudscale.monitoring.IMetricsDatabase;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHostPool;

public class UIConnector implements Closeable 
{
	private Logger log;
	private IMQWrapper mq;
	private CloudManager cm;
	private CloudscaleUIConfiguration cfg;
	
	public UIConnector(CloudManager cloudManager, CloudscaleUIConfiguration cfg)
			throws NamingException, JMSException 
	{
		this.cm = cloudManager;
		this.cfg = cfg;
		
		this.log = JCloudScaleConfiguration.getLogger(this);
		
		if(cfg.isEnabled())
			start();
	}
	
	private void start() throws NamingException, JMSException 
	{
		mq = JCloudScaleConfiguration.createMQWrapper();
		mq.createQueueConsumer(cfg.getMessageQueue());
		mq.registerListener(new StatsRequestListener());
		
		log.info("Started to listen for UI requests");
	}
	
	@Override
	public void close() 
	{
		if(mq != null)
		{
			mq.close();
			mq = null;
			
			log.info("Stopped to listen for UI requests");
		}
	}
	
	private class StatsRequestListener implements MessageListener {

		@Override
		public void onMessage(Message message) {
			
			try {
			
				if(!(message instanceof ObjectMessage)) {
					log.severe("Received invalid message in UI connector: "+message.toString());
					return;
				}
				
				ObjectMessage msg = (ObjectMessage)message;
				
				if(!(msg.getObject() instanceof MessageObject)) {
					log.severe("Received invalid object in UI connector: "+msg.getObject().toString());
					return;
				}
				
				MessageObject obj = (MessageObject)msg.getObject();
				
				MessageObject response = null;
				if(obj instanceof ListCloudObjectsRequest) {
					log.info("Handling ListCloudObjectsRequest");
					response = handle((ListCloudObjectsRequest)obj);
					log.info("Finished Handling ListCloudObjectsRequest");
				}
				
				if(obj instanceof ListServersRequest) {
					log.info("Handling ListServersRequest");
					response = handle((ListServersRequest)obj);
					log.info("Finished Handling ListServersRequest");
				}
				
				if(obj instanceof ServerDetailsRequest) {
					log.info("Handling ServerDetailsRequest");
					response = handle((ServerDetailsRequest)obj);
					log.info("Finished Handling ServerDetailsRequest");
				}
				
				if(obj instanceof CreateServerRequest) {
					log.info("Handling CreateServerRequest");
					response = handle((CreateServerRequest)obj);
					log.info("Finished Handling CreateServerRequest");
				}
				
				if(obj instanceof DestroyServerRequest) {
					log.info("Handling DestroyServerRequest");
					response = handle((DestroyServerRequest)obj);
					log.info("Finished Handling DestroyServerRequest");
				}
				
				if(obj instanceof GetMetricRequest) {
					log.info("Handling GetMetricRequest");
					response = handle((GetMetricRequest)obj);
					log.info("Finished Handling GetMetricRequest");
				}
				
				if(response == null) {
					return;
				}
				
				if(msg.getJMSReplyTo() != null) {
					mq.respond(response, msg.getJMSReplyTo(), UUID.fromString(msg.getJMSCorrelationID()));
				} else {
					if(response != null)
						log.severe("Produced result "+response.toString()+", but don't know where to send it");
				}
			} catch(Exception e) {
				
				log.severe("Error handling request in UI connector: "+e.getMessage());
				e.printStackTrace();
				throw new JCloudScaleException(e);
				
			}
			
		}
		
		private ListCloudObjectsResponse handle(ListCloudObjectsRequest msg) throws ClassNotFoundException {
			
			ListCloudObjectsResponse response = new ListCloudObjectsResponse();
			Class<?> filter = null;
			if(msg.getCoType() != null)
				filter = Class.forName(msg.getCoType());
			Set<UUID> objectIds = cm.getCloudObjects();
			if(objectIds!= null && objectIds.size() > 0) {
				List<CloudObject> objects = new LinkedList<CloudObject>();
				for(UUID objectId : objectIds) {
					Class<?> type = cm.getHost(objectId).getCloudObjectType(objectId);
					if(filter == null || filter.getName().equals(type.getName())) {
						CloudObject object = new CloudObject();
						IVirtualHost host = cm.getHost(objectId);
						
						object.setHost(cm.getHost(objectId).getId().toString());
						object.setId(objectId.toString());
						object.setType(type.getCanonicalName());
						object.setState(host.getCloudObjectState(objectId).name());
						object.setExecutingMethods(host.getExecutingMethods(objectId));
						
						objects.add(object);
						
					}
				}
				response.setObjects(objects);
			}
			return response;
			
		}
		
		private ServerDetailsResponse handle(ServerDetailsRequest msg) {
			
			ServerDetailsResponse response = new ServerDetailsResponse();
			UUID serverId = msg.getServerId();
			Collection<IVirtualHost> hosts = cm.getHosts();
			for(IVirtualHost host : hosts) {
				if(host.getId().equals(serverId)) {
					Server server = new Server();
					server.setIpAdd(host.getIpAddress());
					if(host.getId() != null)
						server.setServerId(host.getId().toString());
					else
						server.setServerId(null);
					server.setStatic(host.isStaticHost());
					response.setServer(server);
					response.setCurrLoad(host.getCurrentCPULoad());
					response.setCurrRam(host.getCurrentRAMUsage());
					List<CloudObject> cloudObjects = new LinkedList<CloudObject>();
					for(UUID objectId : host.getManagedObjectIds()) {
						CloudObject object = new CloudObject();
						object.setHost(host.getId().toString());
						object.setId(objectId.toString());
						object.setType(host.getCloudObjectType(objectId).getCanonicalName());
						object.setState(host.getCloudObjectState(objectId).name());
						object.setExecutingMethods(host.getExecutingMethods(objectId));
						cloudObjects.add(object);
					}
					response.setObjects(cloudObjects);
				}
					
			}
			return response;
			
		}
		
		private ListServersResponse handle(ListServersRequest msg) {
			return listServers();
		}

		private CreateServerResponse handle(CreateServerRequest msg) {
			
			IVirtualHostPool pool = cm.getHostPool();
			IVirtualHost host = (IVirtualHost)pool.startNewHost();
			CreateServerResponse response = new CreateServerResponse();
			Server server = new Server();
			server.setIpAdd(host.getIpAddress());
			server.setServerId(host.getId().toString());
			server.setStatic(host.isStaticHost());
			response.setServer(server);
			return response;
		}
		
		private ListServersResponse handle(DestroyServerRequest msg) {
			
			UUID serverId = msg.getServerId();
			cm.getHostPool().shutdownHost(serverId);
			return listServers();
			
		}
		
		private GetMetricResponse handle(GetMetricRequest msg) {
			
			IMetricsDatabase db = EventCorrelationEngine.getInstance().getMetricsDatabase();
			Object val = db.getLastValue(msg.getMetricName());
			GetMetricResponse response = new GetMetricResponse();
			if(val != null && val instanceof Serializable)
				response.setValue((Serializable)val);
			response.setMetricName(msg.getMetricName());
			return response;
			
		}
		
		private ListServersResponse listServers() {
			Collection<IVirtualHost> hosts = cm.getHosts();
			ListServersResponse response = new ListServersResponse();
			List<Server> servers = new LinkedList<Server>();
			response.setServers(servers);
			if(hosts.size() > 0) {
				for(IVirtualHost host : hosts) {
					Server server = new Server();
					server.setIpAdd(host.getIpAddress());
					if(host.getId() != null)
						server.setServerId(host.getId().toString());
					else
						server.setServerId(null);
					server.setStatic(host.isStaticHost());
					servers.add(server);
					
				}
			}
			return response;
		}
			
	}
	
}
