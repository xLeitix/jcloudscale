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
package at.ac.tuwien.infosys.jcloudscale.sample.service.jcloudscale;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.DataSource;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.sample.service.IPlatformDependentCodeFactory;
import at.ac.tuwien.infosys.jcloudscale.sample.service.Stopwatch;
import at.ac.tuwien.infosys.jcloudscale.sample.service.TestSuiteExecution;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestResults;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSuite;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.OpenstackConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IdManager;

/**
 * Defines the jcloudscale-specific code in a manner that allows to integrate with the generic web-serivce.
 */
public class JCloudScaleCodeFactory implements IPlatformDependentCodeFactory 
{
	static int staticHostsCount;
	private Logger log = Logger.getLogger(this.getClass().getName());
	
	/**
	 * Defines the database access object that will be inserted by jcloudscale datastore library. 
	 */
	@DataSource(name = "testresults")
	private Datastore datastore;
	
	/**
	 * Stopwatch that measures complete user test setup execution (all test suites submitted by a user)
	 */
	private Stopwatch parentStopwatch;
	
	public JCloudScaleCodeFactory()
	{
		// initializing JCloudScale.
		JCloudScaleClient.getClient();
		try(IdManager idManager = new IdManager(JCloudScaleClient.getConfiguration().common().communication()))
		{
			//
			// determining how much static instances we're running on.
			// As this test is focused on static overhead measurement, only static instances should be used. 
			//
			staticHostsCount = idManager.getRegisteredInstances().size();
			if(staticHostsCount == 0)
				System.out.println("   ---==== WARNING: NO STATIC HOSTS DETECTED!!!!! ====---");
			else
				System.out.println("..... JCloudScale detected "+staticHostsCount+" static hosts ....");
		}
	}
	
	@Override
	public Runnable createTestExecutor(TestSuite suite, String engine,
			TestSuiteExecution statuses, int testNr, int suiteNr) 
	{
		return new TestSuiteExecutor(suite, engine, statuses, testNr, suiteNr);
	}
	
	@Override
	public void createDatabase(int testsId)
	{
		ensureDatabaseExists();
		
		TestResults results = new TestResults();
		results.setTestsId(String.valueOf(testsId));
		datastore.save(results);
				
		String filename ="eval_jcloudscale_"+staticHostsCount+".csv";
		log.info("JCloudScale evaluation results will be saved to "+filename);
		parentStopwatch = new Stopwatch(filename).withMessage("TOTAL").withPrecision(TimeUnit.MILLISECONDS);
	}

	private void ensureDatabaseExists() 
	{
		//assuming database is on localhost
		try
		{
			log.info("Ensuring database is ready to work...");
			String couchServer = OpenstackConfiguration.getCouchdbHostname();
			int couchPort = OpenstackConfiguration.getCouchdbPort();
			
			String databaseName = OpenstackConfiguration.getCouchdbDatabase();
			Process process = Runtime.getRuntime().exec("curl -X DELETE http://"+couchServer+":"+couchPort+"/"+databaseName+"/");
			
			process.waitFor();
			
			process = Runtime.getRuntime().exec("curl -X PUT http://"+couchServer+":"+couchPort+"/"+databaseName+"/");
			process.waitFor();
		}
		catch(IOException | InterruptedException ex)
		{
			ex.printStackTrace();
		}
	}

	@Override
	public TestResults getTestResults(int testsId) 
	{
		return datastore.find(TestResults.class, Integer.toString(testsId));
	}

	@Override
	public void deleteTestResults(TestResults testResults) 
	{
		datastore.deleteById(TestResults.class, testResults.getTestsId());
		
		parentStopwatch.close();
		parentStopwatch = null;
	}
	
	/**
	 * Test executor that wraps jcloudscale method execution.
	 */
	private class TestSuiteExecutor implements Runnable {
		
		private TestSuite suite;
		private String engine;
		private TestSuiteExecution statuses;
		private int testNr;
		private int suiteNr;
		
		public TestSuiteExecutor(TestSuite suite, String engine,
				TestSuiteExecution statuses, int testNr, int suiteNr) {
			this.suite = suite;
			this.engine = engine;
			this.statuses = statuses;
			this.testNr = testNr;
			this.suiteNr = suiteNr;
		}

		@Override
		public void run() {
		
			//
			// Creating the new cloud object.
			// At this point scaling policy will be invoked and cloud host will be selected
			// to host this object. This application will work only with a stub that allows
			// to schedule methods execution on the remote host.
			//
			TestSuiteCloudObject co = new TestSuiteCloudObject(engine);
			log.info("Deployed cloud object for suite "+suite.getName()+", cloud object ID is "+co.getId().toString());
			log.info("Running on physical host "+getHostIPtoCloudId(co.getId()));
			
			//
			// Remote method execution to transfer parameters to the remote host.
			// As parameters are transferred by-value, they will be serialized and 
			// transferred to the remote host.
			//
			co.setSuite(suite, testNr);
			log.info("Starting cloud object");
			
			//
			// Measuring the separate long-running method execution to estimate 
			// the separate method call overhead of the jcloudscale.
			//
			try(Stopwatch stopwatch = new Stopwatch(parentStopwatch)
											.withMessage(String.format("Suite %03d", suiteNr))
											.withPrecision(TimeUnit.MILLISECONDS)) 
			{
				//
				// parameters to this method are transferred by-reference,
				// what will cause callbacks to execute during this method call.
				//
				co.runCloudObject(statuses, suiteNr);
			} catch(JCloudScaleException e) {
				log.severe("Cloud object invocation failed with:");
				e.printStackTrace();
			}
		}
		
		private String getHostIPtoCloudId(UUID coId) {
			
			for(IVirtualHost host : CloudManager.getInstance().getHosts()) {
				for(ClientCloudObject obj : host.getCloudObjects()) {
					if(obj.getId() != null && obj.getId().equals(coId))
						return host.getIpAddress();
				}
			}
			return "unknown";
			
		}
		
	}

	
}
