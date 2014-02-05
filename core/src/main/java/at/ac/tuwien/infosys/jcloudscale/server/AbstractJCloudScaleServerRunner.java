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

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.IConfigurationChangedListener;

/**
 * Abstract superclass for JCloudScaleServerRunners to eliminate static
 * references to {@link #getInstance()}, so that different implementations of
 * JCloudScaleServerRunner can be used.
 */
public abstract class AbstractJCloudScaleServerRunner {

	private static AbstractJCloudScaleServerRunner instance;

	protected static void setInstance(AbstractJCloudScaleServerRunner instance) {
		AbstractJCloudScaleServerRunner.instance = instance;
	}

	public static AbstractJCloudScaleServerRunner getInstance() {
		return instance;
	}

	public abstract UUID getId();

	public abstract void setConfiguration(JCloudScaleConfiguration cfg);

	public abstract JCloudScaleConfiguration getConfiguration();

	public abstract void registerConfigurationChangeListner(IConfigurationChangedListener listener);

	// Used as pointcut in StateEventAspect
	public abstract void shutdown() throws JMSException, NamingException;

	// Used as pointcut in StateEventAspect
	protected abstract void run() throws NamingException, JMSException, InterruptedException,
			UnknownHostException, SocketException;

}
