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

import java.util.UUID;

import javax.jms.JMSException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ExecutionFailedEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ExecutionFinishedEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ExecutionStartedEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ObjectCreatedEvent;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.ObjectDestroyedEvent;
import at.ac.tuwien.infosys.jcloudscale.monitoring.EventCorrelationEngine;
import at.ac.tuwien.infosys.jcloudscale.monitoring.IMetricsDatabase;
import at.ac.tuwien.infosys.jcloudscale.monitoring.MonitoringMetric;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.CloudObjectWithCustomEvents;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.CloudObjectWithExecTime;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.FaultingCloudObject1;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.MyCustomEvent;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.ObjectWithIdField;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject1;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestSameVMCloudObject;
 
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class TestMonitoring {
      
    protected static CloudManager cs = null;
      
    @Test
    public void testReceiveStarted1() throws InterruptedException {
         
        assertEquals(0, cs.countCloudObjects());
         
        TestCloudObject1 ob = new TestCloudObject1();
     
        assertEquals(1, cs.countCloudObjects());
         
        MonitoringMetric metric = new MonitoringMetric();
        metric.setName("TestMetric1");
        metric.setEpl("select * from ExecutionStartedEvent");
        EventCorrelationEngine.getInstance().registerMetric(metric);
        ob.killMeSoftly();
         
        Thread.sleep(2000);
         
        IMetricsDatabase db = EventCorrelationEngine.getInstance().getMetricsDatabase();
        assertEquals(1, db.getValues("TestMetric1").size());
        Object o = db.getValues("TestMetric1").iterator().next();
        assertTrue(o instanceof ExecutionStartedEvent);
        ExecutionStartedEvent event = (ExecutionStartedEvent)o;
        assertEquals("killMeSoftly", event.getMethod());
         
        assertEquals(0, cs.countCloudObjects());
         
    }
     
    @Test
    public void testReceiveStarted10() throws InterruptedException {
         
        assertEquals(0, cs.countCloudObjects());
         
        MonitoringMetric metric = new MonitoringMetric();
        metric.setName("TestMetric2");
        metric.setEpl("select * from ExecutionStartedEvent");
        EventCorrelationEngine.getInstance().registerMetric(metric);
         
        for(int i=0; i<10; i++)
            new TestSameVMCloudObject().killMeSoftly();
         
        Thread.sleep(2000);
         
        IMetricsDatabase db = EventCorrelationEngine.getInstance().getMetricsDatabase();
        assertEquals(10, db.getValues("TestMetric2").size());
         
        assertEquals(0, cs.countCloudObjects());
         
    }
     
    @Test
    public void testExecutionFinished() throws InterruptedException {
         
        assertEquals(0, cs.countCloudObjects());
         
        MonitoringMetric metric = new MonitoringMetric();
        metric.setName("TestMetric3");
        metric.setEpl("select * from ExecutionFinishedEvent");
        EventCorrelationEngine.getInstance().registerMetric(metric);
         
        new TestSameVMCloudObject().killMeSoftlyWithReturn();
         
        Thread.sleep(2000);
         
        IMetricsDatabase db = EventCorrelationEngine.getInstance().getMetricsDatabase();
        assertEquals(1, db.getValues("TestMetric3").size());
        Object o = db.getValues("TestMetric3").iterator().next();
        assertTrue(o instanceof ExecutionFinishedEvent);
        ExecutionFinishedEvent event = (ExecutionFinishedEvent)o;
        assertEquals("killMeSoftlyWithReturn", event.getMethod());
         
        assertEquals(0, cs.countCloudObjects());
         
    }
     
    @Test
    public void testExecutionFailed() throws InterruptedException {
         
        assertEquals(0, cs.countCloudObjects());
         
        MonitoringMetric metric = new MonitoringMetric();
        metric.setName("TestMetric4");
        metric.setEpl("select * from ExecutionFailedEvent");
        EventCorrelationEngine.getInstance().registerMetric(metric);
         
        FaultingCloudObject1 faulty = new FaultingCloudObject1();
        try {
            faulty.faulty();
        } catch(Exception e) {}
         
        Thread.sleep(2000);
         
        IMetricsDatabase db = EventCorrelationEngine.getInstance().getMetricsDatabase();
        assertEquals(1, db.getValues("TestMetric4").size());
        Object o = db.getValues("TestMetric4").iterator().next();
        assertTrue(o instanceof ExecutionFailedEvent);
        ExecutionFailedEvent event = (ExecutionFailedEvent)o;
        assertEquals("faulty", event.getMethod());
        assertEquals(JCloudScaleException.class, event.getException().getClass());
         
    }
     
    @Test
    public void testObjectCreated() throws InterruptedException {
         
        assertEquals(0, cs.countCloudObjects());
         
        MonitoringMetric metric = new MonitoringMetric();
        metric.setName("TestMetric5");
        metric.setEpl("select * from ObjectCreatedEvent");
        EventCorrelationEngine.getInstance().registerMetric(metric);
         
        UUID id = new ObjectWithIdField().getId();
         
        Thread.sleep(2000);
         
        IMetricsDatabase db = EventCorrelationEngine.getInstance().getMetricsDatabase();
        assertEquals(1, db.getValues("TestMetric5").size());
        Object o = db.getValues("TestMetric5").iterator().next();
        assertTrue(o instanceof ObjectCreatedEvent);
        ObjectCreatedEvent event = (ObjectCreatedEvent)o;
        assertEquals(id, event.getObjectId());
        assertEquals(ObjectWithIdField.class, event.getObjectType());
         
    }
     
    @Test
    public void testObjectDestroyed() throws InterruptedException {
         
        assertEquals(0, cs.countCloudObjects());
         
        MonitoringMetric metric = new MonitoringMetric();
        metric.setName("TestMetric6");
        metric.setEpl("select * from ObjectDestroyedEvent");
        EventCorrelationEngine.getInstance().registerMetric(metric);
         
        new TestSameVMCloudObject().killMeSoftly();
         
        Thread.sleep(2000);
         
        IMetricsDatabase db = EventCorrelationEngine.getInstance().getMetricsDatabase();
        assertEquals(1, db.getValues("TestMetric6").size());
        Object o = db.getValues("TestMetric6").iterator().next();
        assertTrue(o instanceof ObjectDestroyedEvent);
         
        assertEquals(0, cs.countCloudObjects());
         
    }
     
    @Test
    public void testCombinedMetric() throws InterruptedException {
         
        assertEquals(0, cs.countCloudObjects());
         
        MonitoringMetric metric = new MonitoringMetric();
        metric.setName("TestMetric7");
        metric.setEpl(
            "select (finish.timestamp - started.timestamp) as responsetime " +
            "from ExecutionFinishedEvent.win:keepall() as finish, ExecutionStartedEvent.win:keepall() as started " +
            "where started.requestId = finish.requestId");
        metric.setResultField("responsetime");
        EventCorrelationEngine.getInstance().registerMetric(metric);
         
        new TestSameVMCloudObject().killMeSoftly();
         
        Thread.sleep(2000);
        
        IMetricsDatabase db = EventCorrelationEngine.getInstance().getMetricsDatabase();
        assertEquals(1, db.getValues("TestMetric7").size());
         
    }
     
    @Test
    public void testCombinedMetricWithActualExecTime() throws InterruptedException {
         
        assertEquals(0, cs.countCloudObjects());
         
        MonitoringMetric metric = new MonitoringMetric();
        metric.setName("TestMetric8");
        metric.setEpl(
            "select (finish.timestamp - started.timestamp) as responsetime " +
            "from ExecutionFinishedEvent.win:keepall() as finish, ExecutionStartedEvent.win:keepall() as started " +
            "where started.requestId = finish.requestId and started.objectType.getName() = '"
            + CloudObjectWithExecTime.class.getName()+"'");
        metric.setResultField("responsetime");
        EventCorrelationEngine.getInstance().registerMetric(metric);
         
        new CloudObjectWithExecTime().waitAndReturn(2000);
        
        Thread.sleep(2000);
        
        IMetricsDatabase db = EventCorrelationEngine.getInstance().getMetricsDatabase();
        assertEquals(1, db.getValues("TestMetric8").size());
        Object o = db.getValues("TestMetric8").iterator().next();
        assertTrue(o instanceof Long);
        assertTrue(between((Long)o, 3000, 250));
         
        new CloudObjectWithExecTime().waitAndReturn(1000);
        
        Thread.sleep(2000);
        
        db = EventCorrelationEngine.getInstance().getMetricsDatabase();
        assertEquals(2, db.getValues("TestMetric8").size());
        o = db.getValues("TestMetric8").iterator().next();
        assertTrue(o instanceof Long);
        assertTrue(between((Long)o, 2000, 250));
         
        new CloudObjectWithExecTime().waitAndReturn(5000);
        
        Thread.sleep(2000);
        
        db = EventCorrelationEngine.getInstance().getMetricsDatabase();
        assertEquals(3, db.getValues("TestMetric8").size());
        o = db.getValues("TestMetric8").iterator().next();
        assertTrue(o instanceof Long);
        assertTrue(between((Long)o, 7000, 250));
         
    }
    
	@Test
    public void testTriggerCustomEvent() throws InterruptedException, JMSException {
         
        assertEquals(0, cs.countCloudObjects());
        
        MonitoringMetric metric = new MonitoringMetric();
        metric.setName("TestMetric9");
        metric.setEpl("select * from MyCustomEvent.win:keepall() as custom");
        EventCorrelationEngine.getInstance().registerMetric(metric);
        
        CloudObjectWithCustomEvents obj = new CloudObjectWithCustomEvents();
        obj.triggerEvent("hugo");
        
        Thread.sleep(2000);
        
        IMetricsDatabase db = EventCorrelationEngine.getInstance().getMetricsDatabase();
        Object o = db.getValues("TestMetric9").iterator().next();
        assertTrue(o instanceof MyCustomEvent);
        assertEquals("hugo", ((MyCustomEvent)o).getCustomField());
         
    }
    
//	  @Test
//	    public void testCPUEvent() throws InterruptedException {
//	         
//	        assertEquals(0, cs.countCloudObjects());
//	        
//	        BusyTestObject obj1 = new BusyTestObject();
//	        
//	        String hostId = cs.getHost(obj1.coId).getId().toString();
//	        
//	        MonitoringMetric metric = new MonitoringMetric();
//	        metric.setName("TestMetric10");
//	        metric.setEpl(String.format(
//	        		"select avg(cpuLoad) as avg_load from CPUEvent(hostId.toString() = '%s').win:time(30 sec)",
//	        		hostId));
//	        metric.setResultField("avg_load");
//	        EventCorrelationEngine.getInstance().registerMetric(metric);
//	        
//	        
//	        BusyTestObject obj2 = new BusyTestObject();
//	        BusyTestObject obj3 = new BusyTestObject();
//	        BusyTestObject obj4 = new BusyTestObject();
//	        
//	        Thread.sleep(60000);
//	        
//	        IMetricsDatabase db = EventCorrelationEngine.getInstance().getMetricsDatabase();
//	        double oldCpu = (Double)db.getLastValue("TestMetric10");
//	        
//	        obj1.beBusy(27000);
//	        obj2.beBusy(27000);
//	        obj3.beBusy(27000);
//	        obj4.beBusy(27000);
//	        
//	        Thread.sleep(15000);
//	        
//	        double newCpu = (Double)db.getLastValue("TestMetric10");
//	        assertTrue("OldCPU = "+oldCpu+", NewCPU = " + newCpu, newCpu > oldCpu);
//	        
//	        obj1.destroy();
//	        obj2.destroy();
//	        obj3.destroy();
//	        obj4.destroy();
//	        
//	        assertEquals(0, cs.countCloudObjects());
//	        
//	        Thread.sleep(60000);
//	        
//	    }
//	    
//	    @Test
//	    public void testRAMEvent() throws InterruptedException {
//	         
//	        assertEquals(0, cs.countCloudObjects());
//	         
//	        MonitoringMetric metric = new MonitoringMetric();
//	        metric.setName("TestMetric11");
//	        metric.setEpl("select avg(usedMemory) as avg_usage from RAMEvent.win:time(60 sec)");
//	        metric.setResultField("avg_usage");
//	        EventCorrelationEngine.getInstance().registerMetric(metric);
//	        
//	        BusyTestObject obj1 = new BusyTestObject();
//	        BusyTestObject obj2 = new BusyTestObject();
//	        BusyTestObject obj3 = new BusyTestObject();
//	        BusyTestObject obj4 = new BusyTestObject();
//	        
//	        Thread.sleep(60000);
//	        
//	        IMetricsDatabase db = EventCorrelationEngine.getInstance().getMetricsDatabase();
//	        double oldRam = (Double)db.getLastValue("TestMetric11");
//	        
//	        obj1.beBusy(25450);
//	        obj2.beBusy(25450);
//	        obj3.beBusy(25450);
//	        obj4.beBusy(25450);
//	         
//	        Thread.sleep(10000);
//	        
//	        double newRam = (Double)db.getLastValue("TestMetric11");
//	        assertTrue(newRam > oldRam);
//	        
//	        obj1.destroy();
//	        obj2.destroy();
//	        obj3.destroy();
//	        obj4.destroy();
//	         
//	        assertEquals(0, cs.countCloudObjects());
//	        
//	        Thread.sleep(60000);
//	        
//	   }
//    
//    @Test
//    public void testRAMandCPUViaAPI() throws InterruptedException {
//    	
//    	IScalingPolicy old =
//    			JCloudScaleConfiguration.getConfiguration().common().scalingPolicy(); 
//    	
//    	JCloudScaleConfiguration.getConfiguration().common().setScalingPolicy(
//    			new IScalingPolicy() {
//					
//					@Override
//					public IHost selectHost(ClientCloudObject newCloudObject,
//							IVirtualHostPool hostPool) {
//						
//						for(IHost host : hostPool.getHosts()) {
//							assertTrue(host.getCurrentRAMUsage() != null);
//							assertTrue(host.getCurrentRAMUsage().getUsedMemory() > 0);
//							assertTrue(host.getCurrentCPULoad() != null);
//							assertTrue(host.getCurrentCPULoad().getCpuLoad() > 0);
//						}
//						return null;
//						
//					}
//					
//					@Override
//					public boolean scaleDown(IHost scaledHost, IVirtualHostPool hostPool) {
//						return false;
//					}
//				}
//    	);
//    	
//    	for(int i=0; i<5; i++) {
//    		new TestCloudObject1();
//    		Thread.sleep(10000);
//    	}
//    	
//    	JCloudScaleConfiguration.getConfiguration().common().setScalingPolicy(old);
//    	
//    }
    
    private boolean between(Long o, int i, int j) {
		return (o <=i && o >= j);
	}
     
}