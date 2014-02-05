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
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.cache.CacheType;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.SerializableDataType;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject1;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject1.MyEnum;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject2;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestLocalObject1;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class TestBasic { 
	
	protected static CloudManager cs = null;
	
	@Test
	public void testObjectCount1() {
		
		assertEquals(0, cs.countCloudObjects());
		
		@SuppressWarnings("unused")
		TestCloudObject1 co = new TestCloudObject1();
		
		assertEquals(1, cs.countCloudObjects());		
		
	}
	
	@Test
	public void testObjectCount3() {
		
		assertEquals(0, cs.countCloudObjects());
		
		TestCloudObject1[] co = new TestCloudObject1[3];
		for(int i=0; i < co.length; i++)
			co[i] = new TestCloudObject1();
		
		assertEquals(3, cs.countCloudObjects());		
		
	}
	
	@Test
	public void testImplicitConstructor() {
		
		assertEquals(0, cs.countCloudObjects());
		
		@SuppressWarnings("unused")
		TestCloudObject2 co = new TestCloudObject2();
		
		assertEquals(1, cs.countCloudObjects());		
		
	}
	
	@Test
	public void testObjectCountWithRemove() throws InterruptedException {
	
		assertEquals(0, cs.countCloudObjects());
	
		TestCloudObject1 ob = new TestCloudObject1();
	
		assertEquals(1, cs.countCloudObjects());
	
		ob.killMeSoftly();
	
		assertEquals(0, cs.countCloudObjects());
	
	}

	@Test
	public void testObjectCount10WithRemove() throws InterruptedException {
		
		assertEquals(0,cs.countCloudObjects());
		
		TestCloudObject1[] fives = new TestCloudObject1[5];
		TestCloudObject1[] otherFives = new TestCloudObject1[5];
		for(int i=0; i<10; i++) {
			if(i < 5)
				fives[i] = new TestCloudObject1();
			else
				otherFives[i-5] = new TestCloudObject1();
		}
				
		assertEquals(10, cs.countCloudObjects());
		
		for(TestCloudObject1 o : fives)
			o.killMeSoftly();
		
		assertEquals(5, cs.countCloudObjects());
		
	}
	
	@Test
	public void testParameterMethod() throws InterruptedException {
		
		assertEquals(0,cs.countCloudObjects());
		
		TestCloudObject1 obj = new TestCloudObject1();
		assertEquals("6", obj.multiplyAndConvert(2, 3));
		obj.killMeSoftly();
		
		assertEquals(0,cs.countCloudObjects());
		
	}
	
	@Test
	public void testEnumAsParameterMethod() throws Exception 
	{
		assertEquals(0,cs.countCloudObjects());
		
		TestCloudObject1 obj = new TestCloudObject1();
		
		// checking system enums
		assertEquals(TimeUnit.MINUTES.toString(), obj.convertToStringEnum(TimeUnit.MINUTES));
		
		// checking user-defined enums
		assertEquals(CacheType.NoCache.toString(), obj.converToStringUserEnum(CacheType.NoCache));
		
		// checking user-defined inner enums as return parameters
		assertEquals(MyEnum.Small, obj.getObjectSize("cow"));
		assertEquals(MyEnum.Big, obj.getObjectSize("universe"));
		
		obj.killMeSoftly();
		
		assertEquals(0,cs.countCloudObjects());
	}
	
	@Test
	public void testNondefaultConstructor() throws InterruptedException {
		
		assertEquals(0,cs.countCloudObjects());
		
		@SuppressWarnings("unused")
		TestCloudObject1 co = new TestCloudObject1("test");

		assertEquals(1,cs.countCloudObjects());
		
	}
	
	@Test
	public void testNonLocal() throws InterruptedException {
		
		TestCloudObject1.isLocal = true;
		TestLocalObject1.isLocal = true;
		
		TestCloudObject1 obj = new TestCloudObject1("test");
		TestLocalObject1 local = new TestLocalObject1("test");
		
		assertFalse(obj.executingLocal());
		assertTrue(local.executingLocal());
		
		
	}
	
	@Test
	public void testGettersAndSetters() throws InterruptedException {
		
		TestCloudObject1 obj1 = new TestCloudObject1();
		TestCloudObject1 obj2 = new TestCloudObject1();
		
		obj1.setTestField("test");
		obj2.setTestField("hugo");
		
		assertEquals("test", obj1.getTestField());
		assertEquals("hugo", obj2.getTestField());
		
	}
	
	@Test
	public void testSerializableDataType() throws InterruptedException {
		
		TestCloudObject1 obj1 = new TestCloudObject1();
		SerializableDataType ser = new SerializableDataType();
		ser.setA(3);
		ser.setB(2);
		ser.setSomeId(UUID.randomUUID());
		
		obj1.setSerial(ser);
		
		assertEquals(3, obj1.getSerial().getA());
		assertEquals(2, obj1.getSerial().getB());
		assertEquals(ser.getSomeId(), obj1.getSerial().getSomeId());
		
	}
	
	@Test
	public void testMethodInvocationInScalingPolicy() throws Exception
	{
		//altering scaling policy configuration to call some methods on cloudObject prior to work
		JCloudScaleConfiguration config = JCloudScaleConfiguration.getConfiguration();
		final AbstractScalingPolicy oldPolicy = config.common().scalingPolicy();
		config.common().setScalingPolicy(new AbstractScalingPolicy() {
			@Override
			public IHost selectHost(ClientCloudObject newCloudObject, IHostPool hostPool) {
				System.out.printf("Doing something with object %s of type %s with hashCode %s. %n", 
						newCloudObject.getProxy(), newCloudObject.getCloudObjectClass(), newCloudObject.getProxy().hashCode());
				
				// and let's call something more.
				assertEquals("6", ((TestCloudObject1)newCloudObject.getProxy()).multiplyAndConvert(2, 3));
				
				return oldPolicy.selectHost(newCloudObject, hostPool);
}
			@Override
			public boolean scaleDown(IHost scaledHost, IHostPool hostPool) {
				return oldPolicy.scaleDown(scaledHost, hostPool);
			}
		});
		
		try
		{
			final int X = 15;
			final int Y = 44;
			
			assertEquals(Integer.toString(X * Y), new TestCloudObject1().multiplyAndConvert(X, Y));
		}
		finally
		{
			//restoring previous scaling policy
			config.common().setScalingPolicy(oldPolicy);
		}
	}
	
}
