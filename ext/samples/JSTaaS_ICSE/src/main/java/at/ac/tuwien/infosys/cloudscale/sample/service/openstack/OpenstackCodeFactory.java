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
package at.ac.tuwien.infosys.jcloudscale.sample.service.openstack;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.lightcouch.DocumentConflictException;

import at.ac.tuwien.infosys.jcloudscale.sample.service.IPlatformDependentCodeFactory;
import at.ac.tuwien.infosys.jcloudscale.sample.service.Stopwatch;
import at.ac.tuwien.infosys.jcloudscale.sample.service.TestSuiteExecution;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestResults;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSuite;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.commands.TestSuiteExecutionRequest;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.commands.TestSuiteExecutionResponse;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.commands.TestSuiteUpdateResponse;

/**
 * Defines the platform-dependent code that was used to create a baseline execution
 * to be able to determine the overhead of the jcloudscale execution.
 */
public class OpenstackCodeFactory implements IPlatformDependentCodeFactory 
{
	private Logger log = Logger.getLogger(this.getClass().getName());
	private Map<String, Integer> tasksDistribution = null;
	private Map<Integer, CountDownLatch> suiteToLatchMap = new ConcurrentHashMap<>();
	private TestSuiteExecution testSuiteExecution;
	private CouchDbHelper db;
	private Stopwatch parentStopwatch;
	
	public OpenstackCodeFactory()
	{
		//
		// Locating worker hosts
		//
		tasksDistribution = new HashMap<>();
		for(String hostId : OpenstackWrapper.getInstance().getWorkers())
			tasksDistribution.put(hostId, 0);
		
		// subscribing response listener
		OpenstackWrapper.getInstance().subscribeMessageListener(new ResponseMessageListener());
		
	}
	
	private synchronized CouchDbHelper getDatabase()
	{
		if(db != null)
			return db;
		
		//
		// Initializing database
		//
		return db = new CouchDbHelper(
				OpenstackConfiguration.getCouchdbHostname(), 
				OpenstackConfiguration.getCouchdbPort(), 
				OpenstackConfiguration.getCouchdbDatabase());
	}
	
	
	@Override
	public Runnable createTestExecutor(TestSuite suite, String engine,
			TestSuiteExecution statuses, int testNr, int suiteNr)
	{
		this.testSuiteExecution = statuses;
		
		return new Executor(suite, engine, testNr, suiteNr);
	}

	@Override
	public void createDatabase(int testsId) 
	{
		boolean retry = true;
		while(retry)
		{
			try
			{
				TestResults results = new TestResults();
				results.setTestsId(String.valueOf(testsId));
				getDatabase().save(results);
				retry = false;
			}
			catch(DocumentConflictException ex)
			{
				retry = false;
				getDatabase().delete(getDatabase().load(String.valueOf(testsId)));
			}
			
		}
		
		// starting stopwatch
		String filename ="eval_openstack_"+tasksDistribution.size()+".csv";
		parentStopwatch = new Stopwatch(filename).withMessage("TOTAL").withPrecision(TimeUnit.MILLISECONDS);
	}

	@Override
	public TestResults getTestResults(int testsId) 
	{
		return getDatabase().load(Integer.toString(testsId));
	}

	@Override
	public void deleteTestResults(TestResults tests) 
	{
		getDatabase().delete(tests);
		
		parentStopwatch.close();
		parentStopwatch = null;
	}
	
	private synchronized String getLeastLoadedWorker()
	{
		if(tasksDistribution.isEmpty())
			return null;
		
		// selecting host
		Entry<String, Integer> bestEntry = null;
		for(Entry<String, Integer> hostLoad : tasksDistribution.entrySet())
			if(bestEntry == null || hostLoad.getValue().intValue() < bestEntry.getValue().intValue())
				bestEntry = hostLoad;
		
		// increasing load
		bestEntry.setValue(bestEntry.getValue().intValue()+1);
		
		return bestEntry.getKey();
	}
	
	private synchronized void decreaseWorkerLoad(String worker)
	{
		tasksDistribution.put(worker, tasksDistribution.get(worker).intValue() - 1);
	}
	
	/**
	 * Separate executor that submits the task to the jcloudscale-independent worker and waits for results.
	 */
	private class Executor implements Runnable
	{
		private TestSuite suite;
		private String engine;	
		private int testNr;
		private int suiteNr;
		
		public Executor(TestSuite suite, String engine,	int testNr, int suiteNr)
		{
			this.suite = suite;
			this.engine = engine;
			this.testNr = testNr;
			this.suiteNr = suiteNr;
		}
		
		@Override
		public void run() 
		{
			String selectedHost = getLeastLoadedWorker();
			if(selectedHost == null)
			{
				log.severe("Failed to get host to run the task on! Host selection method returned null.");
				return;
			}
			
			// preparing waiting condition
			CountDownLatch latch = new CountDownLatch(1);
			suiteToLatchMap.put(suiteNr, latch);
			
			// starting execution
			try(Stopwatch stopwatch = new Stopwatch(parentStopwatch)
											.withMessage(String.format("Suite %03d", suiteNr))
											.withPrecision(TimeUnit.MILLISECONDS))
			{
				OpenstackWrapper.getInstance().sendMessageToWorker(new TestSuiteExecutionRequest(suite, engine, testNr, suiteNr), selectedHost);
				log.info("Submitted task "+suiteNr+" to the host "+selectedHost+". Waiting to finish.");
				
				//waiting for execution to finish.
				latch.await();
			}
			catch(InterruptedException ex)
			{
				ex.printStackTrace();
			}
			finally
			{
				suiteToLatchMap.remove(suiteNr);
				decreaseWorkerLoad(selectedHost);
			}
		}
	}

	/**
	 * Response listener that handles responses from the server to determine when execution has finished. 
	 */
	private class ResponseMessageListener implements MessageListener
	{
		@Override
		public void onMessage(Message message) 
		{
			if(!(message instanceof ObjectMessage))
			{
				log.warning("Unexpected message: "+message);
				return;
			}
			
			try
			{
				Serializable payload = ((ObjectMessage)message).getObject();
				
				//
				// This response is here to simulate callback method from the jcloudscale.
				//
				if(payload instanceof TestSuiteUpdateResponse)
				{
					log.info("Test Suite execution status update: "+payload);
					TestSuiteUpdateResponse response = (TestSuiteUpdateResponse)payload;
					testSuiteExecution.changeStatus(response.getNewStatus(), response.getSuiteNumber());
				}
				
				//
				// This one signals that execution is complete.
				//
				if(payload instanceof TestSuiteExecutionResponse)
				{
					TestSuiteExecutionResponse response = (TestSuiteExecutionResponse)payload;
					log.info("Test Suite "+response.getSuiteNr()+" completion event received.");
					CountDownLatch latch = suiteToLatchMap.get(response.getSuiteNr());
					if(latch == null)
						log.severe("Suite execution completed, but appropriate countdown latch was not found!");
					else
						latch.countDown();
				}
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
