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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.logging.Ignore;
import at.ac.tuwien.infosys.jcloudscale.logging.Logged;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.IsAliveObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.IsDeadObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.StaticHostsObject;

@Logged
public class IdManager implements Closeable
{
	/**
	 * Time in ms how long thread should sleep in case the condition is not met.
	 */
	private static final long WAIT_TIME = 100;
	protected static IdManager instance;
	 
	protected final static Object lock = new Object();
	protected final static Object instanceLock = new Object();
	
	private List<UUID> staticInstances;
	private List<UUID> dynamicInstances;
    private Map<UUID,Boolean> ids;
    private Map<UUID, Long> timestamps;
    private Map<UUID, String> ipAddresses;
    private IMQWrapper mq;
	protected Logger log = null;
	private Timer timer = null;
	private volatile boolean duringStartup = false; 
	private MessageQueueConfiguration communicationConfig;
	
	public IdManager(MessageQueueConfiguration communicationConfiguration) 
	{
		this.communicationConfig = communicationConfiguration;
        ids = new HashMap<UUID, Boolean>();
        timestamps = new HashMap<UUID, Long>();
        ipAddresses = new HashMap<UUID, String>();
        staticInstances = new ArrayList<>();
        dynamicInstances = new ArrayList<>();
		log = JCloudScaleConfiguration.getLogger(this);
		
		start();
	}
	
	@Ignore
	private long getIsAliveInterval()
	{
		return JCloudScaleConfiguration.getConfiguration().server().getIsAliveInterval();
	}
	
	@Ignore
	private long getHostInitializationTimeout()
	{
		return JCloudScaleConfiguration.getConfiguration().server().getHostInitializationTimeout();
	}
	
	protected void start() 
	{
		try 
		{
			timer = new Timer();
			long isAliveInterval = getIsAliveInterval();
			timer.schedule(new HostCleanup(), isAliveInterval, isAliveInterval);
			
			mq = communicationConfig.newWrapper();
			mq.createQueueConsumer(JCloudScaleConfiguration.getConfiguration()
					.server().getInitializationQueueName());
			try
			{
				duringStartup = true; // we declare it true here to not miss any static hosts.
				mq.registerListener(new IsAliveMessageReceiver());
				pollStaticInstances();
			}
			finally
			{
				duringStartup = false;
			}
			
			
		} catch (JMSException | NamingException e) {
			e.printStackTrace();
			log.severe(e.getMessage());
		}

	}
	
	@Ignore
	public UUID getFreeId(boolean staticInstanceOk) 
	{
		UUID selectedId = null;
		
		synchronized (lock) 
		{
			//
			// see if we have an unused static instance
			//
			if(staticInstanceOk) {
				for(UUID instance : staticInstances) 
				{
					if(!ids.containsKey(instance) || !ids.get(instance)) 
					{
						ids.put(instance, true);
						selectedId = instance;
						log.info("Using static instance "+instance.toString());
						break;
					}
				}
			}
			
			if(selectedId == null)
			{
				//
				// no static instance available, see if we have a dynamic instance free
				//
				for(UUID instance : dynamicInstances) 
				{
					if(!ids.containsKey(instance) || !ids.get(instance)) 
					{
						ids.put(instance, true);
						selectedId = instance;
						log.info("Using dynamic instance "+instance.toString());
						break;
					}
				}
			}
		}
		
		return selectedId;
	}

