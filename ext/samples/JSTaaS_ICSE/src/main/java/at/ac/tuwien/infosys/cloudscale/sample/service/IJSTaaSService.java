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

import javax.jws.WebService;

import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestResults;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSetup;
import at.ac.tuwien.infosys.jcloudscale.sample.service.exceptions.TestException;

/**
 * Web-service interface that is accessed by the test client.
 */
@WebService
public interface IJSTaaSService {
	
    /**
     * Runs specified tests within the cloud.
     * @param tests The setup of the tests to run.
     */
    void runTestSuites(TestSetup tests) throws TestException;
    
    /**
     * Allows to poll if the tests completed already.
     * @param testId The id of the test setup that has to be verified
     * @return <b>true</b> if all results are available. Otherwise, <b>false</b>.
     */
    boolean resultAvailable(int testId) throws TestException;
    
    /**
     * Gets the test execution results from the server.
     * @param testId The id of the test setup that has to be retrieved
     * @return Test execution results if there are any.
     */
    TestResults getResults(int testId) throws TestException;
    
}
