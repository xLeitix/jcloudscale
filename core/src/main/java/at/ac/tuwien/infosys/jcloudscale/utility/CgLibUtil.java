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
package at.ac.tuwien.infosys.jcloudscale.utility;

import java.lang.reflect.Method;
import java.util.UUID;

import net.sf.cglib.core.CodeGenerationException;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import at.ac.tuwien.infosys.jcloudscale.annotations.DestructCloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.Local;
import at.ac.tuwien.infosys.jcloudscale.api.CloudObjects;
import at.ac.tuwien.infosys.jcloudscale.aspects.CloudObjectAspect;
import at.ac.tuwien.infosys.jcloudscale.classLoader.RemoteClassLoaderUtils;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.management.JCloudScaleReference;
import at.ac.tuwien.infosys.jcloudscale.management.JCloudScaleReferenceManager;
import at.ac.tuwien.infosys.jcloudscale.server.ServerCallbackManager;

public class CgLibUtil {
	
	public static Object[] replaceRefsWithProxies(Object[] objectParams, ClassLoader classloader) 
			throws Throwable {
		
		Object[] proxies = new Object[objectParams.length];
		for(int i=0; i<objectParams.length; i++) {
			proxies[i] = replaceRefWithProxy(objectParams[i], classloader);
		}
		
		return proxies; 
	}
	
	public static Object replaceRefWithProxy(Object object, ClassLoader classloader) 
			throws Throwable {
		
		if(object == null)
			return null;
	
		if(object.getClass().equals(JCloudScaleReference.class)) {
			
			JCloudScaleReference ref = (JCloudScaleReference) object;
			
			Class<?> loadedClazz = RemoteClassLoaderUtils.getClass(ref.getReferenceObjectClassName(), classloader);
			
			return createReferenceProxy(loadedClazz, ref, classloader);
			
		} else {
			return object;
		}
		
	}
	
	public static Object replaceCOWithProxy(Class<?> className, Object[] parameters, Class<?>[] parameterTypes) throws Throwable {
		
		Enhancer enhancer = new Enhancer();
		enhancer.setCallback(new JCloudScaleCloudObjectHandler());
		enhancer.setSuperclass(className);
		
		try {
			if(parameters != null && parameterTypes.length > 0)
				return enhancer.create(parameterTypes, parameters);
			else
				return enhancer.create();
		} catch(CodeGenerationException e) {
			// we are not particularly interested in the CodeGenerationException, more in what caused it
			if(e.getCause() != null)
				throw e.getCause();
			else
				throw e;
		}
	}
	
	private static Object createReferenceProxy(Class<?> templateClass, JCloudScaleReference ref, ClassLoader classloader) throws Throwable {
		
		try {
			return Enhancer.create(templateClass, new JCloudScaleReferenceHandler(ref, classloader));
		} catch(IllegalArgumentException e) {
			// this will happen if no no-arg constructor is present
			throw new JCloudScaleException(
				"Class "+templateClass.getName()+" is used as by-reference parameter but does not contain a no-arg constructor");
		} catch(CodeGenerationException e) {
			// we are not particularly interested in the CodeGenerationException, more in what caused it
			if(e.getCause() != null)
				throw e.getCause();
			else
				throw e;
		}
	    
	}
	
	public static boolean isOverwriteableMethod(Method method) {
		
		// method should not be overridden if it is a finalize method
		// or annotated with @Local
		
		String mName = method.getName();
		if(mName.equals("finalize") && method.getParameterTypes().length == 0)
			return false;
		else {
			return (method.getAnnotation(Local.class) == null);
		}
			
		
	}
	
	public static boolean isCGLibEnhancedClass(Class<?> clazz) {
		return (clazz.getName().contains("EnhancerByCGLIB"));
	}
	
	private static class JCloudScaleCloudObjectHandler implements MethodInterceptor {

		@Override
		public Object intercept(Object o, Method method, Object[] params, MethodProxy proxy) throws Throwable {
			
			if(!isOverwriteableMethod(method))
				return proxy.invokeSuper(o, params);
			
			Object result = null;

			UUID id = CloudObjects.getId(o);
			
			if(id == null)
			{
				JCloudScaleConfiguration.getLogger(this)
						.warning("Invocation of the method "+method.getName()+" on the not-deployed or unknown object! " +
									"Invocation will be performed locally and side effects will not be transferred to the server!");
				
				return proxy.invokeSuper(o, params);
			}
			
			if (CloudObjects.isDestroyed(id))
				throw new JCloudScaleException("Method invocation on already destroyed CloudObject!");
			
			Object[] processedParams = JCloudScaleReferenceManager.getInstance().processArguments(method, params);
			
			result = CloudManager.getInstance().invokeCloudObject(id, method,
					processedParams, method.getParameterTypes());
			
			// TODO: this should better be implemented as a pointcut, but this is easier for now :D 
			if(method.isAnnotationPresent(DestructCloudObject.class)) {
				
				new CloudObjectAspect().destructCloudObject(o);
				
			}
			
			return result;
			
		}
	}
	
	private static class JCloudScaleReferenceHandler implements MethodInterceptor {
		
		private ClassLoader classloader;
		private JCloudScaleReference ref;
		
		public JCloudScaleReferenceHandler(JCloudScaleReference ref, ClassLoader classloader) {
			this.ref = ref;
			this.classloader = classloader;
		}
		
		@Override
		public Object intercept(Object o, Method method, Object[] params, MethodProxy proxy) throws Throwable {
			
			if(!isOverwriteableMethod(method))
				return proxy.invokeSuper(o, params);
			
			ServerCallbackManager callbackManager = ServerCallbackManager.getInstance();
			Object response = callbackManager.callback(ref, method, params, method.getParameterTypes(), classloader);
			return response;
			
		}

	}
	
}
