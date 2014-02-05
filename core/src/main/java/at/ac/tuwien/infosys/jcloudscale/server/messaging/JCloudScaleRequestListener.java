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
package at.ac.tuwien.infosys.jcloudscale.server.messaging;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.IConfigurationChangedListener;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.CreateObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.CreateReturnObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.DeleteObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.DeleteReturnObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.GetFieldValueObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.GetFieldValueResponseObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.KeepaliveObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.RequestObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ReturnObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.SetFieldValueObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.SetFieldValueReturnObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ShutdownObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.StartInvokationObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.StartInvokationReturnObject;
import at.ac.tuwien.infosys.jcloudscale.migration.IMigrationEnabledJCloudScaleHost;
import at.ac.tuwien.infosys.jcloudscale.migration.objects.MigratedCODeploymentObject;
import at.ac.tuwien.infosys.jcloudscale.migration.objects.MigratedCODeploymentReturnObject;
import at.ac.tuwien.infosys.jcloudscale.migration.objects.MigratedCORemoveObject;
import at.ac.tuwien.infosys.jcloudscale.migration.objects.MigratedCORemoveReturnObject;
import at.ac.tuwien.infosys.jcloudscale.migration.objects.SerializedCOStateObject;
import at.ac.tuwien.infosys.jcloudscale.migration.objects.SerializedCOStateReturnObject;
import at.ac.tuwien.infosys.jcloudscale.server.AbstractJCloudScaleServerRunner;
import at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServer;
import at.ac.tuwien.infosys.jcloudscale.server.ServerConfiguration;
 
public class JCloudScaleRequestListener implements MessageListener, Closeable, IConfigurationChangedListener {
	
	private IMigrationEnabledJCloudScaleHost server;

	protected Logger log;
	protected ExecutorService threadpool;
	protected IMQWrapper mq; 
	protected UUID serverId;
	
	public JCloudScaleRequestListener(IMigrationEnabledJCloudScaleHost server, UUID serverId) throws NamingException, JMSException {
		this.server = server;
		this.serverId = serverId;
		this.log = JCloudScaleConfiguration.getLogger(this);
		
		initMessageQueue(JCloudScaleConfiguration.getConfiguration());
		threadpool = Executors.newCachedThreadPool(); 
		
		// JCloudScaleServerRunner.getInstance().registerConfigurationChangeListner(this);
		AbstractJCloudScaleServerRunner.getInstance().registerConfigurationChangeListner(this);
	}
	
	private void initMessageQueue(JCloudScaleConfiguration configuration) throws NamingException, JMSException 
	{
		IMQWrapper wrapper = JCloudScaleConfiguration.createMQWrapper(configuration);
		wrapper.createTopicProducer(configuration.server().getResponseQueueName());
		// create consumer listening on request topic 
		wrapper.createTopicConsumer(configuration.server().getRequestQueueName(), 
									"CS_HostId = '"+serverId.toString()+"'");
		wrapper.registerListener(this);
		
		//have to do like that to stay thread-safe.
		IMQWrapper oldWrapper = this.mq;
		this.mq = wrapper;
		
		if(oldWrapper != null)
			oldWrapper.close();
	}
	@Override
	public void onMessage(Message msg) {
		threadpool.execute(new MessageHandler(msg));
	}
	
