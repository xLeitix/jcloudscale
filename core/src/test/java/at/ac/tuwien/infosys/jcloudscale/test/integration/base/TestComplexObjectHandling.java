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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import at.ac.tuwien.infosys.jcloudscale.annotations.Local;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.ITestObject;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.MoreComplexTestClass;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.MoreComplexTestSubClass;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.MoreComplexTestSuperClass;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject1;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject3;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestSubCloudObject1;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class TestComplexObjectHandling
{
	protected static CloudManager cs = null;
	protected static final String filename = "complexObjectHandling.yap";
	
	
	@Test 
	public void testLocalAnnotation() 
	{
		assertEquals(0, cs.countCloudObjects());
		
		@SuppressWarnings("unused")
		MoreComplexTestClass co = new MoreComplexTestClass();
		
		assertEquals(0, cs.countCloudObjects());
	}
	
	@Test 
	public void testLocalAnnotationOnMethod() 
	{
		assertEquals(0, cs.countCloudObjects());
		MoreComplexTestClass co  = new MoreComplexTestClass("1.yap");
		assertEquals(1, cs.countCloudObjects());
		
		co.setInCloud("server");
		assertEquals("1.yap", co.getLocally());
		
	}
	
	@Test
	public void testMoreComplexObject() 
	{
		assertEquals(0, cs.countCloudObjects());
		
		MoreComplexTestClass testClass = new MoreComplexTestClass(filename);
		
		testClass.run();
		
		assertEquals(1, cs.countCloudObjects());
		
		testClass.destroy();
		
		assertEquals(0, cs.countCloudObjects());		
	}
	
	@Test
	public void testPackageConstructor()
	{
		assertEquals(0, cs.countCloudObjects());
		
		MoreComplexTestClass testClass = MoreComplexTestClass.createInstanceWithProtectedConstructor();
		
		testClass.run();
		
		assertEquals(1, cs.countCloudObjects());
		
		testClass.destroy();
		
		assertEquals(0, cs.countCloudObjects());
	}
	
	@Test
	public void testPackageMethod() throws IllegalArgumentException, SecurityException, IllegalAccessException,
		InvocationTargetException, NoSuchMethodException 
	{
		assertEquals(0, cs.countCloudObjects());
		
		MoreComplexTestClass testClass = new MoreComplexTestClass(filename);
		
		testClass.run();
		
		assertEquals(1, cs.countCloudObjects());
		assertNotNull(MoreComplexTestClass.invokePackageMethod(testClass));
		
		testClass.destroy();
		assertEquals(0, cs.countCloudObjects());		
	}
	
	@Test
	public void testProtectedMethod() throws IllegalArgumentException, SecurityException, IllegalAccessException,
		InvocationTargetException, NoSuchMethodException 
	{
		assertEquals(0, cs.countCloudObjects());
		
		MoreComplexTestClass testClass = new MoreComplexTestClass(filename);
		
		testClass.run();
		
		assertEquals(1, cs.countCloudObjects());
		assertEquals(filename, MoreComplexTestClass.invokeProtectedMethod(testClass));
		
		testClass.destroy();
		assertEquals(0, cs.countCloudObjects());			
	}
	
	@Test
	public void testInheritedMethod() 
	{

		assertEquals(0, cs.countCloudObjects());
		
		MoreComplexTestSubClass obj = new MoreComplexTestSubClass();
		assertEquals(1, cs.countCloudObjects());
		MoreComplexTestSubClass.invokeInheritedMethod(obj);
		obj.destroy();
		assertEquals(0, cs.countCloudObjects());
	}
	
	@Test
	public void testConstructionOfParent() { 

		assertEquals(0, cs.countCloudObjects());
		
		@SuppressWarnings("unused")
		MoreComplexTestSuperClass co = new MoreComplexTestSuperClass();
		
		assertEquals(0, cs.countCloudObjects());
		
	}
	
	@Test
	public void testNoDefaultConstructor() { 

		assertEquals(0, cs.countCloudObjects());
		
		@SuppressWarnings("unused")
		TestCloudObject3 co = new TestCloudObject3("hugo");
		
		assertEquals(1, cs.countCloudObjects());
		
	}
	
	@Test
	public void testMethodOfSuperclass() 
	{

		assertEquals(0, cs.countCloudObjects());
		
		MoreComplexTestSubClass obj = new MoreComplexTestSubClass();
		assertEquals(1, cs.countCloudObjects());
		
		MoreComplexTestSuperClass.isLocal = true; 
		assertFalse(obj.isRunningLocal());
		
	}
	
	@Test
	public void testObjectCountWithRemoveInSubclass() throws InterruptedException {
	
		assertEquals(0, cs.countCloudObjects());
	
		TestSubCloudObject1 ob = new TestSubCloudObject1();
	
		assertEquals(1, cs.countCloudObjects());
	
		ob.killMeSoftly();
	
		assertEquals(0, cs.countCloudObjects());
	
	}
	
	@Test
	public void testObjectCount3WithReflection() throws Exception {
		
		assertEquals(0, cs.countCloudObjects());
		
		Constructor<TestCloudObject1> constructor =
				TestCloudObject1.class.getConstructor();
		
		TestCloudObject1[] co = new TestCloudObject1[3];
		for(int i=0; i < co.length; i++)
			co[i] = constructor.newInstance();
		
		assertEquals(3, cs.countCloudObjects());		
		
	}
	
	@Test
	public void testObjectCount3WithReflectionNoArg() throws Exception {
		
		assertEquals(0, cs.countCloudObjects());
		
		TestCloudObject1[] co = new TestCloudObject1[3];
		for(int i=0; i < co.length; i++)
			co[i] = TestCloudObject1.class.newInstance();
		
		assertEquals(3, cs.countCloudObjects());		
		
	}
	
	@Test
	public void testWithSubclass() {
		
		assertEquals(0, cs.countCloudObjects());
		
		@SuppressWarnings("unused")
		TestSubCloudObject1 co = new TestSubCloudObject1();
		
		assertEquals(1, cs.countCloudObjects());		
		
	}
	
	@Test
	public void testWithSubclassAndRemove() {
		
		assertEquals(0, cs.countCloudObjects());
		
		TestSubCloudObject1 sub = new TestSubCloudObject1();
		
		assertEquals(1, cs.countCloudObjects());		
		
		sub.killMeSoftly();
		
		assertEquals(0, cs.countCloudObjects());
		
	}
	
	@Test
	public void testMethodWithReflection() throws Exception {
		
		TestCloudObject1.isLocal = true;
		
		assertEquals(0,cs.countCloudObjects());
		
		TestCloudObject1 obj = new TestCloudObject1();
		Method m = TestCloudObject1.class.getMethod("executingLocal");
		assertEquals(false, m.invoke(obj, (Object[])null));
		obj.killMeSoftly();
		
		assertEquals(0,cs.countCloudObjects());		
		
	}
	
	@Test
	public void testInterfaceInvocation() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		
		ITestObject obj = new TestCloudObject1();
		TestCloudObject1.isLocal = true;
		assertFalse(obj.executingLocal());
		obj.killMeSoftly();
		
	}
	
}
