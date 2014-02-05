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
package at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.worker;

import java.io.Closeable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.naming.Context;

import org.lightcouch.DocumentConflictException;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.core.DatastoreFactory;
import at.ac.tuwien.infosys.jcloudscale.sample.service.Engine;
import at.ac.tuwien.infosys.jcloudscale.sample.service.TestStatus;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.Test;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestResult;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestResults;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSuite;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSuiteResult;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.MessageQueueHelper;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.OpenstackConfiguration;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.commands.DiscoveryRequest;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.commands.DiscoveryResponse;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.commands.TestSuiteExecutionRequest;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.commands.TestSuiteExecutionResponse;
import at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.commands.TestSuiteUpdateResponse;

/**
 * Dummy implementation of the cloud worker that waits for test suite tasks and executes them 
 * comparably to the cloud objects in jcloudscale implementation.
 */
public class OpenstackWorker implements Closeable {

	private static final Logger log = Logger.getLogger(OpenstackWorker.class.getName());
	
	/**
	 * worker's entry method. starts the worker and waits for the tasks.
	 */
	public static void main(String[] args) 
	{
		String hostId = UUID.randomUUID().toString();
		
		//
		// Detecting MQ address.
		//
		log.info("Host "+hostId+" is starting. Detecting MQ address...");
		String mqAddress = OpenstackConfiguration.getMessageQueueAddress();
		
		try(OpenstackWorker worker = new OpenstackWorker(hostId, mqAddress))
		{
			worker.run();
		}
	}

	
	private String hostId = "";
	private CountDownLatch exitLatch = new CountDownLatch(1);
	
	private Context context;
	private Connection mqConnection;
	private Session session;
	private MessageProducer producer;
	
	private ExecutorService threadPool;
	
	private OpenstackWorker(String hostId, String mqAddress)
	{
		this.hostId = hostId;
		log.info("Connecting to the MQ " +mqAddress);
		context = MessageQueueHelper.createContext(mqAddress);
		mqConnection = MessageQueueHelper.establishConnection(context, mqAddress);
		threadPool = Executors.newCachedThreadPool();
	}
	
	
	private void run()
	{
		log.info("Subscribing command listener...");
		try
		{
			// reply producer
			Destination destination = (Destination)context.lookup("dynamicQueues/"+OpenstackConfiguration.getClientQueue());
			session = mqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			producer = session.createProducer(destination);
			
			// command listener
			destination = (Destination)context.lookup("dynamicTopics/"+OpenstackConfiguration.getWorkerTopic());
			MessageConsumer consumer = session.createConsumer(destination, MessageQueueHelper.createMessageSelector(hostId));
			consumer.setMessageListener(new CommandListener());
			
			//discovery listener
			destination = (Destination)context.lookup("dynamicTopics/"+OpenstackConfiguration.getWorkerDiscoveryTopic());
			consumer = session.createConsumer(destination);
			consumer.setMessageListener(new CommandListener());
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return;
		}
		
		log.info("Host "+hostId+" is ready to execute tasks. Waiting for exit event...");
		try 
		{
			exitLatch.await();
		} catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
		
		log.info("Exit message received, waiting for active tasks to finish...");
	}
	
	@Override
	public void close()
	{
		log.info("Host "+hostId+" is shutting down.");
		if(this.mqConnection != null)
		{
			try {
				this.mqConnection.close();
			} catch (JMSException e) 
			{
				e.printStackTrace();
			}
			this.mqConnection = null;
		}
		
		if(this.threadPool != null)
			threadPool.shutdown();
	}
	
	/**
	 * Listens to the message queue to receive commands to process.
	 */
	private class CommandListener implements MessageListener
	{
		@Override
		public void onMessage(Message message) 
		{
			try
			{
				if(!(message instanceof ObjectMessage))
				{
					log.warning("Unexpected message :"+message);
					return;
				}
				
				Serializable msgobj = ((ObjectMessage)message).getObject();
				
				//
				// sends discovery request to allow discovering of the host.
				//
				if(msgobj instanceof DiscoveryRequest)
				{
					MessageQueueHelper.sendMessage(session, producer, new DiscoveryResponse(hostId));
					log.info("Discovery request received, discovery response sent.");
				}
				
				//
				// Schedules processing of test suite task.
				//
				if(msgobj instanceof TestSuiteExecutionRequest)
				{
					log.info("Received test suite execution request!");
					threadPool.submit(new TestSuiteExecutor((TestSuiteExecutionRequest)msgobj));
				}
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * A class that is responsible for test suite execution and progress reporting.
	 */
	private class TestSuiteExecutor implements Runnable
	{
		private Datastore datastore = DatastoreFactory.getInstance().getDatastoreByName("testresults");
		private int suiteNr;
		private String engine;
		private TestSuite suite;
		private int testNr;
		
		public TestSuiteExecutor(TestSuiteExecutionRequest request)
		{
			this.suite = request.getTestSuite();
			this.engine = request.getEngine();
			this.suiteNr = request.getSuiteNr();
			this.testNr = request.getTestNr();
		}
		
		@Override
		public void run() 
		{
			TestSuiteResult theResults = new TestSuiteResult();
			
				MessageQueueHelper.sendMessage(session, producer, new TestSuiteUpdateResponse(TestStatus.INIT, suiteNr));
				//			statuses.changeStatus(TestStatus.INIT, suiteNr); // appropriate JCloudScale-implementation methods. 
							
				log.info("Starting to run cloud object on cloud host");
	
				Engine theEngine = Engine.create(engine);
				
				MessageQueueHelper.sendMessage(session, producer, new TestSuiteUpdateResponse(TestStatus.RUNNING, suiteNr));
				
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
				
				boolean successfullySaved = false;
				do {
					try 
					{
						tryToSave(Integer.toString(testNr), theResults);
						successfullySaved = true;
						log.info("Successfully stored result to datastore");
					} 
					catch(DatastoreException | DocumentConflictException e) 
					{
						// this happens if we have a conflict
						log.warning("Received conflict while trying to save to DB. Retrying.");
					}
					catch(Exception ex)
					{
						log.warning("Unexpected exception: "+ex);
						ex.printStackTrace();
					}
				} while(!successfullySaved);
			
			
				MessageQueueHelper.sendMessage(session, producer, new TestSuiteUpdateResponse(TestStatus.DONE, suiteNr));
	//			statuses.changeStatus(TestStatus.DONE, suiteNr);
				
			
			log.info("Execution of suite "+suite.getName()+" is complete.");
			MessageQueueHelper.sendMessage(session, producer, new TestSuiteExecutionResponse(suiteNr));
		}
		
		private void tryToSave(String id, TestSuiteResult theResults) throws DatastoreException 
		{
			// datastore-independent implementation of the database storage.
//			TestResults results = db.load(id);
//			if(results == null)
//				throw new NullPointerException();
//			results.putResult(suite.getName(), theResults);
//			db.update(results);
			TestResults resultsStub = datastore.find(TestResults.class, String.valueOf(id));
			resultsStub.putResult(suite.getName(), theResults);
			datastore.update(resultsStub);
		}
	}
}
