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

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.TimeoutException;
import at.ac.tuwien.infosys.jcloudscale.server.AbstractJCloudScaleServerRunner;
import at.ac.tuwien.infosys.jcloudscale.server.ServerConfiguration;
import at.ac.tuwien.infosys.jcloudscale.utility.SerializationUtil;
import at.ac.tuwien.infosys.jcloudscale.utility.XmlSerializer;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JCloudScaleConfiguration implements Serializable, Cloneable
{
    private static final long serialVersionUID = 1L;
    public static final String CS_VERSION = "0.4.0";

    /**
     * Defines if the configuration should be saved to the same file or additional helper files are allowed.
     */
    private static final boolean SERIALIZE_TO_SAME_FILE = false;
    /**
     * Defines the extension of the descriptor file if additional files are allowed.
     */
    private static final String DESCRIPTOR_EXTENSION = ".cls";

    private static boolean isServerContext = false;

    private String version = CS_VERSION;//version of the platform?
    private CommonConfiguration commonConfiguration =  new CommonConfiguration();
    private ServerConfiguration serverConfiguration = new ServerConfiguration();

    //can't change these values as they should be the same on client and server.
    final static String configurationDeliveryTopic = "CS_ConfigurationDelivery";
    final static UUID clientConfigurationSelector = UUID.fromString("11111111-e29b-41d4-a716-446655440000");

    protected JCloudScaleConfiguration()
    {
    }

    /**
     * Detects if the provided configuration version is compatible with the configuration version known to this host.
     * @param configurationVersion
     * @return
     */
    public static boolean isVersionCompatible(String configurationVersion)
    {
        //now we need strict version conformance.
        return CS_VERSION.equals(configurationVersion);
    }

    public CommonConfiguration common()
    {
        return this.commonConfiguration;
    }

    public ServerConfiguration server()
    {
        return this.serverConfiguration;
    }

    public String getVersion()
    {
        return this.version;
    }

    //----------------------------------------------------------------------

    public static JCloudScaleConfiguration getConfiguration()
    {
        if(isServerContext())
            // XXX AbstractJCloudScaleServerRunner
            //return JCloudScaleServerRunner.getInstance().getConfiguration();
            return AbstractJCloudScaleServerRunner.getInstance().getConfiguration();
        else
            return JCloudScaleClient.getConfiguration();
    }

    public static boolean isServerContext()
    {
        return isServerContext;
    }

    public static void setServerContext(boolean isServerContext)
    {
        JCloudScaleConfiguration.isServerContext = isServerContext;
    }

    private static Logger getLogger(JCloudScaleConfiguration cfg, String loggerName)
    {
        return isServerContext() ?
                cfg.serverConfiguration.logging().getLogger(loggerName):
                    cfg.commonConfiguration.clientLogging().getLogger(loggerName);
    }

    public static Logger getLogger(JCloudScaleConfiguration cfg, Object loggedObject)
    {
        if(loggedObject == null)
            return null;
        if(loggedObject instanceof String)
            return getLogger(cfg, loggedObject.toString());
        else
            if(loggedObject instanceof Class<?>)
                return getLogger(cfg, ((Class<?>)loggedObject).getName());
            else
                return getLogger(cfg, loggedObject.getClass().getName());
    }

    public static Logger getLogger(Object loggedObject)
    {
        return getLogger(getConfiguration(), loggedObject);
    }

    public static IMQWrapper createMQWrapper(JCloudScaleConfiguration cfg) throws NamingException, JMSException
    {
        return cfg.commonConfiguration.communication().newWrapper();
    }

    public static IMQWrapper createMQWrapper(JCloudScaleConfiguration cfg, int deliveryMode) throws NamingException, JMSException
    {
        return cfg.commonConfiguration.communication().newWrapper(deliveryMode);
    }

    public static IMQWrapper createMQWrapper() throws NamingException, JMSException
    {
        return createMQWrapper(getConfiguration());
    }

    public static IMQWrapper createMQWrapper(int deliveryMode) throws NamingException, JMSException
    {
        return createMQWrapper(getConfiguration(), deliveryMode);
    }

    //---------------------------------------------------------

    public void sendConfigurationToStaticHost(UUID serverId) throws NamingException, JMSException, TimeoutException, IOException
    {
        ConfigurationDistributor.sendConfigurationToStaticHost(this, serverId);
    }

    public static Closeable createConfigurationListener(UUID serverId) throws NamingException, JMSException
    {
        return new ConfigurationDistributor.ServerConfigurationListener(serverId);
    }

    //----------------------------------------------------------------------------

    @Override
    public JCloudScaleConfiguration clone()
    {
        try
        {
            // calling parent class to clone its stuff and make a shallow-copy object.
            JCloudScaleConfiguration config = (JCloudScaleConfiguration)super.clone();

            // cloning fields that has to be cloned.
            config.commonConfiguration = commonConfiguration.clone();
            config.serverConfiguration = serverConfiguration.clone();

            return config;
        }
        catch (CloneNotSupportedException e)
        {
            throw new JCloudScaleException(e, "Failed to clone configuration.");
        }
    }

    public byte[] serialize() throws IOException
    {
        return SerializationUtil.serializeToByteArray(this);
    }

    public static JCloudScaleConfiguration deserialize(byte[] serializedConfiguration) throws ClassNotFoundException, IOException
    {
        // we can use remote classloader here as well in case some classes need to be transmitted to the server.
        // the only problem is that we have to figure out which remote classloader to use before having configuration from client
        // (as we're actually deserializing it)
        return (JCloudScaleConfiguration)SerializationUtil.getObjectFromBytes(serializedConfiguration, ClassLoader.getSystemClassLoader());
    }

    public void save(File file) throws IOException
    {
        String filePath = file.getCanonicalPath();
        String descriptorFilePath = SERIALIZE_TO_SAME_FILE ? filePath : filePath+DESCRIPTOR_EXTENSION;
        XmlSerializer.serialize(this, filePath, descriptorFilePath);
    }

    public static JCloudScaleConfiguration load(File file) throws IOException
    {
        if(!file.exists() || !file.isFile())
            throw new FileNotFoundException("Cannot load configuration from file "+file.getCanonicalPath()+": file does not exist.");

        try
        {
            String filePath = file.getCanonicalPath();
            String descriptorFilePath = SERIALIZE_TO_SAME_FILE ? filePath : filePath+DESCRIPTOR_EXTENSION;
            return XmlSerializer.deserialize(filePath, descriptorFilePath, JCloudScaleConfiguration.class);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            throw new JCloudScaleException(ex, "Failed to deserialize configuration.");
        }
    }
}
