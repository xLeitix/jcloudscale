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

import java.lang.reflect.Field;

public final class ObjectUtils {

    private static String DEFAULT_POJO_MIME_TYPE = "application/json";

    //Util Class
    private ObjectUtils(){};

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
     * Set the value on the given field on the given object
     *
     * @param field given field
     * @param object given object
     * @param value value of the field
     */
    public static void setFieldValue(Field field, Object object, Object value) {
        if(field == null || object == null) {
            return;
        }
        try {
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new DatastoreException("Error setting value on field " + field.getName());
        }
    }
}