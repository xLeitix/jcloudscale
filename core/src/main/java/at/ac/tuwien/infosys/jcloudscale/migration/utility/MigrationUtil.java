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
package at.ac.tuwien.infosys.jcloudscale.migration.utility;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.ac.tuwien.infosys.jcloudscale.CloudInvocationInfos;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObjectId;
import at.ac.tuwien.infosys.jcloudscale.annotations.DestructCloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.EventSink;
import at.ac.tuwien.infosys.jcloudscale.exception.IllegalDefinitionException;
import at.ac.tuwien.infosys.jcloudscale.migration.annotations.MigrationTransient;
import at.ac.tuwien.infosys.jcloudscale.migration.annotations.NoMigration;
import at.ac.tuwien.infosys.jcloudscale.migration.annotations.PostMigration;
import at.ac.tuwien.infosys.jcloudscale.migration.annotations.PreMigration;
import at.ac.tuwien.infosys.jcloudscale.migration.annotations.SerializationProvider;
import at.ac.tuwien.infosys.jcloudscale.migration.serialization.DefaultSerializationProvider;
import at.ac.tuwien.infosys.jcloudscale.migration.serialization.ISerializationProvider;

/**
 * Provides helper methods for working with {@link CloudObject}'s annotations
 * and for migrating <code>CloudObjects</code>.
 */
public class MigrationUtil {

	// =========================================================================
	// Cache
	// =========================================================================

	// Hashtable: synchronized + non-null
	private static Map<Class<?>, MigrationUtilCacheDto> cacheMap =
			new Hashtable<Class<?>, MigrationUtilCacheDto>();

	private static synchronized MigrationUtilCacheDto getCache(Class<?> clazz) {
		MigrationUtilCacheDto dto = cacheMap.get(clazz);
		if (dto == null) {
			dto = new MigrationUtilCacheDto();
			cacheMap.put(clazz, dto);
		}
		return dto;
	}

	public static void resetCache() {
		cacheMap.clear();
	}

	// =========================================================================
	// Public Methods
	// =========================================================================

	/**
	 * Checks if all migration-specific annotations are used correctly (if any).
	 * 
	 * @param clazz
	 *            The cloud object type to check
	 * @throws IllegalDefinitionException
	 *             If the given class doesn't comply to migration requirements
	 * @throws NullPointerException
	 *             If the given class is <code>null</code>
	 */
	public static void checkLegalMigrationDef(Class<?> clazz) throws IllegalDefinitionException {
		MigrationUtilCacheDto cache = getCache(clazz);

		if (cache.isLegal != null) {
			// Cache hit
			if (!cache.isLegal)
				throw cache.legalError;
		} else {
			// Cache miss
			try {
				// Check @PreMigrate and @PostMigrate methods
				for (Method m : clazz.getDeclaredMethods()) {
					if (m.isAnnotationPresent(PreMigration.class)
							|| m.isAnnotationPresent(PostMigration.class)) {
						if (m.isAnnotationPresent(DestructCloudObject.class))
							throw new IllegalDefinitionException(
									"Illegal method "
											+ m.getName()
											+ " annotated with @DestructCloudObject."
											+ "You may not annotate @PreMigration/@PostMigration with @DestructCloudObject");
						if (m.getParameterTypes().length > 0)
							throw new IllegalDefinitionException(
									"Illegal method "
											+ m.getName()
											+ " annotated with @PreMigration/@PostMigration. "
											+ "You may only annotate methods without parameters with these annotations.");
						if (Modifier.isStatic(m.getModifiers()))
							throw new IllegalDefinitionException(
									"Illegal method "
											+ m.getName()
											+ " annotated with @PreMigration/@PostMigration. "
											+ "You may only annotate non-static methods with these annotations.");
					}
				}

				// Check @MigrationTransient fields
				for (Field f : clazz.getDeclaredFields()) {
					if (f.isAnnotationPresent(MigrationTransient.class)) {
						if (Modifier.isStatic(f.getModifiers()))
							throw new IllegalDefinitionException(
									"Illegal field "
											+ f.getName()
											+ " annotated with @MigrationTransient. "
											+ "You may only annotate non-static fields with this annotation.");

						Class<?> initClazz = f.getAnnotation(MigrationTransient.class).initialize();
						if (initClazz != Class.class && f.getType().isPrimitive())
							throw new IllegalDefinitionException(
									"Illegal field "
											+ f.getName()
											+ " annotated with @MigrationTransient. "
											+ "You may not specify an initialization class for primitive type fields.");
						if (initClazz != Class.class && !f.getType().isAssignableFrom(initClazz))
							throw new IllegalDefinitionException(
									"Illegal field "
											+ f.getName()
											+ " annotated with @MigrationTransient. "
											+ "Specified initialization class not assignable to field.");
					}
				}

				// Recursively Check superclasses (if any)
				if (clazz.getSuperclass() != null) {
					checkLegalMigrationDef(clazz.getSuperclass());
				}
			} catch (IllegalDefinitionException ide) {
				cache.isLegal = false;
				cache.legalError = ide;
				throw ide;
			}
			cache.isLegal = true;
		}
	}

