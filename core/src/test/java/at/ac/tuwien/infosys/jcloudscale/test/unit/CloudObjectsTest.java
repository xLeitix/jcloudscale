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

import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown;
import at.ac.tuwien.infosys.jcloudscale.api.CloudObjects;
import at.ac.tuwien.infosys.jcloudscale.classLoader.AbstractClassLoaderConfiguration;
import at.ac.tuwien.infosys.jcloudscale.classLoader.simple.SimpleRemoteClassLoaderConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
import at.ac.tuwien.infosys.jcloudscale.utility.ReflectionUtil;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;

import com.google.common.base.Objects;
 
/**
 * @author Gregor Schauer
 */
public class CloudObjectsTest {
	
	@Before
	public void startup() throws Exception {
		JCloudScaleConfiguration config = createConfig(new SimpleRemoteClassLoaderConfiguration());
		JCloudScaleClient.setConfiguration(config);
		JCloudScaleClient.getClient();
	}

	@After
	@JCloudScaleShutdown
	public void tearDown() throws Exception {
	}

	@Test
	public void testLifecycle() throws Exception {
		
		MyRemoteObject remote = CloudObjects.create(MyRemoteObject.class, 1);
		UUID id = CloudObjects.getId(remote);
		assertNotNull(id);
		assertEquals(true, CloudObjects.isCloudObject(id));

		assertEquals(2, remote.add(1).intValue());
		Method add = ReflectionUtil.findMethod(remote.getClass(), "add", Integer.class);
		assertEquals(2, CloudObjects.invoke(remote, add, 1));

		CloudObjects.destroy(remote);
		assertEquals(false, CloudObjects.isCloudObject(id));
	}

	@Test
	public void testNull() throws Exception {
		// TODO: next line causes an IllegalArgumentException because the semantics of CgLibUtil is different from the one used along with Javassist
		// If parameters[0] == null in CgLibUtil.replaceCOWithProxy(), a wrong constructor is used 
		MyRemoteObject remote = CloudObjects.create(MyRemoteObject.class, new Object[]{null});
		assertEquals(1, remote.add(1).intValue());
		Method add = ReflectionUtil.findMethod(remote.getClass(), "add", new Class<?>[]{null});
		assertEquals(0, CloudObjects.invoke(remote, add, new Object[]{null}));
	}

	static JCloudScaleConfiguration createConfig(AbstractClassLoaderConfiguration classLoaderConfiguration)
	{
		return ConfigurationHelper.createDefaultTestConfiguration()
				.with(classLoaderConfiguration)
				.build();
	}

	static class MyRemoteObject {
		Integer number;

		MyRemoteObject(Integer number) {
			this.number = number;
		}

		Integer add(Integer n) {
			return Objects.firstNonNull(number, 0) + Objects.firstNonNull(n, 0);
		}
	}
}
