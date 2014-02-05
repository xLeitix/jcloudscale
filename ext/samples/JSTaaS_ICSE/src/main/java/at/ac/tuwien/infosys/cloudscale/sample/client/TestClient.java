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
package at.ac.tuwien.infosys.jcloudscale.sample.client;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import at.ac.tuwien.infosys.jcloudscale.sample.service.IJSTaaSService;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.ContinuationStrategy;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.Test;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSetup;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSuite;
import at.ac.tuwien.infosys.jcloudscale.sample.service.exceptions.TestException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class TestClient {

	public static void main(String[] args) throws IOException, InterruptedException, JMSException, TestException 
	{
		//
		// connecting to the web-service
		//
		JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
		factory.getInInterceptors().add(new LoggingInInterceptor());
		factory.getOutInterceptors().add(new LoggingOutInterceptor());
		factory.setServiceClass(IJSTaaSService.class);
		factory.setAddress("http://localhost:9000/jstaas");		// change this line if client is running from separate machine
		IJSTaaSService client = (IJSTaaSService) factory.create();
		Client httpClient = ClientProxy.getClient(client);
		HTTPClientPolicy policy = new HTTPClientPolicy();
		policy.setConnectionTimeout(Long.MAX_VALUE);
		policy.setReceiveTimeout(Long.MAX_VALUE);
		policy.setAllowChunking(false);
		((HTTPConduit)httpClient.getConduit()).setClient(policy);
		
		//
		// Evaluation Task Parameters
		//
		final int setupId = 1; // database id. Doesn't matter much.
		final int suitesCount = 2; // amount of parallel tasks to run (we run with 20)
		final int testsPerSuite = 1;// duration of task (increases exponentially) (we run with 8)
		
		TestSetup setup = createCPUSetup(setupId, suitesCount, testsPerSuite);
		client.runTestSuites(setup);
		
		//
		// waiting for results to become available
		//
		while(!client.resultAvailable(setupId))
			Thread.sleep(1000);
		
		// cleanup method
		client.getResults(setupId);
	}
	
	private static TestSetup createCPUSetup(int setupId, int suitesCount, int testsPerSuite) 
	{
		TestSetup setup = new TestSetup();
		setup.setEngine("javascript");
		setup.setTestId(setupId);
		
		//
		// creating tests for each test suite
		//
		List<Test> tests = new ArrayList<Test>();
		for(int testId = 1;testId <= testsPerSuite;++testId)
		{
			// creating the test
			int parameter = 20+testId;
			Test test = new Test();
			test.setId(testId);
			test.setExpectedOutcome(fibo(parameter).toString());
			Map<String,String> parameters = new HashMap<String, String>();
			
			// making parameter bindings
			parameters.put("NUMBER", String.valueOf(parameter));
			test.setParameterBindings(parameters);
			
			//loading script
			try {
				test.setScript(load("tests/longFibo.js"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			tests.add(test);
		}
		
		for(int i=1; i<= suitesCount; i++) 
		{
			TestSuite suite = new TestSuite();
			suite.setContinuation(ContinuationStrategy.CONTINUE_ON_FAIL);
			suite.setName("Test "+i);
			
			//adding tests.
			suite.setSuite(tests);
			
			setup.addSuite(suite);
		}
		return setup;
			
	}
	
	/**
	 * Loads file from the specified location
	 */
	private static String load(String path) throws IOException 
	{
		return Files.toString(new File(path), Charsets.UTF_8);
	}
	
	/**
	 * Calculates the fibonacci number for specified position 
	 */
	private static BigInteger fibo(int n)
	{
		if(n<2)
			return BigInteger.ONE;
		
		BigInteger n1 = BigInteger.ONE;
		BigInteger n2 = BigInteger.ONE;
		
		for(int i=2;i<n;++i)
		{
			BigInteger n3 = n1.add(n2);
			n1 = n2;
			n2 = n3;
		}
		
		return n2;	
	}
	
}
