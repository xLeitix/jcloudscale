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
import java.io.IOException;
import java.io.Serializable;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.server.PlatformSpecificUtil;
import at.ac.tuwien.infosys.jcloudscale.server.discovery.MulticastDiscoverer;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MessageQueueConfiguration implements Serializable, Cloneable
{
    private static final long serialVersionUID = 1L;
    static final String SEPARATOR = "#";//some char to separate parameters in multicast transfer.

    protected String serverAddress = "localhost";
    protected int serverPort = 61616;

    int asyncSendThreads = 1;
    long asyncSendShutdownTimeout = 60000;

    long messageSizeLimit = 0;//524288000L;

    long requestTimeout = 60000;
    long retryTimeout = 60000;
    long connectionTimeout = 10*60*1000;//10 min?
    long reconnectIntervalLimit = 60*1000;
    private int deliveryMode = DeliveryMode.PERSISTENT;

    private boolean startMessageQueueServerAutomatically = true;

    // discovery data. Change this all wisely as the sever uses these fields BEFORE receiving configuration from client.
    boolean tryDiscoverWithMulticast = true;
    boolean tryDiscoverOnLocalhostPorts = true;
    boolean tryDiscoverInFileSystem = true;

    final static int[] possibleLocalPorts = new int[]{61615, 61616, 61617, 61618, 61619, 61620};

    final static String multicastDiscoveryAddress = "239.7.7.7";
    final static int multicastDiscoveryPort = 6356;
    /**
     * The path where server machine will search connection parameters to the message Queue on startup.
     */
    private String messageQueueConnectionFilePath = "/opt/jcloudscale/JCloudScaleMessageQueueConnection.cfg";

    long multicastDiscoveryTimeout = 60000;
    long multicastPoolingInterval = 500;
    private boolean startMulticastPublisher = false;

    public MessageQueueConfiguration(){}

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
    public void setMessageSizeLimit(long messageSizeLimit) {
        this.messageSizeLimit = messageSizeLimit;
    }
    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
    public void setAsyncSendThreads(int asyncSendThreads) {
        this.asyncSendThreads = asyncSendThreads;
    }
    public void setAsyncSendShutdownTimeout(long asyncSendShutdownTimeout) {
        this.asyncSendShutdownTimeout = asyncSendShutdownTimeout;
    }
    public void setRetryTimeout(long retryTimeout) {
        this.retryTimeout = retryTimeout;
    }
    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    public void setReconnectIntervalLimit(long reconnectIntervalLimit) {
        this.reconnectIntervalLimit = reconnectIntervalLimit;
    }
    public void setDeliveryMode(int deliveryMode) {
        this.deliveryMode = deliveryMode;
    }
    public void setStartMulticastPublisher(boolean startPublisher)
    {
        this.startMulticastPublisher = startPublisher;
    }
    public boolean startMulticastPublisher() {
        return startMulticastPublisher;
    }

    public long getRequestTimeout()
    {
        return this.requestTimeout;
    }

    public String getServerAddress()
    {
        return this.serverAddress;
    }
    public int getServerPort()
    {
        return this.serverPort;
    }

    public String getMessageQueueConnectionFilePath()
    {
        return messageQueueConnectionFilePath;
    }
    public void setMessageQueueConnectionFilePath(String messageQueueConnectionFilePath)
    {
        this.messageQueueConnectionFilePath = messageQueueConnectionFilePath;
    }

    public boolean getStartMessageQueueServerAutomatically()
    {
        return startMessageQueueServerAutomatically;
    }
    public void setStartMessageQueueServerAutomatically(boolean startServerAutomatically)
    {
        this.startMessageQueueServerAutomatically = startServerAutomatically;
    }

    public IMQWrapper newWrapper() throws NamingException, JMSException
    {
        return new MQWrapper(this, this.deliveryMode);
    }

    public IMQWrapper newWrapper(int deliveryMode) throws NamingException, JMSException
    {
        return new MQWrapper(this, deliveryMode);
    }

    public Closeable createServerPublisher() throws IOException
    {
        return new MessageQueuePublisher();
    }

    public boolean tryDiscoverMQServer() throws IOException
    {
        try(MessageQueueDiscoverer discoverer = new MessageQueueDiscoverer(this))
        {
            if(discoverer.tryDiscoverServer())
            {
                this.serverAddress = discoverer.getServerAddress();
                this.serverPort = discoverer.getServerPort();
                return true;
            }
            else
                return false;
        }
    }

    @Override
    public MessageQueueConfiguration clone()
    {
        try
        {
            // calling parent class to clone its stuff and make a shallow-copy object.
            MessageQueueConfiguration config = (MessageQueueConfiguration)super.clone();

            return config;
        }
        catch (CloneNotSupportedException e)
        {
            throw new JCloudScaleException(e, "Failed to clone configuration.");
        }
    }

    private class MessageQueuePublisher implements Closeable
    {
        Closeable publisher;

        public MessageQueuePublisher() throws IOException
        {
            //if server is not specified, we use our own IP address.
            if(serverAddress == null || serverAddress.length() == 0)
                serverAddress = PlatformSpecificUtil.findBestIP();

            //TODO: should we check server availability at this address and/or print log message about the stuff we offer?
            byte[] discoveryInformation = (serverAddress + SEPARATOR + serverPort).getBytes();

            publisher = MulticastDiscoverer.publishResource(multicastDiscoveryAddress, multicastDiscoveryPort, discoveryInformation);
        }

        @Override
        public void close() throws IOException
        {
            if(publisher != null)
            {
                publisher.close();
                publisher = null;
            }
        }
    }
}
