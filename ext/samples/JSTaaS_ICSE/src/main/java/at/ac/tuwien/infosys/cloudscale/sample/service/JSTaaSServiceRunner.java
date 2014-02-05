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
package at.ac.tuwien.infosys.jcloudscale.sample.service;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.ws.Endpoint;

import at.ac.tuwien.infosys.jcloudscale.sample.service.jcloudscale.JCloudScaleCodeFactory;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.OpenstackCodeFactory;

/**
 * Main entry point of the service application. Starts a web-service and waits for requests to execute.
 */
public class JSTaaSServiceRunner {
	
	public static final String CLOUDSCALE = "jcloudscale";
	public static final String OPENSTACK = "openstack";

	/**
	 * Application entry point. Starts a web-service and waits for requests to execute.
	 */
	public static void main(String[] args) throws IllegalArgumentException, NullPointerException, IOException {
		
		IPlatformDependentCodeFactory executorFactory = createExecutorFactory(args);
		if(executorFactory == null)
			throw new NullPointerException("Failed to construct platform-dependent code factory. Application parameters are incorrect.");
		
		JSTaaSService implementor = new JSTaaSService (executorFactory);
		String address = "http://0.0.0.0:9000/jstaas";
		Endpoint.publish(address, implementor);
	}

	/**
	 * Selects the appropriate executor factory basing on the command-line arguments.
	 * @param args Command-line arguments provided to the app.
	 * @return
	 */
	private static IPlatformDependentCodeFactory createExecutorFactory(String[] args) 
	{
		Logger log = Logger.getLogger(JSTaaSServiceRunner.class.getName());
		
		IPlatformDependentCodeFactory result = null;
		
		try
		{
			if(args.length > 0)
			{
				log.info("Argument \""+args[0]+"\" provided, selecting appropriate executor factory.");
				switch(args[0].toLowerCase())
				{
					case CLOUDSCALE:
						return result = new JCloudScaleCodeFactory();
					case OPENSTACK:
						return result = new OpenstackCodeFactory();
					default:
						log.warning("Could not parse arguments, unknown option "+args[0]+".");
						return result;
				}
			}
			else
			{
				log.info("No args provided, using default executor factory.");
				return result;
			}
		}
		finally
		{
			log.info(result + " was selected.");
		}
	}

}
