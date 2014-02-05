/*
   Copyright 2013 Philipp Leitner

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
package at.ac.tuwien.infosys.jcloudscale.test.integration.base;

import static org.junit.Assert.assertEquals;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.CloudConfigException;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.FaultingCloudObject1;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.FaultingCloudObject1.MyFaultException;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.NonSerializableDataType;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject1;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class TestFaultHandling
{
	protected static CloudManager cs = null;

	@Test(expected = JCloudScaleException.class)
	public void testFaultingObject()
	{
 
		FaultingCloudObject1 faulty = new FaultingCloudObject1();
		faulty.faulty();

	}
	
	@Test(expected = MyFaultException.class)
	public void testFaultingConstructor() throws Exception
	{
		try
		{
			new FaultingCloudObject1(true);
		}
		catch(JCloudScaleException ex)
		{//even though we're expecting some specific exception, we will get JCloudScaleException, but inside should be ours.
			throw (Exception)ex.getCause();
		}
	}
	
	@Test(expected = NullPointerException.class)
	public void testFaultingConstructorNullPointer() throws Exception
	{
		try
		{
			new FaultingCloudObject1("GIMME NULL");
		}
		catch(JCloudScaleException ex)
		{//even though we're expecting some specific exception, we will get JCloudScaleException, but inside should be ours.
			throw (Exception)ex.getCause();
		}
	}
	
	@Test(expected = RuntimeException.class)
	public void testFaultingConstructorRuntime() throws Exception
	{
		try
		{
			new FaultingCloudObject1(0);
		}
		catch(JCloudScaleException ex)
		{//even though we're expecting some specific exception, we will get JCloudScaleException, but inside should be ours.
			throw (Exception)ex.getCause();
		}
	}
	
	@Test(expected = JCloudScaleException.class)
	public void testNonSerializableParam()
	{
		FaultingCloudObject1 faulty = new FaultingCloudObject1();
		faulty.nonSerializable(new NonSerializableDataType());
	}

	@Test(expected = JCloudScaleException.class)
	public void testInvokationAfterObjectDestruction() throws InterruptedException, CloudConfigException
	{
		assertEquals(0, cs.countCloudObjects());
		TestCloudObject1 co = new TestCloudObject1();
		assertEquals(1, cs.countCloudObjects());

		co.killMeSoftly();
		assertEquals(0, cs.countCloudObjects());

		co.killMeSoftly();
	}

}
