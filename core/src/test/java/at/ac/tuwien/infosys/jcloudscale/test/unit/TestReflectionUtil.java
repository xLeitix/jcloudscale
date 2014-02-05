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

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServerRunner;
import com.google.common.base.Function;
import com.google.common.primitives.Primitives;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;

import static at.ac.tuwien.infosys.jcloudscale.utility.ReflectionUtil.*;
import static com.google.common.collect.Collections2.transform;
import static java.lang.ClassLoader.getSystemClassLoader;
import static org.apache.commons.lang.ArrayUtils.*;
import static org.junit.Assert.*;

/**
 * @author Gregor Schauer
 */
public class TestReflectionUtil {
	@Test
	public void testFindDefaultConstructor() throws Exception {
		Constructor<Object> constructor = findConstructor(Object.class, null);
		assertEquals(Object.class.getDeclaredConstructor(), constructor);
	}

	@Test(expected = NoSuchMethodException.class)
	public void testFindNotExistingConstructor() throws Exception {
		findConstructor(Object.class, Object.class);
	}

	@Test
	public void testDetermineConstructor() throws ReflectiveOperationException {
		Constructor<Integer> constructor = findConstructor(Integer.class, new Class<?>[] {null});
		assertEquals(Integer.class.getDeclaredConstructor(String.class), constructor);
	}

	@Test
	public void testPrimitiveConstructor() throws ReflectiveOperationException {
		Constructor<Integer> constructor = findConstructor(Integer.class, Integer.TYPE);
		assertEquals(Integer.class.getDeclaredConstructor(int.class), constructor);
	}

	@Test(expected = NoSuchMethodException.class)
	public void testWrapperConstructor() throws Exception {
		findConstructor(Integer.class, Integer.class);
	}

	@Test(expected = JCloudScaleException.class)
	public void testVarArgsMethod() throws ClassNotFoundException {
		findMethod(JCloudScaleServerRunner.class, "main", String.class, String.class);
	}

	@Test
	public void testVarArgsArrayMethod() throws ClassNotFoundException {
		Method method = findMethod(JCloudScaleServerRunner.class, "main", String[].class);
		assertEquals(ReflectionUtils.findMethod(JCloudScaleServerRunner.class, "main", null), method);
	}

	@Test
	public void testMultipleConstructorCandidates() throws Exception {
		assertNotNull(resolveConstructor(String.class));
		assertNotNull(resolveConstructor(String.class, null));

		assertEquals(null, resolveConstructor(String.class, Object.class));
		assertEquals(null, resolveConstructor(String.class, Appendable.class));
		assertNotNull(resolveConstructor(String.class, String.class));
		assertNotNull(resolveConstructor(String.class, StringBuilder.class));
		assertNotNull(resolveConstructor(String.class, byte[].class, int.class));
	}

	@Test
	public void testAlternativeConstructors() throws Exception {
		assertNotNull(resolveConstructor(Instance.class, String.class));
		// resolveConstructor() cannot be used because getDeclaredConstructor() would not find Instance(Appendable)
		assertNotNull(findConstructor(Instance.class, Writer.class));
		assertNotNull(resolveConstructor(Instance.class, Serializable.class));
		assertNotNull(resolveConstructor(Instance.class, Object.class));
	}

	@Test
	public void testMultipleMethodCandidates() throws Exception {
		assertNotNull(resolveMethod(String.class, "getBytes"));
		assertNotNull(resolveMethod(String.class, "getBytes", null));
		assertEquals(null, resolveMethod(String.class, "getBytes", Object.class));
		assertNotNull(resolveMethod(String.class, "getBytes", String.class));
		assertNotNull(resolveMethod(String.class, "getBytes", Charset.class));
	}

	<T> Constructor<T> resolveConstructor(Class<T> type, Class<?>... paramTypes) {
		Constructor<T> expected = null;
		try {
			expected = type.getDeclaredConstructor(paramTypes);
		} catch (NoSuchMethodException e) {
			// Ignore
		}
		Constructor<T> actual = null;
		try {
			actual = findConstructor(type, paramTypes);
		} catch (Exception e) {
			// Ignore
		}
		assertEquals(expected, actual);
		return actual;
	}
	
	Method resolveMethod(Class<?> type, String name, Class<?>... paramTypes) {
		Method expected = ReflectionUtils.findMethod(type, name, paramTypes);
		Method actual = null;
		try {
			actual = findMethod(type, name, paramTypes);
		} catch (Exception e) {
			// Ignore
		}
		assertEquals(expected, actual);
		if (actual != null) {
			assertEquals(expected.getDeclaringClass(), actual.getDeclaringClass());
		}
		return actual;
	}

	@Test
	public void testGetClassesFromNames() throws ClassNotFoundException {
		Set<Class<?>> types = new HashSet<>(Primitives.allPrimitiveTypes());
		types.addAll(Primitives.allWrapperTypes());
		Collection<String> typeNames = transform(types, new Function<Class<?>, String>() {
			@Override
			public String apply(Class<?> input) {
				return input.getName();
			}
		});

		List<Class<?>> classes = Arrays.asList(getClassesFromNames(typeNames.toArray(new String[typeNames.size()]), getSystemClassLoader()));
		assertEquals(types.size(), classes.size());
		assertEquals(true, classes.containsAll(Primitives.allPrimitiveTypes()));
		assertEquals(true, classes.containsAll(Primitives.allWrapperTypes()));
	}