	@Override
	public void onConfigurationChange(JCloudScaleConfiguration newConfiguration) 
	{
		this.log = JCloudScaleConfiguration.getLogger(newConfiguration, this);
		
		ServerConfiguration newServerCfg = newConfiguration.server();
		ServerConfiguration oldServerCfg = JCloudScaleConfiguration.getConfiguration().server();
		
		if(!this.mq.configurationEquals(newConfiguration.common().communication()) ||
				!oldServerCfg.getRequestQueueName().equals(newServerCfg.getRequestQueueName()) ||
				!oldServerCfg.getResponseQueueName().equals(newServerCfg.getResponseQueueName()))
		{
			try 
			{
				initMessageQueue(newConfiguration);
			} catch (NamingException | JMSException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void close()
	{
		if(this.mq != null)
		{
			this.mq.close();
			this.mq = null;
		}
		
		if(this.threadpool != null)
		{
			this.threadpool.shutdown();
			this.threadpool = null;
		}
	}
	
	
	protected class MessageHandler implements Runnable 
	{
		protected Message msg;
		
		public MessageHandler(Message msg) {
			this.msg = msg;
		}
		
		@Override
		public void run() 
		{
			
			if(!(msg instanceof ObjectMessage)) {
				log.severe("Processed unknown message from request queue: "+msg.toString());
				return;
			}
			
			RequestObject o = null;
			UUID corrId = null;
			try {
				o = (RequestObject) ((ObjectMessage)msg).getObject();
				String corrIdAsString = msg.getJMSCorrelationID();
				if(corrIdAsString != null)
					corrId = UUID.fromString(corrIdAsString);
			} catch (JMSException e) {
				e.printStackTrace();
				log.severe("Failed to write message object from message: "+e.getMessage());
				return;
			}
			
			boolean handled = false;
			
			/*
			 * Handle a create request 
			 */
			if(o instanceof CreateObject) {
				handled = true;
				CreateObject create = (CreateObject)o; 
				String classname = create.getClassname();
				byte[] params = create.getParams();
				String[] paramNames = create.getParamNames();
				
				String ret = null;
				CreateReturnObject returnObj = new CreateReturnObject();
				try {
					ret = server.createNewCloudObject(classname, params, paramNames);
					returnObj.setReturnval(ret);
				} catch(JCloudScaleException e) {
					returnObj.setException(e);
				}
				
				sendReturnMsg(returnObj, corrId);
				
			}
			
			/*
			 * Handle a start invocation request
			 */
			if(o instanceof StartInvokationObject) {
				handled = true;
				StartInvokationObject start = (StartInvokationObject)o;
				String id = start.getId();
				String method = start.getMethod();
				byte[] params = start.getParams();
				String[] paramNames = start.getParamNames();
				String ret = null;
				StartInvokationReturnObject returnObj = new StartInvokationReturnObject();
				try {
					ret = server.startInvokingCloudObject(id, method, params, paramNames);
					returnObj.setReturnval(ret);
				} catch(JCloudScaleException e) {
					returnObj.setException(e);
				}
				
				sendReturnMsg(returnObj, corrId);
			}
			
			/*
			 * Handle a request for a field value
			 */
			if(o instanceof GetFieldValueObject) {
				handled = true;
				GetFieldValueObject get = (GetFieldValueObject)o;
				byte[] ret;
				GetFieldValueResponseObject returnObj = new GetFieldValueResponseObject(); 
				try {
					ret = server.getCloudObjectField(get.getObject(), get.getField());
					returnObj.setFieldValue(ret);
				} catch(JCloudScaleException e) {
					returnObj.setException(e);
				}
				
				sendReturnMsg(returnObj, corrId);
			}
			
			/*
			 * Handle a request for setting a field value
			 */
			if(o instanceof SetFieldValueObject) {
				handled = true;
				SetFieldValueObject set = (SetFieldValueObject)o;
				server.setCloudObjectField(set.getObject(), set.getField(), set.getValue());
				sendReturnMsg(new SetFieldValueReturnObject(), corrId);
			}
			
			/*
			 *  Handle a destroy object request 
			 */
			if(o instanceof DeleteObject) {
				handled = true;
				DeleteObject delete = (DeleteObject)o;
				String id = delete.getId();
				
				DeleteReturnObject returnObj = new DeleteReturnObject();
				try {
					server.destroyCloudObject(id);
				} catch(JCloudScaleException e) {
					returnObj.setException(e);
				}
				
				sendReturnMsg(returnObj, corrId);
			}
			
			/*
			 *  Handle a keepalive request 
			 */
			if(o instanceof KeepaliveObject) {
				handled = true;
				KeepaliveObject keepalive = (KeepaliveObject)o;
				UUID id = keepalive.getId();
				
				try {
					((JCloudScaleServer)server).keepAliveCloudObject(id);
				} catch(Exception e) {
					e.printStackTrace();
					log.severe("Failed to handle isalive message: "+e.getMessage());
					return;
				}
			}
			
			/*
			 *  Handle a shutdown request 
			 */
			if(o instanceof ShutdownObject) {
				handled = true;
				server.shutdown();
			}
			
			// now add the migration requests
			
			/*
			 * Handle a serialize CO state for migration request
			 */
			if (o instanceof SerializedCOStateObject) {
				handled = true;
				SerializedCOStateObject migration = (SerializedCOStateObject) o;
				String id = migration.getId();

				SerializedCOStateReturnObject returnObj = new SerializedCOStateReturnObject();
				try {
					byte[] bytes = server.serializeToMigrate(id);
					returnObj.setSerialized(bytes);
				} catch (JCloudScaleException e) {
					returnObj.setException(e);
				}

				sendReturnMsg(returnObj, corrId);
				return;
			}

			/*
			 * Handle migrated CO deployment request
			 */
			if (o instanceof MigratedCODeploymentObject) {
				handled = true;
				MigratedCODeploymentObject migration = (MigratedCODeploymentObject) o;
				MigratedCODeploymentReturnObject returnObj = new MigratedCODeploymentReturnObject();
				try {
					server.deployMigratedCloudObject(
							migration.getCloudObjectId(),
							migration.getObjectType(),
							migration.getData());
				} catch (JCloudScaleException e) {
					returnObj.setException(e);
				}

				sendReturnMsg(returnObj, corrId);
				return;
			}

			/*
			 * Handle remove migrated CO request
			 */
			if (o instanceof MigratedCORemoveObject) {
				handled = true;
				MigratedCORemoveObject remove = (MigratedCORemoveObject) o;
				MigratedCORemoveReturnObject returnObj = new MigratedCORemoveReturnObject();
				try {
					server.removeCloudObject(remove.getCloudObjectId());
				} catch (JCloudScaleException e) {
					returnObj.setException(e);
				}

				sendReturnMsg(returnObj, corrId);
				return;
			}

			/*
			 * Handle get cloudobject size request
			 */
//			if (o instanceof GetCloudObjectSizeObject) {
//				GetCloudObjectSizeObject sizeReq = (GetCloudObjectSizeObject) o;
//				GetCloudObjectSizeObjectReturn sizeResp = new GetCloudObjectSizeObjectReturn();
//				try {
//					long size = server.getCloudObjectSize(sizeReq.getCloudObjectId());
//					sizeResp.setCloudObjectSize(size);
//				} catch (JCloudScaleException e) {
//					sizeResp.setException(e);
//				}
//
//				sendReturnMsg(sizeResp, corrId);
//				return;
//			}
			
			if(!handled) {
				log.severe("Server message handler received unknown message type: "+o.getClass().getCanonicalName());
			}
			
	
		}

		protected void sendReturnMsg(ReturnObject ret, UUID correlationId) {
			try {
				mq.oneway(ret, correlationId);
			} catch(JMSException e) {
				e.printStackTrace();
				log.severe("Failed to send message to JMS queue: "+e.getMessage());
			}
		}
		
	}
	
}
