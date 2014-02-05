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

import static com.google.common.base.Objects.firstNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.springframework.util.ReflectionUtils.findMethod;
import static org.springframework.util.ReflectionUtils.invokeMethod;
import static org.springframework.util.ReflectionUtils.makeAccessible;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.junit.After;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown;
import at.ac.tuwien.infosys.jcloudscale.classLoader.AbstractClassLoaderConfiguration;
import at.ac.tuwien.infosys.jcloudscale.classLoader.SystemClassLoaderConfiguration;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.CachingClassLoaderConfiguration;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.cache.CacheType;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.fileCollectors.ClassBasedFileCollector;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.fileCollectors.FileCollectorAbstract;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.fileCollectors.StubFileCollector;
import at.ac.tuwien.infosys.jcloudscale.classLoader.simple.SimpleRemoteClassLoaderConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.test.util.ConfigurationHelper;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;

import com.google.common.io.Files;

/**
 * @author Gregor Schauer
 */
public class ClassLoaderTest {
	static final long TIMEOUT = 5000L;
	static final String RESOURCE = "META-INF"+File.separator+"openstack.properties";
	static final String CONTENT;

	static {
		try {
			CONTENT = Files.toString(new ClassPathResource(RESOURCE).getFile(), Charset.defaultCharset());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@After
	@JCloudScaleShutdown
	public void tearDown() throws Exception {
	}

	@Test(timeout = TIMEOUT)
	public void testSystemClassLoader() throws Exception {

		SystemClassLoaderConfiguration classLoaderConfiguration = new SystemClassLoaderConfiguration();
		startup(classLoaderConfiguration);

		ClassLoader classLoader = classLoaderConfiguration.createClassLoader();
		assertSame(ClassLoader.getSystemClassLoader().getClass(), classLoader.getClass());

		InputStream stream = classLoader.getResourceAsStream(RESOURCE);
		assertNotNull(stream);
		assertEquals(CONTENT, IOUtils.toString(stream));
	}

	@Test(timeout = TIMEOUT)
	public void testSimpleClassLoader() throws Exception {
		SimpleRemoteClassLoaderConfiguration classLoaderConfiguration = new SimpleRemoteClassLoaderConfiguration();
		classLoaderConfiguration.setLocalFirst(false);

		startup(classLoaderConfiguration);

		try(at.ac.tuwien.infosys.jcloudscale.classLoader.simple.RemoteClassLoader classLoader 
				= (at.ac.tuwien.infosys.jcloudscale.classLoader.simple.RemoteClassLoader) classLoaderConfiguration.createClassLoader())
		{
			assertSame(at.ac.tuwien.infosys.jcloudscale.classLoader.simple.RemoteClassLoader.class, classLoader.getClass());
	
			InputStream stream = getInputStream(classLoader);
			assertNotNull(stream);
			assertEquals(CONTENT, IOUtils.toString(stream));
		}
	}

	@Test(timeout = TIMEOUT)
	public void testCachingClassLoaderWithClassBasedFileCollector() throws Exception {
		assertEquals(CONTENT, useFileCollector(new ClassBasedFileCollector()));
	}

	@Test(timeout = TIMEOUT)
	public void testCachingClassLoaderWithCompleteFileCollector() throws Exception {
		assertEquals(CONTENT, useFileCollector(new ClassBasedFileCollector()));
	}

	@Test(timeout = TIMEOUT)
	public void testCachingClassLoaderWithFileBasedFileCollector() throws Exception {
		assertEquals(CONTENT, useFileCollector(new ClassBasedFileCollector()));
	}

	@Test(timeout = TIMEOUT)
	public void testCachingClassLoaderWithStubFileCollector() throws Exception {
		assertEquals("", useFileCollector(new StubFileCollector()));
	}

	String useFileCollector(FileCollectorAbstract fileCollector) throws Exception {
		CachingClassLoaderConfiguration classLoaderConfiguration = new CachingClassLoaderConfiguration();
		classLoaderConfiguration.setFileCollector(fileCollector);
		classLoaderConfiguration.setLocalFirst(false);
		classLoaderConfiguration.setCacheType(CacheType.NoCache, true);
		classLoaderConfiguration.getCacheConfiguration().setCacheFolder("target"+File.separator +"classLoaderCache");
		startup(classLoaderConfiguration);

		try(at.ac.tuwien.infosys.jcloudscale.classLoader.caching.RemoteClassLoader classLoader = (at.ac.tuwien.infosys.jcloudscale.classLoader.caching.RemoteClassLoader)classLoaderConfiguration.createClassLoader())
		{
			assertSame(at.ac.tuwien.infosys.jcloudscale.classLoader.caching.RemoteClassLoader.class, classLoader.getClass());

			InputStream stream = getInputStream(classLoader);
			return IOUtils.toString(firstNonNull(stream, new NullInputStream(0)));
		}
	}

	void startup(AbstractClassLoaderConfiguration classLoaderConfiguration) throws Exception {
		JCloudScaleConfiguration config = ConfigurationHelper.createDefaultTestConfiguration()
				.with(classLoaderConfiguration).build();

		JCloudScaleClient.setConfiguration(config);
		
		JCloudScaleClient.getClient();
	}

	InputStream getInputStream(ClassLoader classLoader) {
		Method method = findMethod(classLoader.getClass(), "getRemoteResourceAsStream", (Class<?>[])null);
		makeAccessible(method);
		return (InputStream) invokeMethod(method, classLoader, RESOURCE);
	}
}
