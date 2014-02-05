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

import java.util.logging.Level;

import at.ac.tuwien.infosys.jcloudscale.classLoader.AbstractClassLoaderConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.Event;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.localVm.LocalCloudPlatformConfiguration;

/**
 *	Facade class that allows to build <b>JCloudScaleConfiguration</b> easier and more readable.
 */
public class JCloudScaleConfigurationBuilder
{
    private JCloudScaleConfiguration cfg;

    //-------------------------------------------

    /**
     * Creates a new instance of <b>JCloudScaleConfigurationBuilder</b> with
     * <b>LocalCloudPlatformConfiguration</b>.
     */
    public JCloudScaleConfigurationBuilder()
    {
        this.cfg = new JCloudScaleConfiguration();
        this.cfg.server().setCloudPlatform(new LocalCloudPlatformConfiguration());
    }

    /**
     * Creates a new instance of <b>JCloudScaleConfigurationBuilder</b> with
     * specified cloud platform configuration.
     * @param cloudPlatform The desired cloud platform configuration.
     */
    public JCloudScaleConfigurationBuilder(CloudPlatformConfiguration cloudPlatform)
    {
        this();
        with(cloudPlatform);
    }

    /**
     * Provides access to the configuration that is being built.
     * @return The instance of the <b>JCloudScaleConfiguration</b> that is being built.
     */
    public JCloudScaleConfiguration build()
    {
        return this.cfg;
    }

