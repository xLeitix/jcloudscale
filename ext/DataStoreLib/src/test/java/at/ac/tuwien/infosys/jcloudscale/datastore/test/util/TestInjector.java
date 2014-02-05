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
package at.ac.tuwien.infosys.jcloudscale.datastore.test.util;


import java.lang.reflect.Field;

/**
 * Helper Methods to inject fields
 */
public class TestInjector {

    /**
     * Method to inject a value into an object by the type of the value
     * @param objectToInject object to inject value into
     * @param value value to inject
     */
    public static void injectByType(Object objectToInject, Object value) {
        try {
            for (Field field : objectToInject.getClass().getDeclaredFields()) {
                if (field.getType().equals(value.getClass())) {
                    field.setAccessible(true);
                    field.set(objectToInject, value);
                }
            }
        } catch (Exception e) {
            //DO NOTHING
        }
    }

    /**
     * Method to inject a value into a field by name
     * @param objectToInject object to inject value into
     * @param fieldName name of the field to inject value into
     * @param value value to inject
     */
    public static void injectByName(Object objectToInject, String fieldName, Object value) {
        try {
            Field field = null;
            try {
                field = objectToInject.getClass().getDeclaredField(fieldName);
            } catch (NoSuchFieldException nfe) {
                //try to get field from base class
                field = objectToInject.getClass().getSuperclass().getDeclaredField(fieldName);
            }
            field.setAccessible(true);
            field.set(objectToInject, value);
        } catch (Exception e) {
            //DO NOTHING
        }
    }
}
