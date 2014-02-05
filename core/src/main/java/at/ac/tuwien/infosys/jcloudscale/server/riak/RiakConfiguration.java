package at.ac.tuwien.infosys.jcloudscale.server.riak;
///*
//   Copyright 2013 Philipp Leitner
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//*/
//package at.ac.tuwien.infosys.jcloudscale.server.riak;
//
//import java.io.Closeable;
//import java.io.IOException;
//import java.io.Serializable;
//
//import javax.xml.bind.annotation.XmlAccessType;
//import javax.xml.bind.annotation.XmlAccessorType;
//import javax.xml.bind.annotation.XmlRootElement;
//
//import at.ac.tuwien.infosys.jcloudscale.server.PlatformSpecificUtil;
//import at.ac.tuwien.infosys.jcloudscale.server.discovery.MulticastDiscoverer;
//
//@XmlRootElement
//@XmlAccessorType(XmlAccessType.FIELD)
//public class RiakConfiguration implements Serializable
//{
//	private static final long serialVersionUID = 1L;
//	
//	private static final boolean USE_HTTP_DEFAULT = false;//just to keep both ports here. default is tcp.
//	private static final String SEPARATOR = "#";//some char to separate parameters in multicast transfer. ":" should also work fine.
//	
//	private String serverAddress = ""; //if null/"" here, server will be discovered.
//	private int serverPort = USE_HTTP_DEFAULT ? 8098 :8087;
//	
//	private final String multicastDiscoveryAddress = "239.6.6.6";
//	private final int multicastDiscoveryPort = 6346;
//	private long multicastDiscoveryTimeout = 60000;
//	private long multicastPoolingInterval = 1000;
//	private boolean startMulticastPublisher = false;
//	
//	private boolean useHttpProtocol = USE_HTTP_DEFAULT;//maybe some sort of switch between HTTP and TCP?
//	private String bucketName = "JCloudScale";
//	
//	
//	String getServerAddress() throws IOException
//	{
//		if(serverAddress == null || serverAddress.length() == 0)
//			discoverServerAddress();//this method sets address and port, if succeeds.
//		
//		return this.serverAddress;
//	}
//	
//	int getServerPort()
//	{
//		return this.serverPort;
//	}
//	
//	boolean useHttpProtocol()
//	{
//		return this.useHttpProtocol;
//	}
//	
//	String bucketName()
//	{
//		return this.bucketName;
//	}
//	
//	
//	public RiakWrapper getKeyValueStorageWrapper()
//	{
//		return RiakWrapper.getInstance(this);
//	}
//	
//	public Closeable createServerPublisher() throws IOException
//	{
//		return new RiakServerPublisher();
//	}
//	
//	public boolean startMulticastPublisher() {
//		return startMulticastPublisher;
//	}
//
//	public void setStartMulticastPublisher(boolean startMulticastPublisher) {
//		this.startMulticastPublisher = startMulticastPublisher;
//	}
//	
//	public void setServerAddress(String serverAddress) {
//		this.serverAddress = serverAddress;
//	}
//
//	public void setServerPort(int serverPort) {
//		this.serverPort = serverPort;
//	}
//
//	public void setMulticastDiscoveryTimeout(long multicastDiscoveryTimeout) {
//		this.multicastDiscoveryTimeout = multicastDiscoveryTimeout;
//	}
//
//	public void setMulticastPoolingInterval(long multicastPoolingInterval) {
//		this.multicastPoolingInterval = multicastPoolingInterval;
//	}
//
//	public void setUseHttpProtocol(boolean useHttpProtocol) {
//		this.useHttpProtocol = useHttpProtocol;
//	}
//	
//	//---------------------------------------------------------
//
//	private void discoverServerAddress() throws IOException 
//	{
//		byte[] riakReply = MulticastDiscoverer.discoverResource(
//				this.multicastDiscoveryAddress, 
//				this.multicastDiscoveryPort, 
//				this.multicastPoolingInterval, 
//				this.multicastDiscoveryTimeout);
//		
//		if(riakReply == null)
//			return;//failed to discover.
//		
//		String riakServerAddress = new String(riakReply).trim();
//		
//		String[] parts = riakServerAddress.split(SEPARATOR);
//		
//		if(parts.length != 3)
//			return;//should we log that there's some wrong message?
//		
//		this.serverAddress = parts[0];
//		this.serverPort = Integer.parseInt(parts[1]);
//		this.useHttpProtocol = Boolean.parseBoolean(parts[2]);
//	}
//	
//	//-------------------------------------------------
//	
//	private class RiakServerPublisher implements Closeable
//	{
//		Closeable publisher;
//		
//		public RiakServerPublisher() throws IOException
//		{
//			//if server is not specified, we use our own IP address.
//			if(serverAddress == null || serverAddress.length() == 0)
//				serverAddress = PlatformSpecificUtil.findBestIP();
//			
//			byte[] discoveryInformation = (serverAddress + SEPARATOR + serverPort + SEPARATOR + useHttpProtocol).getBytes();
//			
//			publisher = MulticastDiscoverer.publishResource(multicastDiscoveryAddress, multicastDiscoveryPort, discoveryInformation);
//		}
//		
//		@Override
//		public void close() throws IOException 
//		{
//			if(publisher != null)
//			{
//				publisher.close();
//				publisher = null;
//			}
//		}
//	}
//}
