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
package at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.commands;

import java.io.IOException;
import java.io.Serializable;

import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestSuite;
import at.ac.tuwien.infosys.jcloudscale.utility.SerializationUtil;

/**
 * Request to execute the specified test suite by the worker.
 */
public class TestSuiteExecutionRequest implements Serializable 
{
	private static final long serialVersionUID = 2L;
	
	private byte[] testSuiteSerialized;
	private String engine;
	private int testNr;
	private int suiteNr;
	public TestSuiteExecutionRequest(){}
	
	public TestSuiteExecutionRequest(TestSuite testSuite, String engine, int testNr, int suiteNr)
	{
		setTestSuite(testSuite);
		this.engine = engine;
		this.testNr = testNr;
		this.suiteNr = suiteNr;
	}
	
	public TestSuite getTestSuite() 
	{
		try 
		{
			return (TestSuite) SerializationUtil.getObjectFromBytes(testSuiteSerialized, this.getClass().getClassLoader());
		} 
		catch (ClassNotFoundException | IOException e) 
		{
			e.printStackTrace();
			return null;
		} 
	}
	
	public void setTestSuite(TestSuite testSuite) 
	{
		try
		{
			testSuiteSerialized = SerializationUtil.serializeToByteArray(testSuite);
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public String getEngine() {
		return engine;
	}
	public void setEngine(String engine) {
		this.engine = engine;
	}
	public int getTestNr() {
		return testNr;
	}
	public void setTestNr(int testNr) {
		this.testNr = testNr;
	}
	public int getSuiteNr() {
		return suiteNr;
	}
	public void setSuiteNr(int suiteNr) {
		this.suiteNr = suiteNr;
	}
}
