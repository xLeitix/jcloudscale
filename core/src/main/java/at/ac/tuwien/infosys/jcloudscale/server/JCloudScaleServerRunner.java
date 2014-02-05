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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfigurationBuilder;
import at.ac.tuwien.infosys.jcloudscale.configuration.IConfigurationChangedListener;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.JCloudScaleReferenceManager;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.migration.IMigrationEnabledJCloudScaleHost;
import at.ac.tuwien.infosys.jcloudscale.migration.server.MigrationEnabledJCloudScaleServer;
import at.ac.tuwien.infosys.jcloudscale.server.messaging.JCloudScaleRequestListener;
import at.ac.tuwien.infosys.jcloudscale.server.messaging.MonitoringMQHelper;

public class JCloudScaleServerRunner extends AbstractJCloudScaleServerRunner {

    /**
     * The name of the file from where server tries to load default configuration (used before client provided actual one)
     */
    private static final String CONFIGURATION_FILENAME = "JCloudScaleServerConfig.xml";

    private UUID id;

    private ServerIsAliveSender isalive;
    private Closeable configurationListener;
    private JCloudScaleRequestListener requestListener;
    private JCloudScaleServer server;
    private PrintStream outputRedirectionStream;
    private PrintStream errorRedirectionStream;
    private JCloudScaleReferenceManager referenceManager;

    private JCloudScaleConfiguration configuration;
    private List<IConfigurationChangedListener> configurationChangedListeners = new CopyOnWriteArrayList<>();

    // XXX AbstractJCloudScaleServerRunner
    //	private static JCloudScaleServerRunner instance;

    private Logger log;

    private JCloudScaleServerRunner()
    {
        //specifying configuration. trying to load it from predefined file.
        JCloudScaleConfiguration cfg = null;
        File configFile = new File(CONFIGURATION_FILENAME);

        if(configFile.exists())
        {
            try
            {
                cfg = JCloudScaleConfiguration.load(configFile);
                log = JCloudScaleConfiguration.getLogger(cfg, this);
                log.info("Using configuration from the " +configFile.getAbsolutePath());

            } catch (IOException e)
            {
                e.printStackTrace();//just in case there's some error.
            }
        }

        // in case we could not load configuration, let's use default one.
        if(cfg == null)
        {
            cfg = new JCloudScaleConfigurationBuilder()
            .withLogging(Level.SEVERE)// by default we will just report errors. Until we received the correct configuration.
            .build();
            log = JCloudScaleConfiguration.getLogger(cfg, this);
            log.info("Using default configuration, failed to load configuration from "+configFile.getAbsolutePath());
        }

        setConfiguration(cfg);
    }

    // XXX AbstractJCloudScaleServerRunner
    //	public static JCloudScaleServerRunner getInstance()
    //	{
    //		return instance;
    //	}

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setConfiguration(JCloudScaleConfiguration cfg)
    {
        for(IConfigurationChangedListener listener : configurationChangedListeners)
            listener.onConfigurationChange(cfg);

        this.configuration = cfg;
        JCloudScaleConfiguration.setServerContext(true);
    }

    @Override
    public void registerConfigurationChangeListner(IConfigurationChangedListener listener)
    {
        this.configurationChangedListeners.add(listener);
    }

    @Override
    public JCloudScaleConfiguration getConfiguration()
    {
        return this.configuration;
    }

    public static void main(String[] args) throws NamingException, JMSException, InterruptedException, IOException
    {
        JCloudScaleServerRunner instance = new JCloudScaleServerRunner();
        setInstance(instance);
        instance.run();
    }

    @Override
    protected void run() throws NamingException, JMSException, InterruptedException, UnknownHostException, SocketException
    {
        id = UUID.randomUUID();

        String ip = PlatformSpecificUtil.findBestIP();

        //
        // Discovering configuration.
        //
        try
        {
            //we assume that we have at least any configuration.
            MessageQueueConfiguration mqConfiguration = getConfiguration().common().communication();

            log.info("Instance "+id+" is running. Discovering message queue...");

            while(!mqConfiguration.tryDiscoverMQServer())
            {
                log.severe(String.format("%s failed to discover MQ server. Trying again in a second...%n", id));
                Thread.sleep(1000);
            }

            log.info(id + " discovered MQ server at " + mqConfiguration.getServerAddress()+":"+mqConfiguration.getServerPort());
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            throw new JCloudScaleException(ex, "Failed to discover MQ server. Host is terminating....");
        }

        synchronized (this)
        {
            //
            // In case someone will send us configuration in future, we need to create a general configuration listener
            //
            try
            {
                this.configurationListener = JCloudScaleConfiguration.createConfigurationListener(id);
            }
            catch(Exception ex)
            {
                ex.printStackTrace();//in case we fail, this should not crash the server, we just can't receive configuration from anyone any more.
            }

            // if we should redirect System.out and System.err, we do so now
            try
            {
                if(getConfiguration().server().logging().redirectStdIoToClientConsole())
                {
                    outputRedirectionStream = new PrintStream(new MQOutputStream(false, ip, id), true);
                    System.setOut(outputRedirectionStream);
                }

                if(getConfiguration().server().logging().redirectStdErrToClientConsole())
                {
                    errorRedirectionStream = new PrintStream(new MQOutputStream(true, ip, id), true);
                    System.setErr(errorRedirectionStream);

                    // we have to recreate console handler in server configuration
                    // as old one captured old error stream and won't redirect correctly.
                    getConfiguration().server().logging().recreateConsoleHandler();
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            // create reference manager
            referenceManager = JCloudScaleReferenceManager.getInstance();

            // create JCloudScale server object
            server = new MigrationEnabledJCloudScaleServer();

            //		writeLog("JCloudScaleServer started.");

            // start listening for requests
            requestListener = new JCloudScaleRequestListener((IMigrationEnabledJCloudScaleHost)server, id);

            // start sending out isalive messages
            isalive = new ServerIsAliveSender(id, ip);

            log.info("Instance "+id+" is completely initialized. Waiting for commands.");
        }
    }

    @Override
    public synchronized void shutdown() throws JMSException, NamingException
    {
        isalive.stopSendingIsAliveMessages();
        MonitoringMQHelper.closeInstance();

        List<Closeable> resourcesToClose = new ArrayList<>();
        resourcesToClose.add(this.configurationListener);
        resourcesToClose.add(this.requestListener);
        resourcesToClose.add(this.server);
        resourcesToClose.add(this.outputRedirectionStream);
        resourcesToClose.add(this.errorRedirectionStream);
        resourcesToClose.add(this.referenceManager);

        for(Closeable resource : resourcesToClose)
            if(resource != null)
            {
                try
                {
                    resource.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
    }
}