    /**
     * Allows specifying scaling policy that defines how new cloud objects will be mapped to the cloud hosts.
     * @param scalingPolicy The scaling policy to be used in builded configuration.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder with(AbstractScalingPolicy scalingPolicy)
    {
        this.cfg.common().setScalingPolicy(scalingPolicy);
        return this;
    }

    /**
     * Allows specifying class loader configuration of the server.
     * @param classLoaderConfiguration The class loader configuration to be used on the cloud hosts.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder with(AbstractClassLoaderConfiguration classLoaderConfiguration)
    {
        this.cfg.common().setClassLoader(classLoaderConfiguration);
        return this;
    }

    /**
     * Allows specifying cloud platform configuration.
     * @param cloudPlatform The instance of cloud platform configuration that specifies the platform-specific host management behavior.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder with(CloudPlatformConfiguration cloudPlatform)
    {
        this.cfg.server().setCloudPlatform(cloudPlatform);
        return this;
    }

    /**
     * Allows specifying hostname and port of the Message Queue server.
     * @param hostname The ip address or hostname of the Message Queue server.
     * @param port The port of the Message Queue server
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withMQServer(String hostname, int port)
    {
        return withMQServerHostname(hostname).withMQServerPort(port);
    }

    /**
     * Allows specifying hostname of the Message Queue server
     * @param hostname The ip address or hostname of the Message Queue server.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withMQServerHostname(String hostname)
    {
        this.cfg.common().communication().setServerAddress(hostname);
        return this;
    }

    /**
     * Allows specifying the port of the Message Queue server.
     * @param port The port of the Message Queue server.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withMQServerPort(int port)
    {
        this.cfg.common().communication().setServerPort(port);
        return this;
    }

    /**
     * Allows specifying whether Message Queue server should be started automatically by the JCloudScale
     * in case it is not running or not accessible.
     * @param startServer <b>true</b> if the server has to be started in case it is not available. Oterwise, <b>false</b>.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withMQStartServerAutomatically(boolean startServer)
    {
        this.cfg.common().communication().setStartMessageQueueServerAutomatically(startServer);
        return this;
    }

    /**
     * Allows specifying whether monitoring features of the JCloudScale should be enabled or not.
     * @param monitoringEnabled <b>true</b> if the monitoring should be enabled. Otherwise, <b>false</b>.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withMonitoring(boolean monitoringEnabled)
    {
        this.cfg.common().monitoring().setEnabled(monitoringEnabled);
        return this;
    }

    /**
     * Allows specifying whether multicast publisher should be started within this application instance to
     * allow discovering communication server (Message Queue Server) on non-default ports.
     * @param startPublisher <b>true</b> if the multicast publisher has to be started. Otherwise, <b>false</b>.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withCommunicationServerPublisher(boolean startPublisher)
    {
        this.cfg.common().communication().setStartMulticastPublisher(startPublisher);
        return this;
    }

    /**
     * Allows specifying the level of client-side and server-side logging.
     * @param level The minimal level of logging to be printed to logging stream.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withLogging(Level level)
    {
        return withLoggingClient(level).withLoggingServer(level);
    }

    /**
     * Allows specifying the level of client-side logging.
     * @param level The minimal level of logging to be printed to the log handling stream.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withLoggingClient(Level level)
    {
        this.cfg.common().clientLogging().setDefaultLoggingLevel(level);
        return this;
    }

    /**
     * Allows specifying the level of server-side logging.
     * @param level The minimal level of logging to be printed to the log handling stream.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withLoggingServer(Level level)
    {
        this.cfg.server().logging().setDefaultLoggingLevel(level);
        return this;
    }

    /**
     * Allows specifying logging level for custom logger.
     * @param loggerName The name of the logger that should have custom logging level.
     * @param level The minimal level of logging to be printed to the log handling stream.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withLoggingCustom(String loggerName, Level level)
    {
        this.cfg.server().logging().setCustomLoggingLevel(loggerName, level);
        this.cfg.common().clientLogging().setCustomLoggingLevel(loggerName, level);

        return this;
    }

    /**
     * Allows specifying logging level for custom logger named as the provided class.
     * @param clazz The class which name indicates the logger that needs to have different logging level.
     * @param level The minimal level of logging to be printed to the log handling stream.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withLoggingCustom(Class<?> clazz, Level level)
    {
        return withLoggingCustom(clazz.getName(), level);
    }

    /**
     * Allows specifying how often scaling down method of the scaling policy will be invoked for each host.
     * @param scaleDownIntervalInSec The positive long value that indicates the amount of seconds between scale down method invocations.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withScaleDownIntervalInSec(long scaleDownIntervalInSec)
    {
        this.cfg.common().setScaleDownIntervalInSec(scaleDownIntervalInSec);
        return this;
    }

    /**
     * Allows registering external monitoring events to be used within monitoring functionality of JCloudScale.
     * @param eventClasses The collection of classes to be registered within monitoring engine.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    @SafeVarargs //had to make it final to be able to use SafeArgs..
    public final JCloudScaleConfigurationBuilder withMonitoringEvents(Class<? extends Event>... eventClasses)
    {
        for(Class<? extends Event> event : eventClasses)
            this.cfg.common().monitoring().addMonitoredEvent(event);
        return this;
    }

    /**
     * Allows specifying whether UI of JCloudScale is enabled or not
     * (it can be accessed from code or from CmdClient extension)
     * @param enabled <b>true</b> if UI interface should be enabled and waiting for connections. Otherwise, <b>false</b>.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withUI(boolean enabled)
    {
        this.cfg.common().UI().isEnabled(enabled);
        return this;
    }

    /**
     * Allows specify whether the output from the server (standard and error) should be redirected to the clients appropriate output or not.
     * @param redirect <b>true</b> if both, standard and error, outputs should be redirected to the
     * client standard and error outputs appropriately. Otherwise, <b>false</b>.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withRedirectAllOutput(boolean redirect)
    {
        return withRedirectStdOutput(redirect).withRedirectStdErrorOutput(redirect);
    }

    /**
     * Allows specify whether the standard output from the server should be redirected to the clients standard output or not.
     * @param redirect <b>true</b> if standard output should be redirected to the
     * client standard output. Otherwise, <b>false</b>.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withRedirectStdOutput(boolean redirect)
    {
        this.cfg.server().logging().setRedirectStdIoToClientConsole(redirect);
        return this;
    }

    /**
     * Allows specify whether the error output from the server should be redirected to the clients error output or not.
     * @param redirect <b>true</b> if error output should be redirected to the
     * client error output. Otherwise, <b>false</b>.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withRedirectStdErrorOutput(boolean redirect)
    {
        this.cfg.server().logging().setRedirectStdErrToClientConsole(redirect);
        return this;
    }

    /**
     * Allows to specify the invocation timeout for all requests from client to cloud hosts.
     * @param timeoutInMsec The amount of milliseconds that any request should take.
     * @return The current instance of <b>JCloudScaleConfigurationBuilder</b> to allow continuing configuration.
     */
    public JCloudScaleConfigurationBuilder withInvocationTimeout(long timeoutInMsec)
    {
        this.cfg.server().setInvocationTimeout(timeoutInMsec);
        return this;
    }
}
