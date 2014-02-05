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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public abstract class ClassUtil {

    /**
     * Checks if a given class contains a filed with the given annotation
     *
     * @param clazz      given class
     * @param annotation given annotation
     * @return true if class has field with annotation, false otherwise
     */
    public static boolean hasFieldWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return all fields of the given class with the given annotation
     *
     * @param clazz      given class
     * @param annotation given annotation
     * @return fields with given annotation
     */
    public static List<Field> getFieldsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        List<Field> fields = new ArrayList<Field>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(annotation)) {
                fields.add(field);
            }
        }
        return fields;
    }

    /**
     * Return the first filed of the given class with the given annotation
     *
     * @param clazz given class
     * @param annotation given annotation
     * @return first field with the given annotation
     */
    public static Field getFirstFieldWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        List<Field> fields = getFieldsWithAnnotation(clazz, annotation);
        if(fields != null && !fields.isEmpty()) {
            return fields.get(0);
        }
        return null;
    }

    /**
     * Return an instance of the class with the given name
     *
     * @param className name of the class
     * @param returnClass class returned
     * @return instance of the class with the given name
     */
    public static <T> T getInstanceByName(String className, Class<T> returnClass) {
        try {
            Class<?> clazz = Class.forName(className);
            return (T) clazz.newInstance();
        } catch (Exception e) {
            throw new DatastoreException("Error loading driver: " + e.getMessage());
        }
    }

    /**
     * Get field with given name from given class
     *
     * @param fieldName given field name
     * @param clazz given class
     * @return field from class with given name
     */
    public static Field getField(String fieldName, Class<?> clazz) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            return field;
        } catch (NoSuchFieldException e) {
            throw new DatastoreException("Field " + fieldName + " not found on class " + clazz.getName());
        }
    }

    /**
     * Create an instance of the given class
     *
     * @param targetClass given class
     * @return a new instance of the given class
     */
    public static <T> T createInstance(Class<T> targetClass) {
        try {
            return targetClass.newInstance();
        } catch (Exception e) {
            throw new DatastoreException("Error creating instance of class " + targetClass);
        }
    }

    /**
     * Determines if the given class is a list
     *
     * @param clazz the given class
     * @return true if is a list, false otherwise
     */
    public static boolean isListClass(Class<?> clazz) {
        return clazz.equals(List.class) || clazz.equals(ArrayList.class);
    }
}
