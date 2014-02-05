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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.migration.IMigrationEnabledHostPool;
import at.ac.tuwien.infosys.jcloudscale.migration.IMigrationEnabledHostPool.IObjectMigratedCallback;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.SerializableCloudObject;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject1;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudObjectState;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class TestMigration { 
	
	protected static CloudManager cs = null;
	
	private String fileName = "target"+File.separator+"test-classes"+File.separator+"largeFile.txt";
	
	@Test
	public void testBasicMigrationViaHostPool() {
		
		assertEquals(0, cs.countCloudObjects());
		
		SerializableCloudObject co1 = new SerializableCloudObject();
		ClientCloudObject cco1 = cs.getClientCloudObject(co1);
		SerializableCloudObject co2 = new SerializableCloudObject();
		ClientCloudObject cco2 = cs.getClientCloudObject(co2);
		
		assertEquals(2, cs.countCloudObjects());
		
		IHost managingHost1 = cs.getHost(cco1.getId());
		IHost managingHost2 = cs.getHost(cco2.getId());
		
		assertNotEquals(managingHost1.getId(), managingHost2.getId());
		cs.getHostPool().migrateObject(cco1.getId(), managingHost2);
		
		assertEquals(0, managingHost1.getCloudObjectsCount());
		assertEquals(2, managingHost2.getCloudObjectsCount());
		
		cco1 = cs.getClientCloudObject(co1);
		managingHost1 = cs.getHost(cco1.getId());;
		assertEquals(managingHost1.getId(), managingHost2.getId());
		
	}
	
	@Test
	public void testBasicMigrationViaHostPoolAsync() throws InterruptedException {
		
		assertEquals(0, cs.countCloudObjects());
		
		SerializableCloudObject co1 = new SerializableCloudObject();
		ClientCloudObject cco1 = cs.getClientCloudObject(co1);
		SerializableCloudObject co2 = new SerializableCloudObject();
		ClientCloudObject cco2 = cs.getClientCloudObject(co2);
		
		assertEquals(2, cs.countCloudObjects());
		
		IHost managingHost1 = cs.getHost(cco1.getId());
		IHost managingHost2 = cs.getHost(cco2.getId());
		
		final Semaphore sem = new Semaphore(1);
		assertNotEquals(managingHost1.getId(), managingHost2.getId());
		IObjectMigratedCallback cb = new IObjectMigratedCallback() {
			
			@Override
			public void migrationFinished() {
				sem.release();
			}
		};
		
		sem.acquire();
		cs.getHostPool().migrateObjectAsync(cco1.getId(), managingHost2, cb);
		sem.acquire();	// should be released by callback
		
		assertEquals(0, managingHost1.getCloudObjectsCount());
		assertEquals(2, managingHost2.getCloudObjectsCount());
		
		cco1 = cs.getClientCloudObject(co1);
		
		managingHost1 = cs.getHost(cco1.getId());;
		assertEquals(managingHost1.getId(), managingHost2.getId());
		
	}
	
	@Test
	public void testBasicMigrationViaScalingPolicy() throws InterruptedException {
		
		assertEquals(0, cs.countCloudObjects());
		
		AbstractScalingPolicy origPolicy = JCloudScaleConfiguration.getConfiguration().common().scalingPolicy();
		
		JCloudScaleConfiguration.getConfiguration().common().setScalingPolicy(new AbstractScalingPolicy() {
			
			private int counter = 0;
			
			@Override
			public IHost selectHost(ClientCloudObject newCloudObject, IHostPool hostPool) {
				
				switch(++counter) {
					case 1: return hostPool.startNewHost();
					case 2: return hostPool.startNewHost();
					case 3: return hostPool.startNewHost();
					case 4: return hostPool.startNewHost();
					default: return migrateAll(hostPool); 
				}
					
				
			}
			@Override
			public boolean scaleDown(IHost scaledHost, IHostPool hostPool) {
				return false;
			}
			
			private IHost migrateAll(IHostPool hostPool) {
				IHost host = hostPool.getHosts().iterator().next();
				List<UUID> toMigrate = new ArrayList<>();
				for(IHost h : hostPool.getHosts()) {
					for(ClientCloudObject cco : h.getCloudObjects()) {
						if(!h.getId().equals(host.getId())) {
							toMigrate.add(cco.getId());
						}
						
					}
				}
				for(UUID id : toMigrate) {
					((IMigrationEnabledHostPool)hostPool).migrateObject(id, host);
				}
				return host;
			}
			
		});
		
		SerializableCloudObject co1 = new SerializableCloudObject();
		ClientCloudObject cco1 = cs.getClientCloudObject(co1);
		SerializableCloudObject co2 = new SerializableCloudObject();
		ClientCloudObject cco2 = cs.getClientCloudObject(co2);
		SerializableCloudObject co3 = new SerializableCloudObject();
		ClientCloudObject cco3 = cs.getClientCloudObject(co3);
		SerializableCloudObject co4 = new SerializableCloudObject();
		ClientCloudObject cco4 = cs.getClientCloudObject(co4);
		
		
		
		assertEquals(4, cs.countCloudObjects());
		assertEquals(4, cs.countVirtualMachines());
		
		IHost managingHost1 = cs.getHost(cco1.getId());
		IHost managingHost2 = cs.getHost(cco2.getId());
		IHost managingHost3 = cs.getHost(cco3.getId());
		IHost managingHost4 = cs.getHost(cco4.getId());
		
		assertNotEquals(managingHost1.getId(), managingHost2.getId());
		assertNotEquals(managingHost1.getId(), managingHost3.getId());
		assertNotEquals(managingHost1.getId(), managingHost4.getId());
		
		SerializableCloudObject co5 = new SerializableCloudObject();
		ClientCloudObject cco5 = cs.getClientCloudObject(co5);
		
		JCloudScaleConfiguration.getConfiguration().common().setScalingPolicy(origPolicy);
		
		cco1 = cs.getClientCloudObject(co1);
		cco2 = cs.getClientCloudObject(co2);
		cco3 = cs.getClientCloudObject(co3);
		cco4 = cs.getClientCloudObject(co4);
		cco5 = cs.getClientCloudObject(co5);
		
		managingHost1 = cs.getHost(cco1.getId());
		managingHost2 = cs.getHost(cco2.getId());
		managingHost3 = cs.getHost(cco3.getId());
		managingHost4 = cs.getHost(cco4.getId());
		IHost managingHost5 = cs.getHost(cco5.getId());
		
		assertEquals(managingHost1.getId(), managingHost2.getId());
		assertEquals(managingHost1.getId(), managingHost3.getId());
		assertEquals(managingHost1.getId(), managingHost4.getId());
		assertEquals(managingHost1.getId(), managingHost5.getId());
		
	}
	
	@Test
	@Ignore
	// TODO - this is currently failing as the client cloud object changes
	// during migration. I consider this a bug that should be fixed
	public void testCloudObjectUUIDChanged() throws InterruptedException {
		
		assertEquals(0, cs.countCloudObjects());
		
		SerializableCloudObject co1 = new SerializableCloudObject();
		ClientCloudObject cco1 = cs.getClientCloudObject(co1);
		UUID origId = cco1.getId();
		SerializableCloudObject co2 = new SerializableCloudObject();
		ClientCloudObject cco2 = cs.getClientCloudObject(co2);
		
		assertEquals(2, cs.countCloudObjects());
		
		IHost managingHost1 = cs.getHost(cco1.getId());
		IHost managingHost2 = cs.getHost(cco2.getId());
		
		assertNotEquals(managingHost1.getId(), managingHost2.getId());
		cs.getHostPool().migrateObject(cco1.getId(), managingHost2);
		
		ClientCloudObject cco1_2 = cs.getClientCloudObject(co1);
		ClientCloudObject cco2_2 = cs.getClientCloudObject(co2);
		
		assertEquals(cco2, cco2_2);
		assertEquals(cco1, cco1_2);
		
		
		assertEquals(origId, cco1_2.getId());
		
	}
	
	@Test
	public void testNonSerializable() {
		
		assertEquals(0, cs.countCloudObjects());
		
		TestCloudObject1 co1 = new TestCloudObject1();
		ClientCloudObject cco1 = cs.getClientCloudObject(co1);
		SerializableCloudObject co2 = new SerializableCloudObject();
		ClientCloudObject cco2 = cs.getClientCloudObject(co2);
		
		assertEquals(2, cs.countCloudObjects());
		
		IHost managingHost1 = cs.getHost(cco1.getId());
		IHost managingHost2 = cs.getHost(cco2.getId());
		
		assertNotEquals(managingHost1.getId(), managingHost2.getId());
		try {
			cs.getHostPool().migrateObject(cco1.getId(), managingHost2);
		} catch(JCloudScaleException e) {
			e.printStackTrace();
			assertEquals(NotSerializableException.class, e.getCause().getClass());
			return;
		}
		fail("Expected JCloudScaleException");
		
	}
	
	@Test
	public void testMigrateRunningObject() throws InterruptedException {
		
		assertEquals(0, cs.countCloudObjects());
		
		final SerializableCloudObject co1 = new SerializableCloudObject();
		ClientCloudObject cco1 = cs.getClientCloudObject(co1);
		SerializableCloudObject co2 = new SerializableCloudObject();
		ClientCloudObject cco2 = cs.getClientCloudObject(co2);
		
		assertEquals(2, cs.countCloudObjects());
		
		IHost managingHost1 = cs.getHost(cco1.getId());
		IHost managingHost2 = cs.getHost(cco2.getId());
		
		assertNotEquals(managingHost1.getId(), managingHost2.getId());
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					co1.run(2500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}).start();
		
		Thread.sleep(500);
		
		// note that migrateObject(..) will block until the CO becomes
		// ready again
		cs.getHostPool().migrateObject(cco1.getId(), managingHost2);
		
		assertEquals(0, managingHost1.getCloudObjectsCount());
		assertEquals(2, managingHost2.getCloudObjectsCount());
		
		cco1 = cs.getClientCloudObject(co1);
		
		managingHost1 = cs.getHost(cco1.getId());;
		assertEquals(managingHost1.getId(), managingHost2.getId());
		
	}
	
	@Test
	@Ignore
	public void testMigrationState() throws InterruptedException, IOException {
		
		assertEquals(0, cs.countCloudObjects());
		
		SerializableCloudObject co1 = new SerializableCloudObject();
		final ClientCloudObject cco1 = cs.getClientCloudObject(co1);
		SerializableCloudObject co2 = new SerializableCloudObject();
		ClientCloudObject cco2 = cs.getClientCloudObject(co2);
		
		assertEquals(2, cs.countCloudObjects());
		
		IHost managingHost1 = cs.getHost(cco1.getId());
		final IHost managingHost2 = cs.getHost(cco2.getId());

		assertNotEquals(managingHost1.getId(), managingHost2.getId());
		
		// read in 6MB file and set it here
		co1.setTestField(readFile());
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				cs.getHostPool().migrateObject(cco1.getId(), managingHost2);
				System.out.println("Done migrating");
			}
		}).start();
		
		System.out.println("Continue executing");
		Thread.sleep(50);
		System.out.println("Done sleeping");
		ClientCloudObject cco1_1 = cs.getClientCloudObject(co1);
		assertEquals(CloudObjectState.MIGRATING, cco1_1.getState());
		System.out.println("State was MIGRATING");
		
		// note: if this test blocks forever, maybe we fail to
		// set this state back correctly?
		while(CloudObjectState.MIGRATING == cco1_1.getState()) {
			Thread.sleep(50);
			System.out.println("Checking  .... ");
		}
		
		assertEquals(CloudObjectState.IDLE, cco1_1.getState());
		assertEquals(0, managingHost1.getCloudObjectsCount());
		assertEquals(2, managingHost2.getCloudObjectsCount());
		
		managingHost1 = cs.getHost(cco1_1.getId());;
		assertEquals(managingHost1.getId(), managingHost2.getId());
		
		System.out.println("Done");
		
	}
	
	private String readFile() throws IOException {
		
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while((line  = reader.readLine()) != null)
			sb.append(line);
		reader.close();
		return sb.toString();
		
	}
	
}
