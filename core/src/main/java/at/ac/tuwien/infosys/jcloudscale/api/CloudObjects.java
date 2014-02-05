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
package at.ac.tuwien.infosys.jcloudscale.api;

import static com.google.common.base.Objects.firstNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.lang.ArrayUtils;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.management.JCloudScaleReferenceManager;
import at.ac.tuwien.infosys.jcloudscale.utility.CgLibUtil;
import at.ac.tuwien.infosys.jcloudscale.utility.ReferenceHashmap;
import at.ac.tuwien.infosys.jcloudscale.utility.ReflectionUtil;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;

/**
 * Contains methods for creating, accessing and destroying remote CloudObjects.
 * 
 * @author Gregor Schauer
 */
public abstract class CloudObjects {
	public static final Constructor<Object> DEFAULT_CONSTRUCTOR
			= ConstructorUtils.getAccessibleConstructor(Object.class, ArrayUtils.EMPTY_CLASS_ARRAY);
	 
	static Map<Object, UUID> idMaps = new ReferenceHashmap<>();
	// this contains all IDs we ever had and destroyed - TODO: we should clean this up somehow
	static Set<UUID> destroyedIds = new HashSet<>();

	private CloudObjects() {
	}

	/**
	 * Creates a remote CloudObjects of the desired type using the given parameters for the remote constructor
	 * invocation.
	 * <p/>
	 * <b>Note that the created proxy does not redirect method invocations to the corresponding CloudObject.</b><br/>
	 * Instead {@link #invoke(Object, java.lang.reflect.Method, Object...)} must be used in order to access the remote
	 * object.
	 * 
	 * @param type the type of the object to create
	 * @param args the constructor arguments
	 * @return a proxy for the remote object
	 * @throws ReflectiveOperationException if an exception occurred during object creation
	 */
	public static <T> T create(Class<T> type, Object... args) throws ReflectiveOperationException {
		args = firstNonNull(args, ArrayUtils.EMPTY_OBJECT_ARRAY);
		Constructor<?> constructor = ReflectionUtil.findConstructor(type, ReflectionUtil.getClassesFromObjects(args));
		return create(constructor, type, args);
	}

	/**
	 * Creates a remote CloudObject using the given proxy and parameters for the remote constructor invocation.
	 * <p/>
	 * <b>Note that this method is intended to be used by JCloudScale internally.</b><br/>
	 * It is recommended to use {@link #create(Class, Object...)} instead.
	 *
	 * @param constructor the constructor to use
	 * @param type the type of the object to create
	 * @param args the constructor arguments
	 * @return the proxy
	 * @throws NoSuchMethodException if there are zero or more than one constructor candidates
	 * @see #create(Class, Object...)
	 */
	public static <T> T create(Constructor<?> constructor, Class<T> type, Object... args) {
		
		// we just have to ensure that JCloudScaleClient is started before doing anything with JCloudScale.
		JCloudScaleClient.getClient();

		// create a proxy of this object with cglib
		T newProxy = null;
		try {
			newProxy = (T) CgLibUtil.replaceCOWithProxy(type, args, constructor.getParameterTypes());
		} catch (Throwable e) {
			throw new JCloudScaleException(e);
		}

		ReflectionUtil.checkLegalCloudIdDef(newProxy);
		ReflectionUtil.checkLegalCloudInvocationInfoDef(newProxy);

		// find which parameters should be passed by ref, and which should be passed by value
		Object[] processedParams = JCloudScaleReferenceManager.getInstance().processArguments(constructor, args);
		
		UUID id = CloudManager.getInstance().createNewInstance(
				type, processedParams, constructor.getParameterTypes(), newProxy
		);

		// this doesn't actually work at the moment (and it's also not strictly required)
//		ReflectionUtil.injectCloudId(newProxy, id);

		idMaps.put(newProxy, id);
		return newProxy;
	}

	/**
	 * Destroys the remote object associated with the given proxy.<br/>
	 * If this method is invoked for a destroyed CloudObject, it simply does nothing.
	 * 
	 * @param obj the proxy of the object to destroy.
	 */
	public static void destroy(Object obj) {
		if (JCloudScaleConfiguration.isServerContext()) {
			return;
		}

		UUID id = idMaps.get(obj);
		if (id == null || destroyedIds.contains(id)) {
			return;
		}

		destroyedIds.add(id);
		CloudManager.getInstance().destructCloudObject(id);
		
	}

	/**
	 * Invokes a desired method on remote object using the provided arguments.
	 * 
	 * @param obj the remote object
	 * @param method the method to invoke
	 * @param args the arguments
	 * @return the result of the method invocation
	 */
	public static Object invoke(Object obj, Method method, Object... args) {
		UUID id = getId(obj);
		if (!isCloudObject(id)) {
			throw new JCloudScaleException(id == null ? "Invocation target is no CloudObject!"
					: "Cannot invoke method on destroyed CloudObject!");
		}
		return CloudManager.getInstance().invokeCloudObject(id, method, args, method.getParameterTypes());
	}

	/**
	 * Returns the unique identifier of the proxy.
	 * <p/>
	 * Note that the identifiers are preserved even if the objects are already destroyed.
	 * 
	 * @param obj the proxy
	 * @return the unique identifier or {@code null} if the object is no known cloud object
	 */
	public static UUID getId(Object obj) {
		return idMaps.get(obj);
	}

	/**
	 * Checks whether the given id belongs to a known available cloud object.<br/>
	 * A cloud object is available until it has been marked for destruction.
	 * 
	 * @param id the unique identifier to query
	 * @return {@code true} if there is a remote cloud object with the given ID, {@code false} otherwise.
	 */
	public static boolean isCloudObject(UUID id) {
		return id != null && idMaps.containsValue(id) && !destroyedIds.contains(id);
	}

	/**
	 * Checks whether the given id belongs to a cloud object that is already destroyed.
	 * 
	 * @param id the unique identifier to query
	 * @return {@code true} if a cloud object was associated with the given identifier, {@code false} otherwise.
	 */
	public static boolean isDestroyed(UUID id) {
		return id != null && destroyedIds.contains(id);
	}
}
