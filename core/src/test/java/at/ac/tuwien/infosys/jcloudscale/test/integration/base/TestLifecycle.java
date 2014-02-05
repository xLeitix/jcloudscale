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

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import at.ac.tuwien.infosys.jcloudscale.annotations.FileDependency;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.cache.CacheType;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.CloudObjectWithExecTime;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.ObjectWithIdField;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.ObjectWithIdFieldAndCCOField;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.SerializableDataType;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject1;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject1.MyEnum;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject2;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestLocalObject1;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudObjectState;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class TestLifecycle { 
	
	protected static CloudManager cs = null;
	
	@Test
	public void testGetClientObjectViaCM() {
		
		assertEquals(0, cs.countCloudObjects());
		
		ObjectWithIdField co = new ObjectWithIdField();
		
		assertEquals(1, cs.countCloudObjects());	
		
		ClientCloudObject cco = cs.getClientCloudObject(co);
		
		// note that (co == cco.getProxy()) is true, but
		// co.equals(cco.getProxy()) is false! (reason -> co.equals(..) goes to the server)
		// Is this expected? Should we find a way to work around this?
		assertTrue(co == cco.getProxy());
		assertFalse(co.equals(cco.getProxy()));
		
		// note: those are not exactly the same classes, as co.getClass() is a CGLib generated subclass
		assertTrue(cco.getCloudObjectClass().isAssignableFrom(co.getClass()));
		assertEquals(co.getId(), cco.getId());
		
	}
	
	@Test
	public void testInjectClientObject() {
		
		assertEquals(0, cs.countCloudObjects());
		
		ObjectWithIdFieldAndCCOField co = new ObjectWithIdFieldAndCCOField();
		
		assertEquals(1, cs.countCloudObjects());	
		
		ClientCloudObject cco = co.getCCO();
		
		assertTrue(co == cco.getProxy());
		assertFalse(co.equals(cco.getProxy()));
		assertTrue(cco.getCloudObjectClass().isAssignableFrom(co.getClass()));
		assertEquals(co.getId(), cco.getId());
		
	}
	
	@Test
	public void testObjectIdle() {
		
		assertEquals(0, cs.countCloudObjects());
		
		ObjectWithIdField co = new ObjectWithIdField();
		
		assertEquals(1, cs.countCloudObjects());	
		
		ClientCloudObject cco = cs.getClientCloudObject(co);
		
		assertEquals(cco.getState(), CloudObjectState.IDLE);
		
	}
	
	@Test
	public void testObjectRunning() throws InterruptedException {
		
		assertEquals(0, cs.countCloudObjects());
		
		final CloudObjectWithExecTime co = new CloudObjectWithExecTime();
		
		assertEquals(1, cs.countCloudObjects());	
		
		ClientCloudObject cco = cs.getClientCloudObject(co);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					co.waitAndReturn(5000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}).start();
		
		Thread.sleep(1000);
		assertEquals(CloudObjectState.EXECUTING, cco.getState());
		Thread.sleep(5000);
		assertEquals(CloudObjectState.IDLE, cco.getState());
		
	}
	
	
	@Test
	public void testObjectRemoved() {
		
		assertEquals(0, cs.countCloudObjects());
		
		TestCloudObject1 co = new TestCloudObject1();
		
		assertEquals(1, cs.countCloudObjects());	
		
		ClientCloudObject cco = cs.getClientCloudObject(co);
		
		assertEquals(cco.getState(), CloudObjectState.IDLE);
		co.killMeSoftly();
		assertEquals(cco.getState(), CloudObjectState.DESTRUCTED);
		
	}
	
	@Test
	public void testObjectRemovedByGC() throws InterruptedException {
		
		assertEquals(0, cs.countCloudObjects());
		
		TestCloudObject1 co = new TestCloudObject1();
		
		assertEquals(1, cs.countCloudObjects());	
		
		ClientCloudObject cco = cs.getClientCloudObject(co);
		
		assertEquals(CloudObjectState.IDLE, cco.getState());
		
		co = null;
		
		// desperate attempt to give the garbage collector something to clean up
		Object [] objects= new Object[100000];
		for(int i = 0; i< objects.length; i++)
			objects[i] = new Object();
		objects = null;
		
		
		Thread.sleep(10000);
		
		// this cannot work predictably :/ we'll certainly
		// have to @Ignore this test for Jenkins
		System.gc();
		
		Thread.sleep(4000);
		
		assertEquals(CloudObjectState.DESTRUCTED, cco.getState());
		
	}
	
	@Test
	public void testObjectRemovedByClosedClientExplicit() throws Exception {
		
		assertEquals(0, cs.countCloudObjects());
		
		TestCloudObject1 co = new TestCloudObject1();
		
		assertEquals(1, cs.countCloudObjects());	
		
		ClientCloudObject cco = cs.getClientCloudObject(co);
		
		assertEquals(CloudObjectState.IDLE, cco.getState());
		JCloudScaleClient.closeClient();
		assertEquals(CloudObjectState.DESTRUCTED, cco.getState());
		
	}
	
}
