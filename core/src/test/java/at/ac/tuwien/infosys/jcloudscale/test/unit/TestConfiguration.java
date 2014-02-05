/*
   Copyright 2014 Philipp Leitner 

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
package at.ac.tuwien.infosys.jcloudscale.test.unit;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.infosys.jcloudscale.classLoader.SystemClassLoaderConfiguration;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.fileCollectors.FileCollectorAbstract;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfigurationBuilder;
import at.ac.tuwien.infosys.jcloudscale.configuration.IConfigurationChangedListener;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.policy.sample.SingleHostScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.server.AbstractJCloudScaleServerRunner;
import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;

public class TestConfiguration
{
    private static final String CD_PROPERTY = "user.dir";
    private String correctCurrentDirectory = null;

    @Before
    public void setup() throws Exception
    {
        correctCurrentDirectory = System.getProperty(CD_PROPERTY);
        System.setProperty(CD_PROPERTY, new File(correctCurrentDirectory, "target").getAbsolutePath());
    }

    @After
    public void cleanup() throws Exception
    {
        if(correctCurrentDirectory != null)
            System.setProperty(CD_PROPERTY, correctCurrentDirectory);
    }

    @Test
    public void testDefaultConfiguration() throws Exception
    {
        JCloudScaleConfiguration config = ConfigurationHelper.createDefaultTestConfiguration().build();

        testConfiguration(config);
    }

    @Test
    public void testTrickyConfiguration() throws Exception
    {
        JCloudScaleConfiguration config = new JCloudScaleConfigurationBuilder()
        .with(new SystemClassLoaderConfiguration())
        .with(new SingleHostScalingPolicy())
        .withCommunicationServerPublisher(true)
        .withLogging(Level.ALL)
        .withLoggingCustom(this.getClass(), Level.ALL)
        .withMonitoring(true)
        .withMQServerPort(60000)
        .withRedirectAllOutput(false)
        .withScaleDownIntervalInSec(5)
        .withUI(false)
        .build();

        testConfiguration(config);
    }

    //--------------------------------HELPERS------------------------------------------

    private void testConfiguration(JCloudScaleConfiguration config) throws Exception
    {
        try
        {
            JCloudScaleClient.setConfiguration(config);

            ensureMainActionsWork(config);

            testCloning(config);

            traverseConfiguration(config);

            ensureSerializationWorks(config);
        }
        finally
        {
            JCloudScaleClient.setConfiguration(null);
        }
    }

    private void testCloning(JCloudScaleConfiguration config) throws Exception
    {
        JCloudScaleConfiguration copy = config.clone();
        JCloudScaleConfiguration copy2 = config.clone();

        deepEquals(config, copy);

        // messing up with the clone.
        copy.common().setScalingPolicy(null);
        copy.common().setClassLoader(null);
        copy.common().setCommunicationConfiguration(null);
        copy.common().clientLogging().setParentLoggerName(null);

        copy.server().setCloudPlatform(null);
        copy.server().setRequestQueueName(null);
        copy.server().setStaticFieldReadRequestQueueName(null);

        //ensuring config is still correct
        traverseConfiguration(config);
        deepEquals(copy2, config);
    }

    private void ensureMainActionsWork(JCloudScaleConfiguration config) throws Exception
    {
        //
        // obtaining logger.
        //
        Logger logger = JCloudScaleConfiguration.getLogger(config, this);
        logger.fine("Should work nicely.");

        AutoCloseable mqServer = null;
        try
        {
            //
            //starting MQ server
            //
            if(config.common().communication().getStartMessageQueueServerAutomatically())
                mqServer = config.server().cloudPlatform().ensureCommunicationServerRunning();

            //
            // Testing multicast publisher
            //
            if(config.common().communication().startMulticastPublisher())
                try(Closeable mqPublisher = config.common().communication().createServerPublisher())
                {
                }

            //
            // Checking class loader/class provider
            //
            ClassLoader classLoader = config.common().classLoader().createClassLoader();
            try(Closeable classProviedr = config.common().classLoader().createClassProvider())
            {
                classLoader.loadClass(this.getClass().getName());

                try
                {
                    classLoader.loadClass("rst.test.class.not.existing");
                    throw new Exception("We accidentally loaded unexisting class!");
                }
                catch(ClassNotFoundException ex)
                {
                    //that's ok.
                }
            }
            finally
            {
                if(!classLoader.equals(ClassLoader.getSystemClassLoader()))
                    if(classLoader instanceof Closeable)
                        ((Closeable)classLoader).close();
            }

            //
            // creating MQ wrapper
            //
            try(Closeable mq = JCloudScaleConfiguration.createMQWrapper(config))
            {
            }

            //
            // Testing configuration transmission
            //
            UUID id =UUID.randomUUID();
            CountDownLatch latch = new CountDownLatch(1);
            try(Closeable listener = JCloudScaleConfiguration.createConfigurationListener(id);
                JCloudScaleServerRunnerStub jcloudscaleServer = new JCloudScaleServerRunnerStub(id, latch))
            {
                config.sendConfigurationToStaticHost(id);
                
                latch.await(1, TimeUnit.SECONDS);//should be enough.
                
                JCloudScaleConfiguration loadedConfiguration = jcloudscaleServer.getConfiguration();
                //repairing configuration.
                JCloudScaleConfiguration.getLogger(loadedConfiguration, this);
                
                deepEquals(config, loadedConfiguration);
            }
        }
        finally
        {
            if(mqServer != null)
                mqServer.close();
        }
    }

    private void ensureSerializationWorks(JCloudScaleConfiguration config) throws Exception
    {
        File file = File.createTempFile("csConfigTest", ".xml");
        try
        {
            config.save(file);

            JCloudScaleConfiguration loadedConfig = JCloudScaleConfiguration.load(file);

            //fixing some small differences
            JCloudScaleConfiguration.getLogger(loadedConfig, this);

            traverseConfiguration(loadedConfig);

            deepEquals(config, loadedConfig);
        }
        finally
        {
            if(!file.delete())
                file.deleteOnExit();
            //deleting class description file (created during serialization)
            File dscFile = new File(file.getCanonicalPath()+".cls");
            if(!dscFile.exists())
                throw new Exception("Could not find description file after serialization! Either serialization is not working or naming pattern changed.");
            if(!dscFile.delete())
                dscFile.deleteOnExit();
        }
    }

    private static void traverseConfiguration(Object obj) throws Exception
    {
        if(!isCheckableClass(obj))
            return;

        for(Field field : obj.getClass().getDeclaredFields())
        {
            if(!field.isAccessible())
                field.setAccessible(true);

            Object childObj = field.get(obj);

            if(childObj == null)
            {
                if(!canBeNull(field))
                    throw new Exception("Field "+field.getName()+" of type "+field.getType().getName()+" on object of class "+obj.getClass().getName()+" is NULL.");
            }
            else
                traverseConfiguration(childObj);
        }
    }

    private static boolean canBeNull(Field field)
    {
        String name = field.getName();
        return name.contains("formatterClass") ||
                name.contains("scalingPolicy") ||
                name.contains("fileCollector");
    }

    private static void deepEquals(Object obj1, Object obj2) throws Exception
    {
        if(obj1 == null || obj2 == null)
            if(obj1 == null && obj2 == null)
                return;
            else
            {
                Class<?> clazz = obj1 == null ? obj2.getClass(): obj1.getClass();
                if(!canBeNonEquals(clazz))
                    throw new Exception("Deep Equals Failed! One of the objects is null! Obj1 = "+obj1 +" Obj2="+obj2);
                return;
            }

        if(obj1 == obj2)
            return;//they are the same objects.

        if(!isCheckableClass(obj1))
        {
            if(!obj1.equals(obj2) && !canBeNonEquals(obj1.getClass()))
                throw new Exception("Deep Equals Failed! Non-JCloudScale objects are not equal! Obj1 = "+obj1 +" Obj2="+obj2);

            return;
        }

        if(!obj1.getClass().equals(obj2.getClass()))
            throw new Exception("Deep Equals Failed! Types are different! Obj1 = "+obj1.getClass().getName() +" Obj2="+obj2.getClass().getName());

        for(Field field : obj1.getClass().getDeclaredFields())
        {
            if(!field.isAccessible())
                field.setAccessible(true);

            Object childObj1 = field.get(obj1);
            Object childObj2 = field.get(obj2);
            try
            {
                deepEquals(childObj1, childObj2);
            }
            catch(Exception ex)
            {
                throw new Exception("Detected unequal field \""+field.getName()+"\" in object of class \""+obj1.getClass().getName()+"\".", ex);
            }
        }
    }

    private static boolean canBeNonEquals(Class<?> clazz)
    {
        //todo: maybe we will need to get rid of this rule...
        if(AbstractScalingPolicy.class.isAssignableFrom(clazz) ||
                FileCollectorAbstract.class.isAssignableFrom(clazz))
            return true;

        String name = clazz.getName();
        return name.contains("java.util.logging.ConsoleHandler") ||
                name.contains("java.util.logging.SimpleFormatter")||
                name.contains("java.util.ArrayList");
    }

    private static boolean isCheckableClass(Object obj)
    {
        return obj.getClass().getName().toLowerCase().contains("infosys") ||
                obj.getClass().getName().toLowerCase().contains("jcloudscale");
    }
    
    private static class JCloudScaleServerRunnerStub extends AbstractJCloudScaleServerRunner implements Closeable
    {
        private UUID id;
        private CountDownLatch latch;
        private JCloudScaleConfiguration config;
        
        private JCloudScaleServerRunnerStub (UUID id, CountDownLatch latch)
        {
            this.id = id;
            this.latch = latch;
            AbstractJCloudScaleServerRunner.setInstance(this);
        }
        
        @Override
        public UUID getId() {
            return this.id;
        }

        @Override
        public void setConfiguration(JCloudScaleConfiguration cfg) {
            this.config = cfg;
            latch.countDown();
        }

        @Override
        public JCloudScaleConfiguration getConfiguration() {
           return this.config;
        }

        @Override
        public void registerConfigurationChangeListner(IConfigurationChangedListener listener) {
        }

        @Override
        public void shutdown() throws JMSException, NamingException {
            close();
        }

        @Override
        protected void run() throws NamingException, JMSException,
                InterruptedException, UnknownHostException, SocketException {
        }

        @Override
        public void close() {
               AbstractJCloudScaleServerRunner.setInstance(null);
               this.config = null;
        }
    }
}