	@Test
	public void testGetClassesFromEmptyNames() throws Exception {
		assertArrayEquals(EMPTY_CLASS_ARRAY, getClassesFromNames(null, null));
		assertArrayEquals(EMPTY_CLASS_ARRAY, getClassesFromNames(new String[0], null));
		assertArrayEquals(new Class[] {null}, getClassesFromNames(new String[] {null}, null));
		assertArrayEquals(new Class[] {null, null}, getClassesFromNames(new String[] {null, null}, null));
	}

	@Test
	public void testGetNamesFromClasses() throws ClassNotFoundException {
		Set<Class<?>> types = new HashSet<>(Primitives.allPrimitiveTypes());
		types.addAll(Primitives.allWrapperTypes());
		Collection<String> typeNames = transform(types, new Function<Class<?>, String>() {
			@Override
			public String apply(Class<?> input) {
				return input.getName();
			}
		});

		List<String> names = Arrays.asList(getNamesFromClasses(types.toArray(new Class<?>[types.size()])));
		assertEquals(types.size(), names.size());
		assertEquals(true, typeNames.containsAll(names));
	}

	@Test
	public void testGetNamesFromEmptyClasses() throws Exception {
		assertArrayEquals(EMPTY_STRING_ARRAY, getNamesFromClasses(null));
		assertArrayEquals(EMPTY_STRING_ARRAY, getNamesFromClasses(new Class[0]));
		assertArrayEquals(new Class[] {null}, getNamesFromClasses(new Class[] {null}));
		assertArrayEquals(new Class[] {null, null}, getNamesFromClasses(new Class[] {null, null}));
	}
	
	@Test
	public void testGetClassesFromObjects() throws ReflectiveOperationException {
		Class<?>[] classes = getClassesFromObjects(false, 0, 0D, BeanUtils.instantiateClass(Void.class));
		assertArrayEquals(new Class<?>[]{Boolean.class, Integer.class, Double.class, Void.class}, classes);
	}
	
	@Test
	public void testGetClassesFromEmptyObjects() {
		assertArrayEquals(EMPTY_CLASS_ARRAY, getClassesFromObjects());
		assertArrayEquals(EMPTY_CLASS_ARRAY, getClassesFromObjects(null));
		assertArrayEquals(EMPTY_CLASS_ARRAY, getClassesFromObjects(EMPTY_OBJECT_ARRAY));
		assertArrayEquals(new Class<?>[] {null}, getClassesFromObjects((String) null));
		assertArrayEquals(new Class<?>[] {null, null}, getClassesFromObjects(null, null));
	}

	@Test
	public void testOverride() throws Exception {
		SubType obj = new SubType();

		// resolveMethod() cannot be used because ReflectionUtils.findMethod() would just find overridden(Serializable)
		Method method = findMethod(obj.getClass(), "overridden", String.class);
		assertNotNull(method);
		assertSame(SubType.class, method.getDeclaringClass());

		method = resolveMethod(obj.getClass(), "overridden", Serializable.class);
		assertNotNull(method);
		assertSame(SubType.class, method.getDeclaringClass());
	}

	@Test
	public void testCovariance() throws Exception {
		SubType obj = new SubType();

		Method method = resolveMethod(obj.getClass(), "covariant", String.class);
		assertNotNull(method);
		assertSame(SuperType.class, method.getDeclaringClass());

		method = resolveMethod(obj.getClass(), "covariant", Object.class);
		assertNotNull(method);
		assertSame(SubType.class, method.getDeclaringClass());
	}

	@Test
	public void testContravariance() throws Exception {
		SubType obj = new SubType();

		Method method = resolveMethod(obj.getClass(), "contravariant", Integer.class);
		assertNotNull(method);
		assertSame(SubType.class, method.getDeclaringClass());

		method = resolveMethod(obj.getClass(), "contravariant", Number.class);
		assertNotNull(method);
		assertSame(SuperType.class, method.getDeclaringClass());
	}

	@Test
	public void testInvariance() throws Exception {
		SubType obj = new SubType();

		Method method = resolveMethod(obj.getClass(), "invariant", Object.class, String.class);
		assertNotNull(method);
		assertSame(SuperType.class, method.getDeclaringClass());

		method = resolveMethod(obj.getClass(), "invariant", String.class, Object.class);
		assertNotNull(method);
		assertSame(SubType.class, method.getDeclaringClass());

		assertEquals(null, resolveMethod(obj.getClass(), "invariant", Object.class, Object.class));
	}
	
	static class Instance {
		Instance(String obj) {
		}

		Instance(Appendable obj) {
		}

		Instance(Serializable obj) {
		}

		Instance(Object obj) {
		}
	}

	static class SuperType {
		void overridden(Serializable obj) {
		}

		void covariant(String obj) {
		}

		void contravariant(Number n) {
		}

		void invariant(Object a, String b) {
		}
	}

	static class SubType extends SuperType {
		@Override
		void overridden(Serializable obj) {
		}

		void covariant(Object obj) {
		}

		void contravariant(Integer n) {
		}

		void invariant(String a, Object b) {
		}
	}
}
