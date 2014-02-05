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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.ByRefCloudObject;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestByRefParameter;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestByRefParameter2;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestByValueParameter;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class TestByRefHandling  
{
	protected static CloudManager cs = null;
	
	@Test 
	public void testInstanceofInterface() throws Exception
	{
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		byRef.testInstanceofInterface(new TestByRefParameter());
	}
	
	@Test
	public void testByRefObject() throws Exception {
		
		TestByRefParameter.value = "local";
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		assertEquals("local", byRef.passMeThis(new TestByRefParameter()));
		
	}
	
	@Test
	public void testByRefObjectWithNull() throws Exception {
		
		TestByRefParameter.value = "local";
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		assertEquals(null, byRef.passMeThis((TestByRefParameter)null));
		
	}
	
	@Test
	public void testByRefInConstructor() throws Exception {
		
		TestByRefParameter.value = "local";
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject(new TestByRefParameter());
		assertEquals(1,cs.countCloudObjects());
		
		assertEquals("local", byRef.passMeThis());
		
	}
	
	@Test
	public void testByRefMultipleParams() throws Exception {
		
		TestByRefParameter.value = "local";
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		assertEquals("local:local", byRef.passMeThis(new TestByRefParameter(), new TestByRefParameter()));
		
	}
	
	@Test
	public void testByRefVoidInvocation() throws Exception {
		
		TestByRefParameter.value = "local";
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		byRef.invoke(new TestByRefParameter());
		
	}
	
	@Test
	public void testByRefAndBackAgain() throws Exception {
		
		TestByRefParameter.value = "local";
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		assertEquals("remote", byRef.thereAndBackAgain(new TestByRefParameter()));
		
	}
	
	@Test
	public void testByRefWithThis() throws Exception {
		
		TestByRefParameter.value = "local";
		ByRefCloudObject.value = "local";
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		assertEquals("remote", byRef.thereAndBackAgainWithThis(new TestByRefParameter()));
		
	}
	
	@Test
	public void testByRefCOAsParameter() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef1 = new ByRefCloudObject("byref1");
		ByRefCloudObject byRef2 = new ByRefCloudObject("byref2");
		assertEquals(2,cs.countCloudObjects());
		
		assertEquals("byref2", byRef1.withOtherCO(byRef2));
		
	}
	
	@Test
	public void testByRefReturnRef() throws Exception {
		
		TestByRefParameter.value = "local";
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		TestByRefParameter param = byRef.giveMeAParam();
		assertEquals("remote", param.callByRef());
		
	}
	
	@Test
	public void testByRefReturnRefReturnNull() throws Exception {
		
		TestByRefParameter.value = "local";
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		TestByRefParameter param = byRef.giveMeANullParam();
		assertEquals(null, param);
		
	}
	
	@Test
	public void testByRefReturnYourselfCompareWithToString() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		ByRefCloudObject fromServer = byRef.returnYourself(); 
		
		assertEquals(fromServer.toString(), byRef.toString());
		
	}
	
	@Test
	public void testByRefReturnYourselfCompareWithEquals() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		ByRefCloudObject fromServer = byRef.returnYourself(); 
		
		assertEquals(fromServer, byRef);
		
	}
	
	@Test
	public void testRefWithoutDefaultParameter() throws Exception {
		
		TestByRefParameter2.value = "local";
		
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		try {
			assertEquals("local", byRef.passMeThis(new TestByRefParameter2("hugo")));
			fail("Expected JCloudScale exception");
		} catch(JCloudScaleException e) {
			assertTrue(e.getCause().getMessage().contains("is used as by-reference parameter but does not contain a no-arg constructor"));
		}
	}
	
	@Test
	public void testByValueClassBasedAnnotation() throws Exception {
		
		TestByValueParameter param = new TestByValueParameter("Test By Value", 1);
		assertEquals(0,cs.countCloudObjects());
		ByRefCloudObject byRef = new ByRefCloudObject("byval", param);
		assertEquals(1,cs.countCloudObjects());
		
		param = byRef.swapByValueParameter(new TestByValueParameter("New Parameter By Value", 2));
		
		if(param == null || !param.getClass().equals(TestByValueParameter.class))
			throw new RuntimeException("Incorrect parameter type! Expecting it to be passed by value!");
	}
	
	// TODO does this stuff also work with reflection? add tests
	
}
