/*
   Copyright 2014 Philipp Leitner 

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
package at.ac.tuwien.infosys.jcloudscale.test.integration.local;

import static com.google.common.collect.Lists.newArrayList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown;
import at.ac.tuwien.infosys.jcloudscale.api.CloudObjects;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.TestCloudObject1;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.policy.CalculatingRunnable;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.policy.WaitingRunnable;
import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;
import at.ac.tuwien.infosys.jcloudscale.vm.localVm.LocalCloudPlatformConfiguration;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 * @author Gregor Schauer
 */
public class TestScalingPolicy {
	static final int DEFAULT_TIMEOUT = 10000;

	public JCloudScaleClient getClient(AbstractScalingPolicy scalingPolicy) {
		JCloudScaleConfiguration config = ConfigurationHelper.createDefaultTestConfiguration()
												.with(scalingPolicy)
												.withMonitoring(false)
												.build();
		((LocalCloudPlatformConfiguration)config.server().cloudPlatform()).setJavaHeapSizeMB(8);
		config.common().setScaleDownIntervalInSec(1);
		
		JCloudScaleClient.setConfiguration(config);
		return JCloudScaleClient.getClient();
	}

	@After
	@JCloudScaleShutdown
	public void after() throws Exception {
	}

	@Test
	public void testSingleHost() throws Exception {
		getClient(new AbstractScalingPolicy() {
			@Override
			public IHost selectHost(ClientCloudObject newCloudObject, IHostPool hostPool) 
			{
				if(hostPool.getHostsCount() > 0)
					return hostPool.getHosts().iterator().next();
				else 
					return hostPool.startNewHost();
			}

			@Override
			public boolean scaleDown(IHost scaledHost, IHostPool hostPool) {
				return scaledHost.getCloudObjectsCount() == 0;
			}
		});

		assertInstances(0, 0, DEFAULT_TIMEOUT);

		WaitingRunnable first = new WaitingRunnable();
		first.start();
		assertInstances(1, 1, DEFAULT_TIMEOUT);

		WaitingRunnable second = new WaitingRunnable();
		second.start();
		assertInstances(1, 2, DEFAULT_TIMEOUT);

		first.close();
		second.close();
		assertInstances(0, 0, DEFAULT_TIMEOUT);
	}

	@Test
	public void testCategorizedHosts() throws Exception {
		getClient(new AbstractScalingPolicy() {
			final Map<UUID, Class<?>> categories = Maps.newHashMap();

			@Override
			public IHost selectHost(ClientCloudObject newCloudObject, IHostPool hostPool) {
				synchronized (categories) {
					for (IHost host : hostPool.getHosts()) {
						if (categories.get(host.getId()) == newCloudObject.getCloudObjectClass()) {
							return host;
						}
					}
					IHost host = hostPool.startNewHost();
					categories.put(host.getId(), newCloudObject.getCloudObjectClass());
					return host;
				}
			}

			@Override
			public boolean scaleDown(IHost scaledHost, IHostPool hostPool) {
				return scaledHost.getCloudObjectsCount() == 0;
			}
		});

		assertInstances(0, 0, DEFAULT_TIMEOUT);

		List<Closeable> closeables = newArrayList();
		int i = 1;
		for (; i < 10; i++) {
			closeables.add(new WaitingRunnable());
			assertInstances(1, i, DEFAULT_TIMEOUT);
		}

		for (;i < 13; i++) {
			closeables.add(CloudObjects.create(CalculatingRunnable.class));
			assertInstances(2, i, DEFAULT_TIMEOUT);
		}

		for (Closeable c : Iterables.filter(closeables, Predicates.instanceOf(WaitingRunnable.class))) {
			c.close();
		}
		assertInstances(1, 3, DEFAULT_TIMEOUT);
		
		for (Closeable c : Iterables.filter(closeables, Predicates.instanceOf(CalculatingRunnable.class))) {
			CloudObjects.destroy(c);
		}
		assertInstances(0, 0, DEFAULT_TIMEOUT);
	}

	@Test
	public void testSync() throws Exception {
		AtomicBoolean async = new AtomicBoolean(false);
		AtomicInteger reserve = new AtomicInteger();
		getClient(new AsyncScalingPolicy(async, reserve));

		CalculatingRunnable worker = CloudObjects.create(CalculatingRunnable.class);
		assertInstances(1, 1, -1);

		CloudObjects.destroy(worker);
		assertInstances(0, 0, DEFAULT_TIMEOUT);
	}

	@Test
	public void testAsync() throws Exception {
		AtomicBoolean async = new AtomicBoolean(true);
		AtomicInteger reserve = new AtomicInteger();
		getClient(new AsyncScalingPolicy(async, reserve));

		CalculatingRunnable worker = CloudObjects.create(CalculatingRunnable.class);
		// Note that deploying an object on a host that is started asynchronously blocks until the host is started
		assertInstances(1, 1, -1);

		CloudObjects.destroy(worker);
		assertInstances(0, 0, DEFAULT_TIMEOUT);
	}
	
