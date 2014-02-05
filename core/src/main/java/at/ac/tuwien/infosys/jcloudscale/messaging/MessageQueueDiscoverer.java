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
package at.ac.tuwien.infosys.jcloudscale.messaging;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.server.discovery.MulticastDiscoverer;

class MessageQueueDiscoverer implements Closeable {
	private MessageQueueConfiguration cfg;

	private volatile String serverAddress = null; // we use this field as a completion flag.
	private int serverPort = 0;
	
	private Logger log;
	private ExecutorService threadPool;
	private volatile long discoveryStartTime;
	
	private Object sync = new Object();

	MessageQueueDiscoverer(MessageQueueConfiguration configuration) 
	{
		log = JCloudScaleConfiguration.getLogger(this);
		
		this.cfg = configuration;
		threadPool = Executors.newCachedThreadPool();
	}

	// -------------------------------------

	public String getServerAddress() {
		return serverAddress;
	}

	public int getServerPort() {
		return serverPort;
	}

	// -------------------------------------

	@Override
	public void close()
	{
		if(threadPool != null)
		{
			threadPool.shutdown();
			
//			try 
//			{// this waiting and interrupting causes error messages on localhost searching.
//				//let's wait... but should not be a problem even if we leave thread pool without waiting.
//				if(!threadPool.awaitTermination(1, TimeUnit.SECONDS))
//					threadPool.shutdownNow();
//			} 
//			catch (InterruptedException e) 
//			{
//			}
			
			threadPool = null;
		}
	}
	
	public boolean tryDiscoverServer() 
	{
		synchronized (sync) 
		{
			//we start search here to have correct start time in search threads.
			discoveryStartTime = System.currentTimeMillis();
			
			//
			// scheduling possible/allowed discoverers
			//
			if(cfg.tryDiscoverWithMulticast)
				threadPool.execute(new MulticastMqDiscoverer());
			
			if(cfg.tryDiscoverInFileSystem)
				threadPool.execute(new FileBasedMqDiscoverer());
			
			if(cfg.tryDiscoverOnLocalhostPorts)
			{
				//scheduling the one that we have configuration for (it may be different from defaults).
				int configuredPort = cfg.serverPort;
				threadPool.execute(new LocalhostMqDiscoverer(cfg.serverPort));
				
				//scheduling default ports.
				for(int port : MessageQueueConfiguration.possibleLocalPorts)
					if(port != configuredPort)//to avoid scheduling same port twice.
						threadPool.execute(new LocalhostMqDiscoverer(port));
			}
			
			//
			// Waiting for results.
			//
			try 
			{
				do
				{
					sync.wait(cfg.multicastDiscoveryTimeout);
				}
				while(canTryDiscoveringLonger());
					
				log.info("Discovered MQ Server at "+serverAddress+":"+serverPort);
				
				return serverAddress != null;//we discovered successfully if we have something in serverAddress.
				
			} catch (InterruptedException e) 
			{
				return false;
			}
		}
	}
	
	/**
	 * Tells if caller can spend some more efforts trying to discover resource.
	 * @return <b>true</b> if resource was not discovered yet and timeout did not passed. Otherwise, <b>false</b>.
	 */
	private boolean canTryDiscoveringLonger()
	{
		return serverAddress == null && System.currentTimeMillis() - discoveryStartTime < cfg.multicastDiscoveryTimeout;
	}
	
	private void signalDiscovered(String address, int port)
	{
		synchronized (sync) 
		{
			this.serverAddress = address;
			this.serverPort = port;
			
			sync.notifyAll();
		}
	}

	// --------------------- Inner Classes---------------------------

	private class FileBasedMqDiscoverer implements Runnable
	{
		@Override
		public void run() 
		{
			File configurationFile = new File(cfg.getMessageQueueConnectionFilePath());
			try
			{
				while(canTryDiscoveringLonger())
				{
					if(configurationFile.exists() && configurationFile.isFile())
					{
						try(Scanner scanner = new Scanner(configurationFile).useDelimiter(":|"+System.lineSeparator()))
						{
							String hostname = scanner.next();
							int port = scanner.nextInt();
							
							signalDiscovered(hostname, port);
							
							return;
						}
						catch(Exception ex)
						{
							log.severe("Failed to parse content of MQ configuration file "+
											configurationFile.getAbsolutePath()+": " +ex);
						}
					}
					
					Thread.sleep(500);//let's wait some time.
				}
			}
			catch(InterruptedException ex)
			{
				//that's completely fine.
			}
		}
	}
	
	private class MulticastMqDiscoverer implements Runnable 
	{
		@Override
		public void run() 
		{
			byte[] serverReply = null;
			
			try 
			{
				int attempts = 3;//sometimes it does not workout of the first attempts
				do
				{
					try
					{
						serverReply = MulticastDiscoverer.discoverResource(
								MessageQueueConfiguration.multicastDiscoveryAddress,
								MessageQueueConfiguration.multicastDiscoveryPort, cfg.multicastPoolingInterval);
					}
					catch(IOException ex)
					{
						if(attempts-- < 0)
							throw ex;
						else
						{
							log.info("Failed to do multicast discovery, retrying :" + ex);
							Thread.sleep(10);//waiting for a while
						}
					}
				}
				while(serverReply == null && canTryDiscoveringLonger());
					
			} catch (IOException | InterruptedException e) 
			{
				log.severe("Exception while trying to do multicast discovery: "+ e);
			}

			if (serverReply == null)
				return;// failed to discover. shit happens, need to move along.

			String serverAddress = new String(serverReply).trim();

			String[] parts = serverAddress.split(MessageQueueConfiguration.SEPARATOR);

			if (parts.length != 2)
			{
				log.warning("Unexpected message while trying to do Message Queue multicast Discovery: "+ serverAddress);
				return;
			}
			
			String address = parts[0];
			int serverPort = Integer.parseInt(parts[1]);
			signalDiscovered(address, serverPort);
		}
	}
	
	private class LocalhostMqDiscoverer implements Runnable
	{
		private int port;
		
		public LocalhostMqDiscoverer(int port) 
		{
			this.port = port;
		}
		@Override
		public void run() 
		{
			while(canTryDiscoveringLonger())
			{
				try 
				{
					if(JMSConnectionHolder.isMessageQueueServerAvailable(cfg.serverAddress, port))
					{
						signalDiscovered(cfg.serverAddress, port);
						return;
					}
				} 
				catch(JCloudScaleException ex)
				{
					//this exception will be thrown in case of timeout or interruption, what is quite fine for us.
				}
				catch (NamingException | JMSException e) 
				{
					log.warning("Trying to discover ActiveMQ on localhost:"+port+", received exception:"+e);
				}
			}
		}
		
	}
}