	/**
	 * Checks if the given class is marked to be not considered in migration
	 * decisions.
	 * 
	 * @param clazz
	 *            The class to check
	 * @return <code>true</code> or <code>false</code>
	 * @throws NullPointerException
	 *             If the given class is <code>null</code>
	 */
	public static boolean isNoMigrate(Class<?> clazz) {
		MigrationUtilCacheDto cache = getCache(clazz);
		if (cache.isNoMigrate == null) {
			// Cache miss
			cache.isNoMigrate = clazz.isAnnotationPresent(NoMigration.class);
		}
		// Cache hit
		return cache.isNoMigrate;
	}

	/**
	 * Returns the {@link ISerializationProvider} implementation class object
	 * specified for the given class using the {@link SerializationProvider}
	 * annotation.
	 * 
	 * @param clazz
	 *            The class to scan for.
	 * @return The implementation class object specified or
	 *         {@link DefaultSerializationProvider} if the annotation is
	 *         missing.
	 */
	public static ISerializationProvider getSerializationProvider(Class<?> clazz)
			throws InstantiationException, InvocationTargetException, IllegalAccessException,
			NoSuchMethodException {
		MigrationUtilCacheDto cache = getCache(clazz);

		if (cache.serializationProvider == null) {
			// Cache miss
			SerializationProvider ann = clazz.getAnnotation(SerializationProvider.class);
			if (ann == null)
				cache.serializationProvider = DefaultSerializationProvider.class;
			else
				cache.serializationProvider = ann.value();
		}

		// Instantiate the serialization provider
		try {
			return cache.serializationProvider.getConstructor(Class.class).newInstance(clazz);
		} catch (Exception e) {
		}
		return cache.serializationProvider.getConstructor().newInstance();
	}

	/**
	 * Scans the given class for methods annotated with {@link PreMigration} and
	 * orders them by their priority
	 * 
	 * @param clazz
	 *            The class to scan
	 * @return A sorted set with methods or an empty set if the class has no
	 *         annotated methods
	 */
	// Can't use a SortedSet because methods with the same priority are seen as
	// equal and thus can't be added to the set
	public static List<Method> getSortedPreMigrate(Class<?> clazz) {
		MigrationUtilCacheDto cache = getCache(clazz);
		if (cache.sortedPreMigrate == null) {
			List<Method> tmp = new ArrayList<Method>();

			for (Method method : clazz.getDeclaredMethods()) {
				if (method.isAnnotationPresent(PreMigration.class)) {
					tmp.add(method);
				}
			}

			Collections.sort(tmp, new Comparator<Method>() {
				@Override
				public int compare(Method m1, Method m2) {
					int m1Priority = m1.getAnnotation(PreMigration.class).priority();
					int m2Priority = m2.getAnnotation(PreMigration.class).priority();
					return Integer.compare(m1Priority, m2Priority);
				}
			});

			cache.sortedPreMigrate = Collections.unmodifiableList(tmp);
		}

		return cache.sortedPreMigrate;
	}

	/**
	 * Scans the given class for methods annotated with {@link PostMigration}
	 * and orders them by their priority
	 * 
	 * @param clazz
	 *            The class to scan
	 * @return A sorted set with methods or an empty set if the class has no
	 *         annotated methods
	 */
	// Can't use a SortedSet because methods with the same priority are seen as
	// equal and thus can't be added to the set
	public static List<Method> getSortedPostMigrate(Class<?> clazz) {
		MigrationUtilCacheDto cache = getCache(clazz);
		if (cache.sortedPreMigrate == null) {
			List<Method> tmp = new ArrayList<Method>();

			for (Method method : clazz.getDeclaredMethods()) {
				if (method.isAnnotationPresent(PostMigration.class)) {
					tmp.add(method);
				}
			}

			Collections.sort(tmp, new Comparator<Method>() {
				@Override
				public int compare(Method m1, Method m2) {
					int m1Priority = m1.getAnnotation(PostMigration.class).priority();
					int m2Priority = m2.getAnnotation(PostMigration.class).priority();
					return Integer.compare(m1Priority, m2Priority);
				}
			});

			cache.sortedPreMigrate = Collections.unmodifiableList(tmp);
		}

		return cache.sortedPreMigrate;
	}

