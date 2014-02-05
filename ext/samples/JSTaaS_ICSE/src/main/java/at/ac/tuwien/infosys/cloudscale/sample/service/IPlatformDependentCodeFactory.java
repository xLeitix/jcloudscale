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

import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestResults;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSuite;

/**
 * Provides platform-dependent code for web-service.
 */
public interface IPlatformDependentCodeFactory 
{
	/**
	 * Creates platform-specific test executor with specified parameters
	 * @param suite Test suite that has to be executed by this executor
	 * @param engine The engine name that has to be used
	 * @param statuses The shared object where task execution process should be shown
	 * @param testNr The id of the test suite that specifies the id in database where execution log has to be stored
	 * @param suiteNr the position in test suite execution array where status of current execution has to be reflected.
	 * @return The platform-specific test executor
	 */
	public Runnable createTestExecutor(TestSuite suite, String engine, TestSuiteExecution statuses, int testNr, int suiteNr);
	
	/**
	 * Initializes database and prepares everything to start execution
	 * @param testsId The id of the testing that has to be used 
	 */
	public void createDatabase(int testsId);
	
	/**
	 * Gets test execution results from the database
	 * @param testsId
	 * @return
	 */
	public TestResults getTestResults(int testsId);
	
	/**
	 * Removes specified test results from the database, cleaning up the testing environment.
	 * @param testResults The test results instance that should be deleted
	 */
	public void deleteTestResults(TestResults testResults);
}
