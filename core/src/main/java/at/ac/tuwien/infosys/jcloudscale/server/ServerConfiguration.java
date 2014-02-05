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
package at.ac.tuwien.infosys.jcloudscale.server;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.logging.ServerLoggingConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudPlatformConfiguration;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServerConfiguration implements Serializable, Cloneable
{
    private static final long serialVersionUID = 1L;

    private String initializationQueueName = "CS_Hosts";
    private String staticHostsQueueName = "CS_StaticHosts";
    private String requestQueueName = "CS_Requests";
    private String responseQueueName = "CS_Responses";
    private String callbackRequestQueueName = "CS_Callback_Requests";
    private String callbackResponseQueueName = "CS_Callback_Responses";
    private String staticFieldWriteRequestQueueName = "CS_StaticField_Writes_Requests";
    private String staticFieldWriteResponseTopicName = "CS_StaticField_Writes_Responses";
    private String staticFieldReadRequestQueueName = "CS_StaticField_Reads_Requests";
    private String staticFieldReadResponsesQueueName = "CS_StaticField_Reads_Responses";
    private long isAliveInterval = 5000;
    private long staticHostDiscoveryInterval = 1000;
    private long hostInitializationTimeout = 1000*60*6;
    private long invocationTimeout = Long.MAX_VALUE;

    private CloudPlatformConfiguration cloudPlatform;

    private ServerLoggingConfiguration serverLogging = new ServerLoggingConfiguration();

    public ServerConfiguration(){}

    public ServerLoggingConfiguration logging()
    {
        return this.serverLogging;
    }

    public long getIsAliveInterval() {
        return isAliveInterval;
    }
    public void setIsAliveInterval(long isAliveInterval) {
        this.isAliveInterval = isAliveInterval;
    }

    public void setInitializationQueueName(String initializationQueueName) {
        this.initializationQueueName = initializationQueueName;
    }

    public String getStaticHostsQueueName() {
        return staticHostsQueueName;
    }

    public void setStaticHostsQueueName(String staticHostsQueueName) {
        this.staticHostsQueueName = staticHostsQueueName;
    }

    public void setRequestQueueName(String requestQueueName) {
        this.requestQueueName = requestQueueName;
    }

    public void setResponseQueueName(String responseQueueName) {
        this.responseQueueName = responseQueueName;
    }

    public String getCallbackRequestQueueName() {
        return callbackRequestQueueName;
    }

    public void setCallbackRequestQueueName(String callbackRequestQueueName) {
        this.callbackRequestQueueName = callbackRequestQueueName;
    }

    public String getCallbackResponseQueueName() {
        return callbackResponseQueueName;
    }

    public void setCallbackResponseQueueName(String callbackResponseQueueName) {
        this.callbackResponseQueueName = callbackResponseQueueName;
    }
    
    public void setHostInitializationTimeout(long hostInitializationTimeout) {
        this.hostInitializationTimeout = hostInitializationTimeout;
    }

    public long getHostInitializationTimeout()
    {
        return this.hostInitializationTimeout;
    }

    public String getRequestQueueName() {
        return requestQueueName;
    }

    public String getResponseQueueName() {
        return responseQueueName;
    }

    public String getInitializationQueueName() {
        return initializationQueueName;
    }

    public String getStaticFieldWriteRequestQueueName() {
        return staticFieldWriteRequestQueueName;
    }

    public void setStaticFieldWriteRequestQueueName(String staticFieldWriteRequestQueueName) {
        this.staticFieldWriteRequestQueueName = staticFieldWriteRequestQueueName;
    }

    public String getStaticFieldWriteResponseTopicName() {
        return staticFieldWriteResponseTopicName;
    }

    public void setStaticFieldWriteResponseTopicName(String staticFieldWriteResponseTopicName) {
        this.staticFieldWriteResponseTopicName = staticFieldWriteResponseTopicName;
    }

    public String getStaticFieldReadRequestQueueName() {
        return staticFieldReadRequestQueueName;
    }

    public void setStaticFieldReadRequestQueueName(
            String staticFieldReadRequestQueueName) {
        this.staticFieldReadRequestQueueName = staticFieldReadRequestQueueName;
    }

    public String getStaticFieldReadResponsesQueueName() {
        return staticFieldReadResponsesQueueName;
    }

    public void setStaticFieldReadResponsesQueueName(
            String staticFieldReadResponsesQueueName) {
        this.staticFieldReadResponsesQueueName = staticFieldReadResponsesQueueName;
    }

    public long getStaticHostDiscoveryInterval() {
        return staticHostDiscoveryInterval;
    }

    public void setStaticHostDiscoveryInterval(long staticHostDiscoveryInterval) {
        this.staticHostDiscoveryInterval = staticHostDiscoveryInterval;
    }

    public void setCloudPlatform(CloudPlatformConfiguration cloudPlatform)
    {
        this.cloudPlatform = cloudPlatform;
    }

    public CloudPlatformConfiguration cloudPlatform()
    {
        return this.cloudPlatform;
    }

    public long getInvocationTimeout() {
        return invocationTimeout;
    }

    public void setInvocationTimeout(long invocationTimeout) {
        this.invocationTimeout = invocationTimeout;
    }

    @Override
    public ServerConfiguration clone()
    {
        try
        {
            // calling parent class to clone its stuff and make a shallow-copy object.
            ServerConfiguration config = (ServerConfiguration)super.clone();

            return config;
        }
        catch (CloneNotSupportedException e)
        {
            throw new JCloudScaleException(e, "Failed to clone configuration.");
        }
    }
}