	public UUID waitForId() 
	{
		long begin = System.nanoTime();
		
		while(true) 
		{
			
			UUID id = getFreeId(false);
            if(id != null)
            	return id;
			
            long now = System.nanoTime();
            if(now - begin > getHostInitializationTimeout() * 1000000)	// nanotime -> milliseconds
            	throw new JCloudScaleException("Timed out waiting for new host to become available.");
            
            try {
				Thread.sleep(WAIT_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
            
		}
	}
	
	
	/**
	 * Releases specified id in the local Id manager. 
	 * @param id
	 * The id of the instance to be released.
	 */
	public void releaseId(UUID id)
	{
		synchronized (lock) 
		{
			if(this.ids.containsKey(id))
				this.ids.put(id, false);
		}
	}
	
	/**
	 * Remove this id from the Id manager. This should be called when
	 * we requested to destroy a host. Basically a faster way to remove
	 * a host before the id times out/
	 * 
	 * @param id The id to be removed
	 */
	public void removeId(UUID id) {
		synchronized (lock) {
			cleanupId(id);
			log.info("Removed old host "+id.toString()+" because of explicit request.");
		}
	}
	
	/**
	 * Checks if this ID belongs to a static or dynamic host
	 * 
	 * @param serverId The id to check
	 * @return True if this ID belongs to a static host, false otherwise.
	 */
	public boolean isStaticId(UUID serverId) {
		return staticInstances.contains(serverId);
	}
	
	/**
	 * @param id
	 * @return
	 */
	public String getIpToId(UUID id) 
	{
		return ipAddresses.get(id);
	}
	
	/**
	 * Returns the collection of registered instances in this id manager. 
	 * Currently needed for the openstack tests to be able to wait for instances to start
	 * and shut down them properly. 
	 * @return
	 */
	public List<UUID> getRegisteredInstances()
	{
		List<UUID> hosts = new ArrayList<>();
		hosts.addAll(staticInstances);
		hosts.addAll(dynamicInstances);
		
		return hosts;
	}
	
    @Override
	public void close() 
	{
    	timer.cancel();
    	mq.close();// have to close outside of the lock to allow last messages being processed. (caused deadlock otherwise)
    	
    	synchronized (lock) 
    	{
    		staticInstances.clear();
    		dynamicInstances.clear();
    		ids.clear();
    		timestamps.clear();
    		ipAddresses.clear();
		}
	}
	
//	public static IdManager getInstance() 
//	{
//		synchronized (instanceLock) 
//		{
//			if(instance == null) {
//				instance = new IdManager();
//				instance.start();
//			}
//			return instance;	
//		}
//	}
	
//	public static synchronized void closeInstance()
//	{
//		if(instance != null)
//		{
//			instance.close();
//			instance = null;
//		}
//	}
	
	/**
	 *  this method should only be called at startup, before
		 launching any dynamic instances
		 Basically, the only difference between static and dynamic
		 instances is whether they were already there during
		 application startup
		 we expect "duringStartup" variable to be set prior to this 
		 method call to avoid race conditions and missed hosts.
	 */
	private void pollStaticInstances() {
		
		// we send a special request to all currently active hosts to
		// send an isalive message NOW so that we have an up-to-date list
		try(IMQWrapper tmpWrapper = communicationConfig.newWrapper()) {
			
			tmpWrapper.createTopicProducer(JCloudScaleClient.getConfiguration().server().getStaticHostsQueueName());
			StaticHostsObject obj = new StaticHostsObject();
			obj.setClientId(JCloudScaleClient.getConfiguration().common().clientID());
			
			// sending the message.
			tmpWrapper.oneway(obj);
			
			log.info("Starting to wait for static instances to announce themselves.");
			
			// now wait to give the hosts time to respond.
			Thread.sleep(JCloudScaleClient.getConfiguration().server().getStaticHostDiscoveryInterval()/2);
			
			// trying to send message again.
			tmpWrapper.oneway(obj);
			
			// now wait to give the hosts time to respond.
			Thread.sleep(JCloudScaleClient.getConfiguration().server().getStaticHostDiscoveryInterval()/2);
			
			log.fine("Finished waiting for static instances.");
			
			log.info("Found "+staticInstances.size()+" static instances and "+dynamicInstances.size()+" dynamic instances.");
			
			if(dynamicInstances.size() > 0)
				log.warning("   !!There were static hosts discovered as dynamic ones!!");
			
		} catch (NamingException | JMSException | InterruptedException e) {
			e.printStackTrace();
			log.severe("Exception while polling for static instances during ID manager startup: "+e.getMessage());
		}
	}
	
	private void cleanupId(UUID id) {
		if(staticInstances.contains(id))
			staticInstances.remove(id);
		if(dynamicInstances.contains(id))
			dynamicInstances.remove(id);
		ids.remove(id);
		timestamps.remove(id);
		ipAddresses.remove(id);
	}
	
	private class IsAliveMessageReceiver implements MessageListener 
	{
        @Override
        public void onMessage(Message msg) 
        {
            if(msg instanceof ObjectMessage) 
            {
                try 
                {
                    Object obj = ((ObjectMessage)msg).getObject();

                    synchronized (lock) 
                    {
                    	if(obj instanceof IsAliveObject) 
                        	processIsAliveMessage(((IsAliveObject)obj).getId(), msg.getJMSTimestamp(), ((IsAliveObject)obj).getIp());
                        else 
                        	if(obj instanceof IsDeadObject) 
                    			processIsDeadMessage(((IsDeadObject)obj).getId());	
                        	else 
                        		log.warning("Unexpected message in isAlive Queue:" + obj);
					}
                } 
                catch (JMSException e) 
                {
                    e.printStackTrace();
                }
            } 
            else
                log.severe("Received invalid message in isalive queue");
        }

		private void processIsDeadMessage(String id) 
		{
			
			UUID theId = UUID.fromString(id);
			
			if(ids.containsKey(theId)) 
			{
				cleanupId(theId);
	         	log.finest("Received isdead message for host "+id+". Removing from list.");
			} 
			else 
			{
			 	log.finest("Received isdead message for unknown host "+id+". Ignoring");
			}
		}

		private void processIsAliveMessage(String id, long jmsTimestamp, String ipAddress) 
		{
			
			UUID theId = UUID.fromString(id);
			
			long messageDelay = System.currentTimeMillis() - jmsTimestamp;
			
	        if(ids.containsKey(theId)) 
	        {
    			timestamps.put(theId, System.currentTimeMillis());
    			log.finest("Received isalive message for known host "+id+". Updating timestamp. (msg delay = "+messageDelay+"ms)");
	        } 
	        else {
//	        else if(jmsTimestamp >= System.currentTimeMillis() - 3 * getIsAliveInterval()) 
//    		{
				ipAddresses.put(theId, ipAddress);
				timestamps.put(theId, System.currentTimeMillis());
				if(duringStartup)
					staticInstances.add(theId);
				else
					dynamicInstances.add(theId);
				ids.put(theId, false);
                log.fine("Received isalive message for new host "+id+" (ip="+ipAddress+"). Adding to list (msg delay = "+messageDelay+"ms)");
//    		} 
//	        else 
//    		{
//    			log.finest("Received old isalive message for host "+id+", ignoring. (msg delay = "+messageDelay+"ms)");
//    		}
	        }
		}
	}
	
	private class HostCleanup extends TimerTask {
		
		@Override
		public void run() 
		{
			// the host timed out when last timestamp was more than 3 msg intervals ago.
			long timeout = System.currentTimeMillis() - 3 * getIsAliveInterval();
			
			List<UUID> hostsToRemove = new ArrayList<UUID>();
			
			//gathering hosts that should be removed.
			synchronized (lock) 
			{
				for(Entry<UUID, Long> entry : timestamps.entrySet())
				{
					if(entry.getValue() < timeout)
						hostsToRemove.add(entry.getKey());
				}
				
				if(hostsToRemove.size()  == 0)
					return;//nothing to clean up
				
				//removing hosts
				for(UUID id : hostsToRemove)	
				{
					//checking if host is in use.		
					if(ids.containsKey(id) && ids.get(id))
					{ 
						log.severe("Should be timing out host "+id.toString()+", but it is in use.");
					}
					else 
					{
						cleanupId(id);
						log.info("Removed old host "+id.toString()+" because of timeout.");
					}
				}
			}	
		}
	}
}
