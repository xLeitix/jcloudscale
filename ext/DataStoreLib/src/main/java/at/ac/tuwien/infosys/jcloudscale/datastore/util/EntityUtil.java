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

import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;

import javax.persistence.Id;
import java.lang.reflect.Field;

public abstract class EntityUtil {

    /**
     * Get the ID of a given entity object
     *
     * @param entity given entity object
     * @return ID of the entity
     */
    public static Object getID(Object entity) {
        Field idField = ClassUtil.getFirstFieldWithAnnotation(entity.getClass(), Id.class);
        if(idField != null) {
            Object id = ObjectUtils.getFieldValue(idField, entity);
            return id;
        }
        return null;
    }

    /**
     * Check if given entity has a datastore field
     *
     * @param entity given entity
     * @return true if entity has datastore field, false otherwise
     */
    public static boolean hasDatastoreField(Object entity) {
        return ClassUtil.hasFieldWithAnnotation(entity.getClass(), Datastore.class);
    }

    /**
     * Check if given field on given entity is null
     *
     * @param field given field
     * @param entity given entity
     * @return true if field is null, false otherwise
     */
    public static boolean fieldIsNull(Field field, Object entity) {
        try {
            field.setAccessible(true);
            return field.get(entity) == null;
        } catch (Exception e) {
            throw new DatastoreException("Error getting field: " + field.getName());
        }
    }
}