	/**
	 * Scans the given class for fields annotated with
	 * {@link MigrationTransient}
	 * 
	 * @param clazz
	 *            The class to scan
	 * @return A set of fields
	 */
	public static Set<Field> getTransientFields(Class<?> clazz) {
		MigrationUtilCacheDto cache = getCache(clazz);
		if (cache.transientFields == null) {
			Set<Field> tmp = new HashSet<Field>();

			for (Field field : clazz.getDeclaredFields()) {
				if (!Modifier.isStatic(field.getModifiers())
						&& field.isAnnotationPresent(MigrationTransient.class)) {
					tmp.add(field);
				}
			}

			cache.transientFields = Collections.unmodifiableSet(tmp);
		}

		return cache.transientFields;
	}

	/**
	 * Scans the class for fields NOT annotated with {@link MigrationTransient}
	 * 
	 * @param clazz
	 *            The class to scan
	 * @return A set of fields
	 */
	public static Set<Field> getNonTransientFields(Class<?> clazz) {
		MigrationUtilCacheDto cache = getCache(clazz);
		if (cache.nonTransientFields == null) {
			Set<Field> tmp = new HashSet<Field>();

			for (Field field : clazz.getDeclaredFields()) {
				if (!Modifier.isStatic(field.getModifiers())
						&& !field.isAnnotationPresent(MigrationTransient.class)) {
					tmp.add(field);
				}
			}

			cache.nonTransientFields = Collections.unmodifiableSet(tmp);
		}

		return cache.nonTransientFields;
	}

	/**
	 * Scans the class for fields annotated with JCloudScale-specific annotations
	 * 
	 * @param clazz
	 *            The class to scan
	 * @return A set of fields
	 * 
	 * @see CloudObjectId
	 * @see CloudInvocationInfos
	 * @see EventSink
	 */
	public static Set<Field> getJCloudScaleFields(Class<?> clazz) {
		MigrationUtilCacheDto cache = getCache(clazz);
		if (cache.cloudScaleFields == null) {
			Set<Field> tmp = new HashSet<Field>();

			for (Field field : clazz.getDeclaredFields()) {
				if (!Modifier.isStatic(field.getModifiers())
						&& (field.isAnnotationPresent(CloudObjectId.class) ||
								field.isAnnotationPresent(CloudInvocationInfos.class) ||
						field.isAnnotationPresent(EventSink.class))) {
					tmp.add(field);
				}
			}

			cache.cloudScaleFields = Collections.unmodifiableSet(tmp);
		}

		return Collections.unmodifiableSet(cache.cloudScaleFields);
	}

	/**
	 * Resets all given complex-type fields to <code>null</code>, so that the
	 * object can be serialized
	 * 
	 * @param cloudObject
	 *            The cloudObject which fields should be reseted
	 * @param fields
	 *            The fields to reset
	 * @throws Exception
	 *             If an exception occurs
	 */
	public static void resetFields(Object cloudObject, Set<Field> fields) throws Exception {
		for (Field f : fields) {
			f.setAccessible(true);
			if (!f.getType().isPrimitive())
				f.set(cloudObject, null);
			else if (f.getType() == double.class)
				f.setDouble(cloudObject, 0d);
			else if (f.getType() == float.class)
				f.setFloat(cloudObject, 0f);
			else if (f.getType() == long.class)
				f.setLong(cloudObject, 0l);
			else if (f.getType() == int.class)
				f.setInt(cloudObject, 0);
			else if (f.getType() == short.class)
				f.setShort(cloudObject, (short) 0);
			else if (f.getType() == byte.class)
				f.setByte(cloudObject, (byte) 0);
			else if (f.getType() == char.class)
				f.setChar(cloudObject, (char) 0);
			else if (f.getType() == boolean.class)
				f.setBoolean(cloudObject, false);
		}
	}

	/**
	 * Initializes all complex type fields of an {@link CloudObject} annotated
	 * with {@link MigrationTransient}. Primitive fields are set to their
	 * declared initialization value. Complex type fields are initialized with a
	 * new instance of the class specified in
	 * {@link MigrationTransient#initialize()} or their declared initialization
	 * value if there's nothing specified
	 * 
	 * @param cloudObject
	 *            The <code>CloudObject</code> to initialize.
	 * @param fields
	 *            The fields to initialize.
	 * @throws IllegalAccessException
	 *             If the fields which should be initialized couldn't be
	 *             accessed
	 * @throws InstantiationException
	 *             If a complex field couldn't be initialized
	 * @throws NullPointerException
	 *             If a given field isn't annotated with
	 *             {@link MigrationTransient}
	 */
	public static void initTransientFields(Object cloudObject, Set<Field> fields)
			throws IllegalAccessException, InstantiationException, NullPointerException {
		Object templateCloudObject = cloudObject.getClass().newInstance();
		for (Field f : fields) {
			f.setAccessible(true);
			if (f.getType().isPrimitive()) {
				f.set(cloudObject, f.get(templateCloudObject));
			} else {
				Class<?> initType = f.getAnnotation(MigrationTransient.class).initialize();
				if (initType != Class.class)
					f.set(cloudObject, initType.newInstance());
				else
					f.set(cloudObject, f.getType().newInstance());
			}
		}
	}
}
