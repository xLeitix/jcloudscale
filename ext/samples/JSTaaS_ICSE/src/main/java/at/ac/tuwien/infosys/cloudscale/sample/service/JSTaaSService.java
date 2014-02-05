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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.jws.WebService;

import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestResults;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSetup;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSuite;
import at.ac.tuwien.infosys.jcloudscale.sample.service.exceptions.TestException;

/**
 * Testing web-service implementation.
 */
@WebService
public class JSTaaSService implements IJSTaaSService {
	
	private Logger log;
	
	private ExecutorService executor;
	
	private Map<Integer, TestSuiteExecution> executions = null; 
	private IPlatformDependentCodeFactory executorFactory;
	
	/**
	 * Constructs the new instance of <b>JSTaaSService</b> with the platform-specific code factory provided.
	 * @param executorFactory The platform-specific code factory that is responsible for performing platform-dependent tasks.
	 */
	public JSTaaSService(IPlatformDependentCodeFactory executorFactory) {
		log = Logger.getLogger(JSTaaSService.class.getName());
		log.info("Creating new service instance of "+JSTaaSService.class.getName());
		executor = Executors.newCachedThreadPool();
		executions = new HashMap<>();
		this.executorFactory = executorFactory;
	}
	
	@Override
    public void runTestSuites(TestSetup tests) throws TestException {
		
		TestSuiteExecution statuses = new TestSuiteExecution(tests.getSuites().size());
		
		log.info("Received "+tests.getSuites().size()+" test suites to execute on engine "+tests.getEngine());
		
		executorFactory.createDatabase(tests.getTestId());
		log.info("Wrote result stub to database");
		
		
		int i =0;
		for(TestSuite suite : tests.getSuites()) {
			log.info("Running test suite "+suite.getName());
			executor.execute(executorFactory.createTestExecutor(suite, tests.getEngine(), statuses, tests.getTestId(), i++));
		}
		
		executions.put(tests.getTestId(), statuses);
    }
	
	@Override
	public boolean resultAvailable(int testId) throws TestException {
		log.info("Querying available results for test "+testId);
		if(!executions.containsKey(testId))
			throw new TestException();
		return executions.get(testId).done();
	}

	@Override
	public TestResults getResults(int testsId) throws TestException {
		if(resultAvailable(testsId)) 
		{
			log.info("Returning results for test "+testsId);
			
			TestResults results = executorFactory.getTestResults(testsId);
			executorFactory.deleteTestResults(results);
			
			return results;
		} 
		else 
		{
			return null;
		}
	}
}
