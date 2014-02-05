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
package at.ac.tuwien.infosys.jcloudscale.configuration;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.classLoader.AbstractClassLoaderConfiguration;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.CachingClassLoaderConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.logging.ClientLoggingConfiguration;
import at.ac.tuwien.infosys.jcloudscale.management.CloudscaleUIConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.monitoring.MonitoringConfiguration;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.policy.sample.HostPerObjectScalingPolicy;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class CommonConfiguration implements Serializable, Cloneable
{
    private static final long serialVersionUID = 1L;

    private ClientLoggingConfiguration clientLogging = new ClientLoggingConfiguration();
    private MessageQueueConfiguration communication = new MessageQueueConfiguration();
    private MonitoringConfiguration monitoring = new MonitoringConfiguration();
    private CloudscaleUIConfiguration ui = new CloudscaleUIConfiguration();
    private AbstractClassLoaderConfiguration classloader = new CachingClassLoaderConfiguration();

    private UUID clientId = UUID.randomUUID();

    private AbstractScalingPolicy scalingPolicy = new HostPerObjectScalingPolicy();
    private long scaleDownInterval = 5*60*1000;//5 min?
    private long keepaliveInterval = 30*1000; // 30 secs

    protected CommonConfiguration()
    {
    }

    public UUID clientID()
    {
        return this.clientId;
    }

    public AbstractScalingPolicy scalingPolicy()
    {
        return this.scalingPolicy;
    }

    public void setScalingPolicy(AbstractScalingPolicy scalingPolicy)
    {
        this.scalingPolicy = scalingPolicy;
    }

    public long scaleDownIntervalInSec()
    {
        return scaleDownInterval/1000;
    }

    public void setScaleDownIntervalInSec(long scaleDownIntervalInSec)
    {
        this.scaleDownInterval = scaleDownIntervalInSec*1000;
    }
    
    public long keepAliveIntervalInSec()
    {
        return keepaliveInterval/1000;
    }

    public void setKeepAliveIntervalInSec(long keepaliveIntervalInSec)
    {
        this.keepaliveInterval = keepaliveIntervalInSec*1000;
    }

    public ClientLoggingConfiguration clientLogging()
    {
        return this.clientLogging;
    }

    public MessageQueueConfiguration communication()
    {
        return this.communication;
    }

    public MonitoringConfiguration monitoring()
    {
        return this.monitoring;
    }

    public CloudscaleUIConfiguration UI()
    {
        return this.ui;
    }

    public AbstractClassLoaderConfiguration classLoader()
    {
        return this.classloader;
    }

    public void setClassLoader(AbstractClassLoaderConfiguration classLoader)
    {
        this.classloader = classLoader;
    }

    public void setCommunicationConfiguration(MessageQueueConfiguration communicationConfiguration)
    {
        this.communication = communicationConfiguration;
    }

    //--------------------------SERIALIZATION HELPERS-------------------------------------

    @Override
    public CommonConfiguration clone()
    {
        try
        {
            // calling parent class to clone its stuff and make a shallow-copy object.
            CommonConfiguration config = (CommonConfiguration)super.clone();

            // cloning fields that has to be cloned.
            config.classloader = classloader.clone();
            config.clientLogging = clientLogging.clone();
            config.communication = communication.clone();

            return config;
        }
        catch (CloneNotSupportedException e)
        {
            throw new JCloudScaleException(e, "Failed to clone configuration.");
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        //this field is not necessary on the server side and can be cleaned to avoid classloading issues.
        AbstractScalingPolicy scalingPolicy = this.scalingPolicy;
        this.scalingPolicy = null;
        try
        {
            out.defaultWriteObject();
        }
        finally
        {
            this.scalingPolicy = scalingPolicy;
        }
    }
}
