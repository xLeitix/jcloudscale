package at.ac.tuwien.infosys.jcloudscale.test.integration.base;
///*
//   Copyright 2013 Philipp Leitner
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//*/
//package at.ac.tuwien.infosys.jcloudscale.test.integration.base;
//
//import org.junit.After;
//import org.junit.Before;
//
//import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown;
//import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
//import at.ac.tuwien.infosys.jcloudscale.test.testobject.ObjectWithInfo;
//import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
//import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
//
//public class TestInjection {
//	
//	private CloudManager cs = null;
//	
//	private ObjectWithInfo obj;
//	
//	@Before
//	public void setup() throws Exception {
//		
//		JCloudScaleClient.setConfiguration(ConfigurationHelper.createDefaultTestConfiguration()
//				.build());
//		
//		// we have to get client prior to using CloudManager instance to initialize JCloudScale infrastructure (partially -- message queue). 
//		JCloudScaleClient.getClient();
//		
//		cs = CloudManager.getInstance();
//	}
//	
//	@After
//	@JCloudScaleShutdown
//	public void tearDown() throws Exception {
//	}
//	
////	@Test
////	public void testAccessId() {
////		
////		assertEquals(0, cs.countCloudObjects());
////		
////		ObjectWithIdField obj = new ObjectWithIdField();
////		
////		// client
////		assertNotNull(obj.id);
////		
////		// server
////		assertNotNull(obj.getId());
////		
////		assertEquals(1, cs.countCloudObjects());		
////		
////	}
////	
////	@Test
////	public void testAccessMultipleIds() {
////		
////		assertEquals(0, cs.countCloudObjects());
////		
////		ObjectWithMultipleIdFields obj = new ObjectWithMultipleIdFields();
////		
////		// client
////		assertNotNull(obj.id1);
////		assertEquals(obj.id1, UUID.fromString(obj.id2));
////		assertEquals(obj.id2, obj.id3.toString());
////		
////		// server
////		assertNotNull(obj.getId1());
////		assertEquals(obj.getId1(), UUID.fromString(obj.getId2()));
////		assertEquals(obj.getId2(), obj.getId3().toString());
////		
////		assertEquals(1, cs.countCloudObjects());		
////		
////	}
////	
////	@Test
////	public void testAccessPrivateId() {
////		
////		assertEquals(0, cs.countCloudObjects());
////		ObjectWithPrivateIdField obj = new ObjectWithPrivateIdField();
////		assertNotNull(obj.getId());
////		assertEquals(1, cs.countCloudObjects());		
////		
////	}
////	
////	@Test(expected=IllegalDefinitionException.class)
////	public void testAccessWrongId() {
////		
////		assertEquals(0, cs.countCloudObjects());
////		new ObjectWithWrongIdField();
////		
////	}
////	
////	@Test
////	public void testInvocationInfo() throws InterruptedException {
////		
////		assertEquals(0, cs.countCloudObjects());
////		obj = new ObjectWithInfo();
////		assertNull(obj.infos);
////		
////		new Thread() {
////			
////			@Override
////			public void run() {
////				try {
////					obj.doStuff(2000);
////				} catch (InterruptedException e) {
////					e.printStackTrace();
////				}
////			}
////			
////		}.start();
////		
////		Thread.sleep(1000);
////		
////		assertNotNull(obj.infos);
////		assertEquals("doStuff", obj.infos.get(0).getMethodName());
////		
////		Thread.sleep(2000);
////		
////		assertEquals(0, obj.infos.size());
////		
////	}
//	
//	// FIXME
//	
////	@Test
////	public void testMultipleInvocationInfos() throws InterruptedException {
////		
////		assertEquals(0, cs.countCloudObjects());
////		obj = new ObjectWithInfo();
////		assertNull(obj.infos);
////		
////		for(int i=0; i<5; i++) {
////			new Thread() {
////				
////				@Override
////				public void run() {
////					try {
////						obj.doStuff(2000);
////					} catch (InterruptedException e) {
////						e.printStackTrace();
////					}
////				}
////				
////			}.start();
////		}
////		
////		Thread.sleep(1000);
////		
////		assertNotNull(obj.infos);
////		assertEquals(5, obj.infos.size());
////		assertEquals("doStuff", obj.infos.get(0).getMethodName());
////		assertEquals("doStuff", obj.infos.get(1).getMethodName());
////		assertEquals("doStuff", obj.infos.get(4).getMethodName());
////		
////		assertNotSame(obj.infos.get(0), obj.infos.get(4));
////		
////		Thread.sleep(2000);
////		
////		assertEquals(0, obj.infos.size());
////		
////	}
////	
////	@Test
////	public void testMultipleInvocationInfosServerside() throws InterruptedException {
////		
////		assertEquals(0, cs.countCloudObjects());
////		obj = new ObjectWithInfo();
////		
////		for(int i=0; i<5; i++) {
////			new Thread() {
////				
////				@Override
////				public void run() {
////					try {
////						obj.doStuff(2000);
////					} catch (InterruptedException e) {
////						e.printStackTrace();
////					}
////				}
////				
////			}.start();
////		}
////		
////		Thread.sleep(1000);
////		
////		assertNotNull(obj.getInfos());
////		assertEquals(5, obj.getInfos().size());
////		assertEquals("doStuff", obj.getInfos().get(0).getMethodName());
////		assertEquals("doStuff", obj.getInfos().get(1).getMethodName());
////		assertEquals("doStuff", obj.getInfos().get(4).getMethodName());
////		
////		assertNotSame(obj.getInfos().get(0), obj.getInfos().get(4));
////		
////		Thread.sleep(2000);
////		
////		assertEquals(0, obj.getInfos().size());
////		
////	}
//}
