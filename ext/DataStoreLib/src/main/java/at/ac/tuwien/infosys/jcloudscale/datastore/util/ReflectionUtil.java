/*
   Copyright 2013 Rene Nowak 

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
package at.ac.tuwien.infosys.jcloudscale.datastore.util;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Util class for reflection related methods
 */
public final class ReflectionUtil {

    //Prevent Initialization
    private ReflectionUtil(){};

    /**
     * Returns the value of a given filed on a given object
     *
     * @param field given field
     * @param object given object
     * @return value of the field
     */
    public static Object getFieldValue(Field field, Object object) {
        if(field == null || object == null) {
            return null;
        }
        Object fieldValue = null;
        try {
            field.setAccessible(true);
            fieldValue = field.get(object);
        } catch (Exception e) {
            throw new DatastoreException("Error getting value from field: " + field.getName());
        }
        return fieldValue;
    }

    /**
     * Determines if the type of a given field is java internal
     *
     * @param field the given field
     * @return true if java internal, false otherwise
     */
    public static boolean isJavaInternalType(Field field) {
        return isJavaInternalType(field.getType());
    }

    /**
     * Determines if the given class is java internal
     *
     * @param clazz the given class
     * @return true if java internal, false otherwise
     */
    public static boolean isJavaInternalType(Class<?> clazz) {
        ClassLoader classLoader = clazz.getClassLoader();
        if(classLoader == null || classLoader.getParent() == null || clazz.isEnum()) {
            return true;
        }
        return false;
    }

    /**
     * Create an instance of the given class
     *
     * @param targetClass given class
     * @return a new instance of the given class
     */
    public static <T> T createInstance(Class<T> targetClass) {
        try {
            //check if class is inner class
            if(targetClass.getEnclosingClass() != null && !Modifier.isStatic(targetClass.getModifiers())) {
                return createInstanceInnerClass(targetClass);
            }
            return targetClass.newInstance();
        } catch (Exception e) {
            throw new DatastoreException("Error creating instance of class " + targetClass);
        }
    }

    /**
     * Create an instance of the given inner class
     *
     * @param targetClass given inner class
     * @return a new instance of the inner class
     */
    public static <T> T createInstanceInnerClass(Class<T> targetClass) {
        try {
            Constructor<T> constructor = targetClass.getDeclaredConstructor(targetClass.getEnclosingClass());
            constructor.setAccessible(true);
            return constructor.newInstance(createInstance(targetClass.getEnclosingClass()));
        } catch (NoSuchMethodException e) {
            throw new DatastoreException("Error creating instance for inner class " + targetClass);
        } catch (InvocationTargetException e) {
            throw new DatastoreException("Error creating instance for inner class " + targetClass);
        } catch (InstantiationException e) {
            throw new DatastoreException("Error creating instance for inner class " + targetClass);
        } catch (IllegalAccessException e) {
            throw new DatastoreException("Error creating instance for inner class " + targetClass);
        }
    }

    /**
     * Set the field value of an object
     *
     * @param target the target object
     * @param fieldName the name of the field to set
     * @param fieldValue the value of the field
     */
    public static void setFieldValue(Object target, String fieldName, Object fieldValue) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, fieldValue);
        } catch (NoSuchFieldException e) {
            throw new DatastoreException("Error setting field value. Field " + fieldName + " not found.");
        } catch (IllegalAccessException e) {
            throw new DatastoreException("Error setting field " + fieldName + ": " + e.getMessage());
        }
    }

    /**
     * Create an instance for the type of the specified field
     *
     * @param object the object containing the field
     * @param fieldName the name of the field
     * @return the instance of the field type
     */
    public static Object createInstanceForField(Object object, String fieldName) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            return createInstance(field.getType());
        } catch (NoSuchFieldException e) {
            throw new DatastoreException("Error creating instance for field. Field " + fieldName + " not found.");
        }
    }


    /**
     * Set the given value on the field of the given object annotated with an annotation of the given class
     *
     * @param annotationClass the class of the annotation
     * @param object the object containing the field
     * @param value the value to set
     */
    public static void setFieldValueForFieldWithAnnotation(Class<? extends Annotation> annotationClass, Object object, Object value) {
        Field field = getFieldWithAnnotation(object, annotationClass);
        setFieldValue(object, field.getName(), value);
    }

    /**
     * Get the first field with an annotation of the given annotation class
     *
     * @param object the object containing the fields
     * @param annotationClass the class of the annotation
     * @return a field
     */
    public static Field getFieldWithAnnotation(Object object, Class<? extends Annotation> annotationClass) {
        for(Field field : object.getClass().getDeclaredFields()) {
            if(field.isAnnotationPresent(annotationClass)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Get the annotation from the given object
     *
     * @param object given object
     * @param annotationClass the class of the annotation
     * @return the annotation
     */
    public static <T extends Annotation> T getAnnotationFromField(Object object, Class<T> annotationClass) {
        Field[] declaredFields = object.getClass().getDeclaredFields();
        for(Field field : declaredFields) {
            if(field.isAnnotationPresent(annotationClass)) {
                return field.getAnnotation(annotationClass);
            }
        }
        return null;
    }

    /**
     * Get the value of the field with the annotation of the given class
     *
     * @param object the object containing the field
     * @param annotationClass the class of the annotation
     * @return the field value
     */
    public static Object getFieldValueOfFieldWithAnnotation(Object object, Class<? extends Annotation> annotationClass) {
        Field field = getFieldWithAnnotation(object, annotationClass);
        return getFieldValue(field, object);
    }

    /**
     * Get the field with the given name from the given object
     *
     * @param object given object
     * @param fieldName the field name
     * @return the field
     */
    public static Field getFieldByName(Object object, String fieldName) {
        try {
            return object.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new DatastoreException("Error getting field with name " + fieldName);
        }
    }
}
