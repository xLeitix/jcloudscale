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
package at.ac.tuwien.infosys.jcloudscale.logging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.utility.ReflectionUtil;

/**
 * Defines the configuration of the client-side logging infrastructure.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ClientLoggingConfiguration implements Serializable, Cloneable
{
    private static final long serialVersionUID = 1L;

    protected Map<String, String> customLoggingLevels = new HashMap<>();
    protected String defaultLoggingLevel = Level.SEVERE.getName();
    protected String parentLoggerName = "at.ac.tuwien.infosys.jcloudscale";
    protected List<String> handlers = new ArrayList<>();
    protected String formatterClass = null;
    protected long logMessageTimeout = 2000;//determines old messages that have to be discarded.

    protected transient Logger parentLogger;

    /**
     * Creates default instance of the logging configuration with default parameters.
     */
    public ClientLoggingConfiguration()
    {
        addHandler(ConsoleHandler.class);
    }

    /**
     * Sets the custom logging level for a specified logger.
     * @param logger The name of the logger that should have custom configuration
     * @param level The custom level assigned to this logger
     */
    public synchronized void setCustomLoggingLevel(String logger, Level level)
    {
        customLoggingLevels.put(logger, level.getName());
    }

    /**
     * Removes custom logging level for the specified logger.
     * @param logger The name of the logger that should not use custom configuration any more.
     */
    public synchronized void removeCustomLoggingLevel(String logger)
    {
        customLoggingLevels.remove(logger);
    }

    /**
     * Sets the default logging level for all loggers that have no custom level explicitly set.
     * @param defaultLoggingLevel The level that should be used by default.
     */
    public void setDefaultLoggingLevel(Level defaultLoggingLevel) {
        this.defaultLoggingLevel = defaultLoggingLevel.getName();
    }

    /**
     * Sets the name of the parent logger for all loggers created from this configuration.
     * @param parentLoggerName
     */
    public void setParentLoggerName(String parentLoggerName) {
        this.parentLoggerName = parentLoggerName;
    }

    /**
     * Adds logger output handler that will handle output of all loggers created.
     * @param handlerClass The class that implements <b>Handler</b> and has to handle output of the loggers.
     */
    public synchronized void addHandler(Class<? extends Handler> handlerClass)
    {
        if(handlerClass == null)
            return;

        this.handlers.add(handlerClass.getName());
    }

    /**
     * Gets all handlers currently configured to handle logging output.
     */
    protected List<Handler> getHandlers()
    {
        List<Handler> realHandlers = new ArrayList<>();

        for(String handler : this.handlers)
        {
            try
            {
                realHandlers.add((Handler)ReflectionUtil.newInstance(Class.forName(handler)));
            }
            catch(ClassNotFoundException ex)
            {
            }
        }

        return realHandlers;
    }

    /**
     * Sets logging output formatter providing class.
     * @param formatterClass The class that has to format logging messages.
     */
    public void setFormatter(Class<? extends Formatter> formatterClass) {
        this.formatterClass = formatterClass.getName();
    }

    /**
     * Gets currently configured formatter.
     * @return Returns the instance of the formatter class.
     */
    protected Formatter getFormatter()
    {
        if(this.formatterClass == null || formatterClass.isEmpty())
            return null;

        try
        {
            return (Formatter)ReflectionUtil.newInstance(Class.forName(this.formatterClass));
        } catch (ClassNotFoundException e)
        {
            return null;
        }
    }

    /**
     * Determines the deadline after which messages will be just dropped.
     */
    public long getLogMessageTimeout() {
        return logMessageTimeout;
    }
    /**
     * Determines the deadline after which messages will be just dropped.
     * @param logMessageTimeout The timeout in milliseconds that specifies the delay when messages should be
     * dropped instead of printing to the output.
     */
    public void setLogMessageTimeout(long logMessageTimeout) {
        this.logMessageTimeout = logMessageTimeout;
    }

    //------------------------------------------------

    private Level getLevelForLogger(String loggerName)
    {
        synchronized (this)
        {
            if(this.customLoggingLevels.containsKey(loggerName))
                return Level.parse(this.customLoggingLevels.get(loggerName));
        }

        return Level.parse(defaultLoggingLevel);
    }

    protected synchronized Logger getParentLogger()
    {
        if(this.parentLogger == null)
        {
            this.parentLogger = Logger.getLogger(parentLoggerName);
            this.parentLogger.setLevel(getLevelForLogger(parentLoggerName));
            this.parentLogger.setUseParentHandlers(false);

            Formatter formatter = getFormatter();

            if(this.handlers != null)
            {
                // removing existing handlers
                for(Handler handler : this.parentLogger.getHandlers())
                    this.parentLogger.removeHandler(handler);

                // adding required handlers.
                for(Handler handler : getHandlers())
                {
                    handler.setLevel(Level.ALL);
                    if(formatter != null)
                        handler.setFormatter(formatter);

                    this.parentLogger.addHandler(handler);
                }
            }
            else
            {
                if(formatter != null)
                {
                    Handler[] handlers = this.parentLogger.getHandlers();
                    if(handlers != null)
                        for(Handler handler : handlers)
                        {
                            handler.setFormatter(formatter);
                            handler.setLevel(Level.ALL);
                        }
                }
            }
        }

        return parentLogger;
    }

    /**
     * Provides the logger for the specified name with the configured configuration.
     * @param loggerName The name of the logger to provide.
     * @return The configured logger with the specified name.
     */
    public Logger getLogger(String loggerName)
    {
        if(loggerName.equals(parentLoggerName))
            return getParentLogger();

        Logger log = Logger.getLogger(loggerName);

        log.setParent(getParentLogger());
        log.setUseParentHandlers(true);
        log.setLevel(getLevelForLogger(loggerName));

        return log;
    }

    @Override
    public synchronized ClientLoggingConfiguration clone()
    {
        try
        {
            // calling parent class to clone its stuff and make a shallow-copy object.
            ClientLoggingConfiguration config = (ClientLoggingConfiguration)super.clone();

            // cloning fields that has to be cloned.
            config.customLoggingLevels = new HashMap<>(customLoggingLevels);
            config.handlers = new ArrayList<>(handlers);

            return config;
        }
        catch (CloneNotSupportedException e)
        {
            throw new JCloudScaleException(e, "Failed to clone configuration.");
        }
    }
}
