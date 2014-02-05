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
package at.ac.tuwien.infosys.jcloudscale.test.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown;
import at.ac.tuwien.infosys.jcloudscale.monitoring.IMetricsCallback;
import at.ac.tuwien.infosys.jcloudscale.monitoring.IMetricsDatabase;
import at.ac.tuwien.infosys.jcloudscale.monitoring.InMemMetricsDatabase;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractEventDrivenScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;

public class TestInMemMetricsDB {
	
	private IMetricsDatabase db;
	
	@Before
	public void setup() 
	{
		JCloudScaleClient.setConfiguration(ConfigurationHelper.createDefaultTestConfiguration().build());
		
		this.db = new InMemMetricsDatabase();
	}
	
	@After
	@JCloudScaleShutdown
	public void clean(){}
	
	@Test
	public void testWriteAndReadValues() throws InterruptedException {
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", i);
			Thread.sleep(1);
		}
		
		assertTrue(db.getValues("TestMetric").size() == 20);
		assertTrue(db.getValues("TestMetric2") == null);
		assertTrue(db.getValues("TestMetric").toArray()[0] instanceof Integer);
		assertTrue(db.getValues("TestMetric").toArray()[19] instanceof Integer);
		
	}
	
	@Test
	public void testSameValues() throws InterruptedException {
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", 12.0f);
			Thread.sleep(1);
		}
		
		assertTrue(db.getValues("TestMetric").size() == 20);
		assertTrue(db.getValues("TestMetric2") == null);
		assertTrue(db.getValues("TestMetric").toArray()[0] instanceof Float);
		assertTrue(db.getValues("TestMetric").toArray()[19] instanceof Float);
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testComplexValues() throws InterruptedException {
		
		HashMap<String, Object> props = new HashMap<>();
		props.put("key1", new Object());
		props.put("key2", new Date());
		props.put("key3", "hugo");
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", props);
			Thread.sleep(1);
		}
		
		assertTrue(db.getValues("TestMetric").size() == 20);
		assertTrue(db.getValues("TestMetric2") == null);
		assertTrue(db.getValues("TestMetric").toArray()[0] instanceof HashMap<?, ?>);
		assertTrue(db.getValues("TestMetric").toArray()[19] instanceof HashMap<?, ?>);
		
		assertEquals("hugo", ((HashMap<String, Object>) db.getValues("TestMetric").toArray()[19]).get("key3"));
		
	}
	
	@Test
	public void testGetStringValues() throws InterruptedException {
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", new Integer(i).toString());
			Thread.sleep(1);
		}
		
		assertTrue(db.getValues("TestMetric").toArray()[0] instanceof String);
		assertTrue(db.getValues("TestMetric").toArray()[19] instanceof String);
		
	}
	
	@Test
	public void testGetLastValue() throws InterruptedException {
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", i);
			Thread.sleep(1);
		}
		
		assertTrue(((Integer)db.getLastValue("TestMetric")) == 19);
		
	}
	
	@Test
	public void testValuesChangingOverTime() throws InterruptedException {
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", i);
			assertEquals(i, db.getLastValue("TestMetric"));
			Thread.sleep(1);
		}
		
	}
	
	@Test
	public void testOrderOfValues() throws InterruptedException {
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", i);
			Thread.sleep(1);
		}
		
		Collection<Object> values = db.getValues("TestMetric");
		int j = 19;
		for(Object o : values) {
			assertEquals(j--, o);
		}
		
	}
	
	@Test
	public void testValuesSince() throws InterruptedException {
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", i);
			Thread.sleep(1);
		}
		
		long ts = System.currentTimeMillis();
		
		for(int i=20; i<40; i++) {
			db.addValue("TestMetric", i);
			Thread.sleep(1);
		}
		
		Collection<Object> values = db.getValuesSince("TestMetric", ts);
		int j = 39;
		for(Object o : values) {
			assertEquals(j--, o);
		}
		
	}
	
	@Test
	public void testMultipleMetrics() throws InterruptedException {
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetricInteger", i);
			db.addValue("TestMetricString", new Integer(i).toString());
			Thread.sleep(1);
		}
		
		Collection<Object> values = db.getValues("TestMetricInteger");
		int j = 19;
		for(Object o : values) {
			assertEquals(j--, o);
		}
		
		values = db.getValues("TestMetricString");
		j = 19;
		for(Object o : values) {
			assertEquals(((Integer)j--).toString(), o);
		}
		
	}
	
	@Test
	public void testGetTimestamp() throws InterruptedException {
		
		for(int i=0; i<10; i++) {
			db.addValue("TestMetricString", new Integer(i).toString());
			Thread.sleep(1);
		}
		
		long ts = System.currentTimeMillis();
		db.addValue("TestMetricString", new Integer(10).toString(), ts);
		
		for(int i=11; i<20; i++) {
			db.addValue("TestMetricString", new Integer(i).toString());
			Thread.sleep(1);
		}
		
		assertEquals(ts, db.getTimestampToValue("TestMetricString", new Integer(10).toString()));
		
	}
	
	@Test
	public void testRegistration() throws InterruptedException {
		
		final long ts = System.currentTimeMillis();
		
		IMetricsCallback callback = new IMetricsCallback() {
			
			@Override
			public void valueReceived(String metricName, Object value, long timestamp) {
				
				assertEquals("TestMetric", metricName);
				assertTrue(value instanceof Integer);
				assertEquals(new Integer(25), value);
				assertEquals(ts, timestamp);
				
			}
		};
		
		
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", i);
			Thread.sleep(1);
		}
		
		db.registerCallbackForMetric("TestMetric", callback);
		
		db.addValue("TestMetric", 25, ts);
		
		db.addValue("TestMetric2", 40, ts);
		
	}
	
	@Test
	public void testUnregistration() throws InterruptedException {
		
		final long ts = System.currentTimeMillis();
		
		IMetricsCallback callback = new IMetricsCallback() {
			
			@Override
			public void valueReceived(String metricName, Object value, long timestamp) {
				
				assertEquals("TestMetric", metricName);
				assertTrue(value instanceof Integer);
				assertEquals(new Integer(25), value);
				assertEquals(ts, timestamp);
				
			}
		};
		
		
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", i);
			Thread.sleep(1);
		}
		
		UUID regId = db.registerCallbackForMetric("TestMetric", callback);
		
		db.addValue("TestMetric", 25, ts);
		
		db.unregisterCallbackForMetric("TestMetric", regId);
		
		db.addValue("TestMetric", 40, ts);
		
	}
	
	@Test
	public void testMultipleRegistrants() throws InterruptedException {
		
		final long ts = System.currentTimeMillis();
		
		final AtomicInteger counter = new AtomicInteger(0);
		
		IMetricsCallback callback1 = new IMetricsCallback() {
			
			@Override
			public void valueReceived(String metricName, Object value, long timestamp) {
				
				assertEquals("TestMetric", metricName);
				assertTrue(value instanceof Integer);
				assertEquals(new Integer(25), value);
				assertEquals(ts, timestamp);
				
				counter.incrementAndGet();
				
			}
		};
		
		IMetricsCallback callback2 = new IMetricsCallback() {
			
			@Override
			public void valueReceived(String metricName, Object value, long timestamp) {
				
				assertEquals("TestMetric", metricName);
				assertTrue(value instanceof Integer);
				assertEquals(new Integer(25), value);
				assertEquals(ts, timestamp);
				
				counter.incrementAndGet();
				
			}
		};
		
		IMetricsCallback callback3 = new IMetricsCallback() {
			
			@Override
			public void valueReceived(String metricName, Object value, long timestamp) {
				
				assertEquals("TestMetric", metricName);
				assertTrue(value instanceof Integer);
				assertEquals(new Integer(25), value);
				assertEquals(ts, timestamp);
				
				counter.incrementAndGet();
				
			}
		};
		
		
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", i);
			Thread.sleep(1);
		}
		
		db.registerCallbackForMetric("TestMetric", callback1);
		db.registerCallbackForMetric("TestMetric", callback2);
		db.registerCallbackForMetric("TestMetric", callback3);
		
		db.addValue("TestMetric", 25, ts);
		
		assertEquals(counter.get(), 3);
		
	}
	
	@Test
	public void testScalingRegistration() throws InterruptedException, IOException {
		
		JCloudScaleClient.getClient();
		
		final long ts = System.currentTimeMillis();
		
		AbstractEventDrivenScalingPolicy policy = new AbstractEventDrivenScalingPolicy() {
			
			@Override
			public IHost selectHost(ClientCloudObject newCloudObject,
					IHostPool hostPool) {
				return null;
			}
			
			@Override
			public boolean scaleDown(IHost scaledHost, IHostPool hostPool) {
				return false;
			}
			
			@Override
			public void onEvent(IHostPool hostPool, Object triggeringValue,
					long timestamp) {
				
				assertNotNull(hostPool);
				assertTrue(triggeringValue instanceof Integer);
				assertEquals(new Integer(25), triggeringValue);
				assertEquals(ts, timestamp);
				
			}
		};
		
		
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", i);
			Thread.sleep(1);
		}
		
		db.registerEventDrivenScalingPolicy("TestMetric", policy);
		
		db.addValue("TestMetric", 25, ts);
		
		db.addValue("TestMetric2", 40, ts);
		
		JCloudScaleClient.closeClient();
		
	}
	
	@Test
	public void testScalingUnregistration() throws InterruptedException, IOException {
		
		JCloudScaleClient.getClient();
		
		final long ts = System.currentTimeMillis();
		
		AbstractEventDrivenScalingPolicy policy = new AbstractEventDrivenScalingPolicy() {
			
			@Override
			public IHost selectHost(ClientCloudObject newCloudObject,
					IHostPool hostPool) {
				return null;
			}
			
			@Override
			public boolean scaleDown(IHost scaledHost, IHostPool hostPool) {
				return false;
			}
			
			@Override
			public void onEvent(IHostPool hostPool, Object triggeringValue,
					long timestamp) {
				
				assertNotNull(hostPool);
				assertTrue(triggeringValue instanceof Integer);
				assertEquals(new Integer(25), triggeringValue);
				assertEquals(ts, timestamp);
				
			}
		};
		
		
		
		for(int i=0; i<20; i++) {
			db.addValue("TestMetric", i);
			Thread.sleep(1);
		}
		
		UUID regId = db.registerEventDrivenScalingPolicy("TestMetric", policy);
		
		db.addValue("TestMetric", 25, ts);
		
		db.unregisterEventDrivenScalingPolicy("TestMetric", regId);
		
		db.addValue("TestMetric", 40, ts);
		
		JCloudScaleClient.closeClient();
		
	}
	
//	
//	@Test
//	public void testGetPerformanceOfLargeNumberOfMetrics() throws InterruptedException {
//		
//		// simple performance test - add 100.000 values and see if we are still able to deliver the latest
//		// value in less than a second (if it is much slower than that it is probably not actually
//		// very useful)
//		
//		for(int i=0; i<100000; i++) {
//			db.addValue("TestMetric", i);
//			Thread.sleep(1);
//		}
//		
//		long before = System.currentTimeMillis();
//		db.getLastValue("TestMetric");
//		long after = System.currentTimeMillis();
//		
//		assertTrue(after - 1000 <= before);
//		
//	}
//	
//	@Test
//	public void testSetPerformanceOfLargeNumberOfMetrics() throws InterruptedException {
//		
//		// simple performance test - add 100.000 values and how long it takes
//		// to write the last value
//		// (should be less than 10 ms)
//		
//		for(int i=0; i<100000; i++) {
//			db.addValue("TestMetric", i);
//			Thread.sleep(1);
//		}
//		
//		long before = System.currentTimeMillis();
//		db.addValue("TestMetric", 100000);
//		long after = System.currentTimeMillis();
//		
//		assertTrue(after - 10 <= before);
//		
//	}
	
	
	
}