	@Test
	public void testSingleHostScalingPolicy() throws Exception {
		
		getClient(new SingleHostScalingPolicy(false));
		
		List<TestCloudObject1> objs = new ArrayList<>();
		objs.add(new TestCloudObject1());
		objs.add(new TestCloudObject1());
		objs.add(new TestCloudObject1());
		objs.add(new TestCloudObject1());
		
		for(TestCloudObject1 obj : objs)
			obj.killMeSoftly();
		
	}
	
	@Test
	public void testSingleHostScalingPolicyAsync() throws Exception {
		
		getClient(new SingleHostScalingPolicy(true));
		
		List<TestCloudObject1> objs = new ArrayList<>();
		objs.add(new TestCloudObject1());
		objs.add(new TestCloudObject1());
		objs.add(new TestCloudObject1());
		objs.add(new TestCloudObject1());
		
		for(TestCloudObject1 obj : objs)
			obj.killMeSoftly();
		
	}
	
	@Test
	public void testSingleHostScalingPolicyParallel() throws Exception {
		
		getClient(new SingleHostScalingPolicy(false));
		
		final List<TestCloudObject1> objs = new CopyOnWriteArrayList<>();
		ExecutorService threadPool = Executors.newCachedThreadPool();
		try
		{
			for(int i=0;i<5;++i)
				threadPool.execute(new Runnable() {
					@Override
					public void run() 
					{
						objs.add(new TestCloudObject1());
					}
				});
		}
		finally
		{
			threadPool.shutdown();
			if(!threadPool.awaitTermination(10, TimeUnit.MINUTES))
				throw new Exception("Failed to create objects in time!");
		}
		
		for(TestCloudObject1 obj : objs)
			obj.killMeSoftly();
		
	}
	
	@Test
	public void testSingleHostScalingPolicyParallelAsync() throws Exception {
		
		getClient(new SingleHostScalingPolicy(true));
		
		final List<TestCloudObject1> objs = new CopyOnWriteArrayList<>();
		ExecutorService threadPool = Executors.newCachedThreadPool();
		try
		{
			for(int i=0;i<5;++i)
				threadPool.execute(new Runnable() {
					@Override
					public void run() 
					{
						objs.add(new TestCloudObject1());
					}
				});
		}
		finally
		{
			threadPool.shutdown();
			if(!threadPool.awaitTermination(10, TimeUnit.MINUTES))
				throw new Exception("Failed to create objects in time!");
		}
		
		for(TestCloudObject1 obj : objs)
			obj.killMeSoftly();
		
	}

	private void assertInstances(int hosts, int cloudObjects, int timeout) throws InterruptedException {
		CloudManager cloudManager = CloudManager.getInstance();
		assertNotNull(cloudManager);

		for (long now = System.currentTimeMillis(); timeout > 0 && System.currentTimeMillis() - now < timeout
				&& cloudObjects != cloudManager.getCloudObjects().size(); )
			;
		assertEquals(cloudObjects, cloudManager.getCloudObjects().size());
		assertEquals(cloudObjects, cloudManager.getHostPool().countCloudObjects());
		assertEquals(cloudObjects, cloudManager.getHostPool().getCloudObjects().size());

		for (long now = System.currentTimeMillis(); System.currentTimeMillis() - now < timeout
				&& hosts != cloudManager.getHosts().size(); )
			;
		assertEquals(hosts, cloudManager.getHosts().size());
		assertEquals(hosts, cloudManager.getHostPool().getHostsCount());
		assertEquals(hosts, Iterables.size(cloudManager.getHostPool().getHosts()));
	}

	private static class SingleHostScalingPolicy extends AbstractScalingPolicy
	{
		private volatile IHost host = null;
		private boolean startAsync = false;
		private Object lock = new Object();
		
		public SingleHostScalingPolicy(boolean startAsync)
		{
			this.startAsync = startAsync;
		}
		
		@Override
		public IHost selectHost(ClientCloudObject newCloudObject, IHostPool hostPool) 
		{
			if(host == null)
			{
				synchronized (lock) 
				{
					if(host == null)
						host = startAsync ? hostPool.startNewHostAsync() : hostPool.startNewHost();
				}
			}
			
			if(hostPool.getHostsCount() != 1 || !hostPool.getHosts().iterator().next().equals(host))
				throw new RuntimeException("THERE'S SOMETHING WRONG WITH SCALING POLICY!");
			
			return host;
		}

		@Override
		public boolean scaleDown(IHost scaledHost, IHostPool hostPool) {
			return false;
		}
		
	}
	private static class AsyncScalingPolicy extends AbstractScalingPolicy {
		private final AtomicBoolean async;
		private AtomicInteger reserve;

		public AsyncScalingPolicy(AtomicBoolean async, AtomicInteger reserve) {
			this.async = async;
			this.reserve = reserve;
		}

		@Override
		public IHost selectHost(ClientCloudObject newCloudObject, IHostPool hostPool) {
			IHost host = null;
			for (int i = 0; i <= reserve.get(); i++) {
				host = async.get() ? hostPool.startNewHostAsync() : hostPool.startNewHost();
			}
			return host;
		}

		@Override
		public boolean scaleDown(IHost scaledHost, IHostPool hostPool) {
			 return reserve.get() == 0 && scaledHost.getCloudObjectsCount() == 0;
		}
	}
}
