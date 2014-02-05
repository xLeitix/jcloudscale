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
package at.ac.tuwien.infosys.jcloudscale.server.discovery;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.logging.Logger;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.server.PlatformSpecificUtil;

/**
 * @author rst
 * Allows to publish and discover resource using multicast protocol.
 */
public class MulticastDiscoverer 
{
	private static final int MESSAGE_SIZE = 128;//size of the data message, in bytes.
	
	/**
	 * Publishes information specified by <b>discoveryInformation</b> on multicast address and port.
	 * @param multicastAddress The multicast address <b>discoveryInformation</b> should be available at.
	 * @param port The port <b>discoveryInformation</b> should be available at.
	 * @param discoveryInformation The information that needs to be discovered via multicast. Size should be limited to MESSAGE_SIZE.
	 * @return <i>Closeable</i> object that allows to control how long provided information will be available for discovery.
	 * @throws IOException In case specified address or port are incorrect (not correct values or not multicast address).
	 */
	public static Closeable publishResource(String multicastAddress, int port, byte[] discoveryInformation) throws IOException
	{
		return new MulticastResourceNotificator(multicastAddress, port, discoveryInformation);
	}
	
	/**
	 * Sends discovery message and waits for the answer until timeout occurs. 
	 * @param mulitcastAddress The multicast address the resource is expected to be publish on.
	 * @param port The port that information should be available on.
	 * @param responseAwaitTimeout How long response should be awaited. (for normal networks there's no need in big values 1 sec is more than enough.)
	 * @return The discovery information or null if timeout occured.
	 * @throws IOException if failed to bind local socket or send discovery message.
	 */
	public static byte[] discoverResource(String mulitcastAddress, int port, long responseAwaitTimeout) throws IOException
	{
		try(DatagramSocket socket = new DatagramSocket(0))
		{
			socket.setSoTimeout((int)responseAwaitTimeout);
			//
			// Preparing and sending multicast packet.
			//
			InetAddress group = InetAddress.getByName(mulitcastAddress);
            String message = PlatformSpecificUtil.findBestIP()+":"+socket.getLocalPort();
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, group, port);
            
            socket.send(packet);
            
            //
            // receiving answer
            //
            packet = new DatagramPacket(new byte[MESSAGE_SIZE], MESSAGE_SIZE);
            socket.receive(packet);
            
            return packet.getData();
		}
		catch(SocketTimeoutException ex)
		{
			//that's fine. Just that no answer within timeout
			return null;
		}
	}
	
	public static byte[] discoverResource(String mulitcastAddress, int port, long responseAwaitTimeout, long discoveryTimeout) throws IOException
	{
		long startTime = System.currentTimeMillis();
		byte[] serverReply = null;
		do
		{
			try
			{
				serverReply = MulticastDiscoverer.discoverResource(
					mulitcastAddress, port,	responseAwaitTimeout);
			}
			catch(Exception ex)
			{	
				// this might happen because of some race conditions in socket implementations of Java or some windows bugs, at least from what I saw. 
				// (like http://stackoverflow.com/questions/10088363/java-net-socketexception-no-buffer-space-available-maximum-connections-reached)
				JCloudScaleConfiguration.getLogger(MulticastDiscoverer.class).severe("Failed to send Multicast Discovery Request: "+ex);
			}
		}
		while(serverReply == null && 
				((System.currentTimeMillis() - startTime) < discoveryTimeout) && 
				!Thread.interrupted());
		
		return serverReply;
	}
	
	//-------------------------------------------------
	
	private static class MulticastResourceNotificator implements Closeable, Runnable
	{
		private static final String NAME = "MULTICAST RESOURCE NOTIFICATOR THREAD";
		
		private final Logger log = JCloudScaleConfiguration.getLogger(MulticastResourceNotificator.class);
		
		private volatile boolean isAborted = false;
		
		private MulticastSocket socket;
		InetAddress group;
		private byte[] data;
		
		public MulticastResourceNotificator(String multicastAddress, int port, byte[] discoveryInformation) throws IOException
		{
			this.data = discoveryInformation;
			
			//
			//configuring multicast socket.
			//
			socket = new MulticastSocket(port);
			group = InetAddress.getByName(multicastAddress);
			socket.joinGroup(group);
			Thread notificationThread = new Thread(this, NAME);
			notificationThread.start();
		}

		@Override
		public void run() 
		{
			log.fine("Multicast resource notificator starts on thread "+NAME);
			
			while(!isAborted)
			{
				try
				{
					byte[] buffer = new byte[MESSAGE_SIZE];
					
					//
					// Waiting for message
					//
					DatagramPacket packet =  new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);//here we are waiting for something to come.
					
					String discovererAddress = new String(packet.getData()).trim();
					
					log.info(String.format("%s:\tNew discovery request \"%s\"", new Date(System.currentTimeMillis()), discovererAddress));
					
					String hostname = discovererAddress.substring(0, discovererAddress.indexOf(':'));
					if(hostname == null || hostname.length() == 0)
					{
						log.severe("Message \""+discovererAddress+"\": format is incorrect. Skipping.");
						continue;
					}
					
					int port = Integer.parseInt(discovererAddress.substring(discovererAddress.indexOf(':')+1,discovererAddress.length()));

					//
					// Sending response.
					//
					packet = new DatagramPacket(data, data.length, InetAddress.getByName(hostname), port);
					socket.send(packet);
				}
				catch(SocketException ex)
				{
					log.fine("SocketException: Assuming that this is fine, just socket is closed: "+ex.toString());
				}
				catch(Exception ex)
				{
					log.severe(String.format("%s: Exception occured: %s", NAME, ex.toString()));
				}
			}
			
			log.fine(NAME+": Multicast resource notificator stops.");
		}
		
		@Override
		public void close()
		{
			isAborted = true;
			if(socket == null)
				return;
			
			try
			{
				socket.leaveGroup(group);
			}
			catch(IOException ex){}//who cares? we close anyways.
			
			socket.close();
			socket = null;
		}
	}
}
