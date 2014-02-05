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
package at.ac.tuwien.infosys.jcloudscale.aspects;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.FieldSignature;

import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObjectParent;
import at.ac.tuwien.infosys.jcloudscale.annotations.Local;
import at.ac.tuwien.infosys.jcloudscale.api.CloudObjects;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.CloudConfigException;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.utility.CgLibUtil;

@Aspect
public class CloudObjectAspect {
	
	@Around("call(!@at.ac.tuwien.infosys.jcloudscale.annotations.Local (@at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject *).new(..))")
	public Object createNewCloudObject(ProceedingJoinPoint jp) throws Throwable 
	{
			// check if we are running in a server context
			if(JCloudScaleConfiguration.isServerContext())
				return jp.proceed();
			
			Class<?> coType = jp.getSignature().getDeclaringType();
			Constructor<?> constructor = ((ConstructorSignature)jp.getSignature()).getConstructor();
			
			// check if this is already a CGLib modified class
			// TODO: it would be more efficient to add this to the pointcut def above
			if(CgLibUtil.isCGLibEnhancedClass(coType))
				return jp.proceed();

			return deployCloudObject(coType, jp.getArgs(), constructor);
			
	}
	
	@Around("call(* java.lang.reflect.Constructor.newInstance(..)) && target(constructor)")
	public Object createNewCloudObjectViaReflection(ProceedingJoinPoint pjp, Object constructor) throws Throwable {
		
		// check if we are running in a server context
		if(JCloudScaleConfiguration.isServerContext())
			return pjp.proceed();
		
		Constructor<?> constr = (Constructor<?>)constructor;
		Class<?> coType = constr.getDeclaringClass(); 
		
		// check if we are constructing some other object
		if(!coType.isAnnotationPresent(CloudObject.class))
			return pjp.proceed();
		
		// check if this is already a CGLib modified class
		if(CgLibUtil.isCGLibEnhancedClass(coType))
			return pjp.proceed();
		
		// everything checked, we should intercept this reflection call
		return deployCloudObject(coType, pjp.getArgs(), constr);
		
	}
	
	@Around("call(* java.lang.Class.newInstance()) && target(clazz)")
	public Object createNewCloudObjectViaReflectionDirectlyFromClass(ProceedingJoinPoint pjp, Object clazz) throws Throwable {
		
		Constructor<?> constr = ((Class<?>)clazz).getConstructor();
		return createNewCloudObjectViaReflection(pjp, constr);
		
	}
	
	private <T> T deployCloudObject(Class<T> coType, Object[] arguments, Constructor<?> constructor) throws JCloudScaleException, CloudConfigException {

		return CloudObjects.create(constructor, coType, arguments);
	}
	
	// TODO: moved to CGLibUtil - not very nice but easier than making the pointcut work for now
	// @AfterReturning("target(obj) && execution(@at.ac.tuwien.infosys.jcloudscale.annotations.DestructCloudObject * *(..))")
	public void destructCloudObject(Object obj) throws JCloudScaleException, CloudConfigException {
		
		CloudObjects.destroy(obj);
	}
	
	@Around("target(object) && " +
			"(get(!@at.ac.tuwien.infosys.jcloudscale.annotations.Local * @at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject *.*) || " +
			" get(!@at.ac.tuwien.infosys.jcloudscale.annotations.Local * @at.ac.tuwien.infosys.jcloudscale.annotations.CloudObjectParent *.*))")
	public Object getCloudObjectField(Object object, ProceedingJoinPoint pjp) throws Throwable {
		
		// 
		
		if(JCloudScaleConfiguration.isServerContext())
			return pjp.proceed();
		
		UUID id = CloudObjects.getId(object);
		
		if(id == null) {
			// maybe we are just working on a cloudobjectparent that is not actually a cloud object
			return pjp.proceed();
		}
		
		FieldSignature sig = (FieldSignature) pjp.getSignature();
		Field field = sig.getField();
		
		return getFieldValue(id, field);
		
	}
	
	@Around("call(* java.lang.reflect.Field.get(..)) && target(field)")
	public Object getCloudObjectFieldViaReflection(ProceedingJoinPoint pjp, Field field) throws Throwable {
		
		// check if we are running in a server context
		if(JCloudScaleConfiguration.isServerContext())
			return pjp.proceed();
		
		// check if this field is @local
		if(field.getAnnotation(Local.class) != null)
			return pjp.proceed();
			
		Field theField = field;
		Object arg = pjp.getArgs()[0];

		if(arg == null) {
			// we are getting a static field
			return pjp.proceed();
		}
		
		UUID id = CloudObjects.getId(arg);
		
		// check if we are intercepting a class we are not interested in
		if(!isCloudObjectOrParent(theField.getDeclaringClass()))
			return pjp.proceed();
		
		return getFieldValue(id, theField);
		
	}
	
	private Object getFieldValue(UUID id, Field field) {
		
		if (CloudObjects.isDestroyed(id)) {
			throw new JCloudScaleException("Field access on already destroyed CloudObject!");
		}
		
		return CloudManager.getInstance().getFieldValue(id, field);
		
	}
	
	@Around("target(object) && args(val) && " +
			"(set(!@at.ac.tuwien.infosys.jcloudscale.annotations.Local * @at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject *.*) || " +
			" set(!@at.ac.tuwien.infosys.jcloudscale.annotations.Local * @at.ac.tuwien.infosys.jcloudscale.annotations.CloudObjectParent *.*))")
	public void setCloudObjectField(Object object, Object val, ProceedingJoinPoint pjp) throws Throwable {
		
		if(JCloudScaleConfiguration.isServerContext()) {
			pjp.proceed();
			return;
		}
		
		UUID id = CloudObjects.getId(object);
		
		if(id == null) {
			// maybe we are just working on a cloudobjectparent that is not actually a cloud object
			pjp.proceed();
			return;
		}
		
		if(CloudObjects.isDestroyed(id)) {
			// throw new JCloudScaleException("Field access on already destroyed CloudObject!");
			// this case happens during regular object construction - just proceed
			pjp.proceed();
			return;
		}
		
		FieldSignature sig = (FieldSignature) pjp.getSignature();
		Field field = sig.getField();
		
		setFieldValue(id, field, val);
		
	}
	
	@Around("call(* java.lang.reflect.Field.set(..)) && target(field)")
	public void setCloudObjectFieldViaReflection(ProceedingJoinPoint pjp, Field field) throws Throwable {
		
		// check if we are running in a server context
		if(JCloudScaleConfiguration.isServerContext()) {
			pjp.proceed();
			return;
		}	
				
		Field theField = field;
		
		// check if this field is @local
		if(field.getAnnotation(Local.class) != null) {
			pjp.proceed();
			return;
		}
		
		// check if we are intercepting a class we are not interested in
		if(!isCloudObjectOrParent(theField.getDeclaringClass())) {
			pjp.proceed();
			return;
		}
		
		Object arg = pjp.getArgs()[0];
		Object val = pjp.getArgs()[1];

		if(arg == null) {
			// we are setting a static field
			pjp.proceed();
			return;
		}
		
		UUID id = CloudObjects.getId(arg);
		
		setFieldValue(id, theField, val);
		
	}
	
	private void setFieldValue(UUID id, Field field, Object value) {
		
		CloudManager.getInstance().setFieldValue(id, field, value);
		
	}
	
	private boolean isCloudObjectOrParent(Class<?> clazz) {
		
		return
			clazz.isAnnotationPresent(CloudObject.class) || 
			clazz.isAnnotationPresent(CloudObjectParent.class);
		
	}
	
}
