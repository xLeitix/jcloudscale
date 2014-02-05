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
package at.ac.tuwien.infosys.jcloudscale.cli;

import java.io.IOException;
import java.util.logging.Level;

import javax.jms.JMSException;
import javax.naming.NamingException;

import asg.cliche.ShellFactory;
import at.ac.tuwien.infosys.jcloudscale.cli.demoapp.DemoScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfigurationBuilder;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;

public class CLI {

	public static void main(String[] args) throws IOException, NamingException, JMSException 
	{
		ShellFactory.createConsoleShell("jcloudscale", "... welcome to the JCloudScale commandline interface ...",
				new CLIBackend()).commandLoop();
	}
	
	/**
	 * This is the main configuration accessor object for the CLI. everyone gets configuration from here.
	 * @return
	 */
	public static JCloudScaleConfiguration getConfiguration()
	{
		JCloudScaleConfiguration cfg = JCloudScaleClient.getConfiguration();
		if(cfg == null)
		{
			cfg = loadConfiguration();
			JCloudScaleClient.setConfiguration(cfg);
		}
		
		return cfg;
	}

	private static JCloudScaleConfiguration loadConfiguration() 
	{
		JCloudScaleConfiguration cfg = new JCloudScaleConfigurationBuilder()
												.withLogging(Level.INFO)
												.with(new DemoScalingPolicy())
												.build();
		
		cfg.common().communication().setRequestTimeout(15000);//as it was set in CLIBackend.
		
		return cfg;
	}

}
