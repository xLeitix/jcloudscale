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

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.StaticFieldSubObject;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.StaticFieldTestObject1;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.StaticFieldTestObject2;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class TestStaticFields 
{
	
	protected static CloudManager cs;
	 
	
	/**
	 * this method performs necessary preparations and should be called prior to any test.
	 */
	@Before
	public void setup() throws Exception 
	{
		StaticFieldTestObject1.resetStaticFieldsToDefaults();
	}
	
	@Test
	public void testAccessPublicField() {
		
		StaticFieldTestObject1 sf1 = new StaticFieldTestObject1();
		sf1.setField("remote");
		
		assertEquals("remote", StaticFieldTestObject1.myTestField2);
		
		sf1.resetStaticFieldsToDefaultsRemote(); //resetting remote values on the server.
	}
	
	@Test
	public void testWritePublicField() {
		
		StaticFieldTestObject1 sf1 = new StaticFieldTestObject1();
		StaticFieldTestObject1.myTestField2 = "remote";
		
		assertEquals("remote", sf1.getOtherField());
		
		sf1.resetStaticFieldsToDefaultsRemote(); //resetting remote values on the server.
	}
	
	@Test
	public void testAccessLocalField() {
		
		StaticFieldTestObject1 sf1 = new StaticFieldTestObject1();
		sf1.setField("remote");
		
		assertEquals("local", StaticFieldTestObject1.myTestField3);
		
		sf1.resetStaticFieldsToDefaultsRemote(); //resetting remote values on the server.
	}
	
	@Test
	public void testWriteLocalField() {
		
		StaticFieldTestObject1 sf1 = new StaticFieldTestObject1();
		StaticFieldTestObject1.myTestField3 = "remote";
		
		assertEquals("local", sf1.getYetAnotherField());
		
		sf1.resetStaticFieldsToDefaultsRemote(); //resetting remote values on the server.
	}
	
	@Test
	public void testStaticStringField() {
		
		StaticFieldTestObject1 sf1 = new StaticFieldTestObject1();
		assertEquals(1, cs.countVirtualMachines());
		sf1.setField("oldVal");
		assertEquals("oldVal", sf1.getField());
		StaticFieldTestObject1 sf2 = new StaticFieldTestObject1();
		assertEquals(2, cs.countVirtualMachines());
		assertEquals("oldVal", sf2.getField());
		sf2.setField("newVal");
		assertEquals("newVal", sf2.getField());
		assertEquals("newVal", sf1.getField());
		
		sf1.resetStaticFieldsToDefaultsRemote(); //resetting remote values on the server.
		sf2.resetStaticFieldsToDefaultsRemote(); //resetting remote values on the server.
	}
	
	@Test
	public void testStaticIntField() {
		
		StaticFieldTestObject2 sf1 = new StaticFieldTestObject2();
		assertEquals(1, cs.countVirtualMachines());
		sf1.setField(100);
		assertEquals(100, sf1.getField());
		StaticFieldTestObject2 sf2 = new StaticFieldTestObject2();
		assertEquals(2, cs.countVirtualMachines());
		assertEquals(100, sf2.getField());
		sf2.setField(200);
		assertEquals(200, sf2.getField());
		assertEquals(200, sf1.getField());
	}
	
	@Test
	public void testStaticInheritedField() {
		
		StaticFieldSubObject sf1 = new StaticFieldSubObject();
		assertEquals(1, cs.countVirtualMachines());
		sf1.setField("oldValXXXXXX");
		assertEquals("oldValXXXXXX", sf1.getField());
		StaticFieldSubObject sf2 = new StaticFieldSubObject();
		assertEquals(2, cs.countVirtualMachines());
		assertEquals("oldValXXXXXX", sf2.getField());
		sf2.setField("newValYYYYYY");
		assertEquals("newValYYYYYY", sf2.getField());
		assertEquals("newValYYYYYY", sf1.getField());
		
		sf1.resetStaticFieldsToDefaultsRemote(); //resetting remote values on the server.
		sf2.resetStaticFieldsToDefaultsRemote(); //resetting remote values on the server.
	}
	
	@Test
	public void testStaticInheritedFieldDifferentType() {
		
		StaticFieldSubObject sf1 = new StaticFieldSubObject();
		assertEquals(1, cs.countVirtualMachines());
		sf1.setField("oldValZZZZZZ");
		assertEquals("oldValZZZZZZ", sf1.getField());
		StaticFieldTestObject1 sf2 = new StaticFieldTestObject1();
		assertEquals(2, cs.countVirtualMachines());
		assertEquals("oldValZZZZZZ", sf2.getField());
		sf2.setField("newValAAAAAA");
		assertEquals("newValAAAAAA", sf2.getField());
		assertEquals("newValAAAAAA", sf1.getField());
		
		sf1.resetStaticFieldsToDefaultsRemote(); //resetting remote values on the server.
		sf2.resetStaticFieldsToDefaultsRemote(); //resetting remote values on the server.
	}
	
//	@Test
//	public void testStaticComplexField() {
//		
//		StaticFieldTestObjectWithComplexType complex = new StaticFieldTestObjectWithComplexType();
//		assertEquals(1, cs.countVirtualMachines());
//		MyComplexObject c = new MyComplexObject();
//		c.a = "aaa";
//		c.b = "bbb";
//		c.c = new byte[]{1,1,2};
//		complex.setField(c);
//		assertEquals("bbb", complex.getField().b);
//		
//		StaticFieldTestObjectWithComplexType complex2 = new StaticFieldTestObjectWithComplexType();
//		assertEquals(2, cs.countVirtualMachines());
//		assertEquals("bbb", complex2.getField().b);
//		
//	}
	
	// TODO: test with reference types, and maybe some actual design patterns like singletons
	
}