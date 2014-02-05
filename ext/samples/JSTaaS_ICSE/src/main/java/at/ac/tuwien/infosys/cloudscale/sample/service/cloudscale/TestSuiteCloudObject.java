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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import at.ac.tuwien.infosys.jcloudscale.annotations.ByValueParameter;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObjectId;
import at.ac.tuwien.infosys.jcloudscale.annotations.DestructCloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.FileDependency;
import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.DataSource;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.sample.service.Engine;
import at.ac.tuwien.infosys.jcloudscale.sample.service.TestStatus;
import at.ac.tuwien.infosys.jcloudscale.sample.service.TestSuiteExecution;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.Test;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestResult;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestResults;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSuite;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSuiteResult;

/**
 * JCloudScale cloud object that is being executed on the remote host.
 */
@CloudObject
@FileDependency(files = {"META-INF/datastores.xml"})
public class TestSuiteCloudObject {
	
	
	/**
	 * The cloudScale id that is needed to determine where the current object was deployed. 
	 */
	@CloudObjectId
	private UUID coId;
	
	/**
	 * Defines the database access object that will be inserted by jcloudscale datastore library. 
	 */
	@DataSource(name = "testresults")
	private Datastore datastore;
	
	private Logger log;
	
	private TestSuite suite;
	private String engine;
	private int testId;
	
	/**
	 * Creates a new instance of the object. 
	 * For jcloudscale cloud objects, this call is intercepted and scheduled to a specific host.
	 * @param engine The engine that should be used for tests execution (e.g. "javascript")
	 */
	public TestSuiteCloudObject(String engine) 
	{
		log = Logger.getLogger(TestSuiteCloudObject.class.getName());
		this.engine = engine;
	}

	/**
	 * Returns the cloudobject id injected by the jcloudscale.
	 * @return
	 */
	public @ByValueParameter UUID getId() {
		return coId;
	}

	/**
	 * Specifies the test suite that has to be executed by this cloud object.
	 * @param suite The by-value parameter that specifies the test suite that has to be executed by this cloud object.
	 * @param testId The location of this test suite in the database where execution results has to be stored.
	 */
	public void setSuite(@ByValueParameter TestSuite suite, int testId) {
		this.suite = suite;
		this.testId = testId;
	}

	/**
	 * Main execution method that accepts by-reference parameter where execution progress should be reported.
	 * As this method is anotated by <b>DestructCloudObject</b>, it also determines the point when cloudobject
	 * can be destroyed.
	 * @param statuses
	 * @param suiteNr
	 */
	@DestructCloudObject
	public void runCloudObject(TestSuiteExecution statuses, int suiteNr) 
	{
		try
		{
			TestSuiteResult theResults = new TestSuiteResult();
			
			// this method call will be intercepted by clouscale and the actual object on the
			// client will change.
			statuses.changeStatus(TestStatus.INIT, suiteNr);
			
			// all user output from cloud objects is redirected to the client application as well.
			log.info("Starting to run cloud object on cloud host");
			
			Engine theEngine = Engine.create(engine);
			
			statuses.changeStatus(TestStatus.RUNNING, suiteNr);

			//
			// Executing tests scheduled for this cloud object.
			//
			Map<Integer,TestResult> results = new HashMap<>();
			for(Test test : suite.getSuite()) 
			{
				log.info("Running test "+test.getId());
				theEngine.run(test);
				TestResult result = new TestResult();
				result.setMessage(theEngine.lastMessage());
				result.setOutcome(theEngine.lastOutcome());
				result.setTestId(test.getId());
				results.put(test.getId(), result);
				log.info("Done with "+test.getId()+" --> "+result.getOutcome());
				if(!suite.continueOnOutcome(result.getOutcome())) {
					log.info("Stopping because of outcome "+result.getOutcome());
					break;
				}
			}
			
			theResults.setName(suite.getName());
			theResults.setResults(results);
			
			log.info("Storing results for "+suite.getName()+" to datastore "+datastore.getName());
	
			//
			// Saving results to the database.
			// As conflicts are solved optimistically, in case of failure,
			// saving attempt has to be repeated.
			//
			boolean successfullySaved = false;
			do {
				try {
					tryToSave(testId, theResults);
					successfullySaved = true;
					log.info("Successfully stored result to datastore");
				} catch(DatastoreException e) 
				{
					// this happens if we have a conflict
					log.warning("Suite "+suite.getName()+" received conflict while trying to save to DB. Retrying.");
				}
			} while(!successfullySaved);
			
			statuses.changeStatus(TestStatus.DONE, suiteNr);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private void tryToSave(int testId, TestSuiteResult theResults) throws DatastoreException 
	{
		TestResults resultsStub = datastore.find(TestResults.class, String.valueOf(testId));
		resultsStub.putResult(suite.getName(), theResults);
		datastore.update(resultsStub);
	}
}
