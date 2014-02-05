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
package at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.dto;

import java.lang.reflect.Field;

/**
 * DTO for a entity field
 */
public class EntityFieldDto {

    /**
     * Entity containing the field
     */
    private Object entity;

    /**
     * Entity Field
     */
    private Field field;

    /**
     * Name of the datastore with field value
     */
    private String datastoreName;

    /**
     * Type of the field
     */
    private Class<?> fieldType;

    /**
     * ID of the datastore entry
     */
    private String datastoreID;


    public Object getEntity() {
        return entity;
    }

    public void setEntity(Object entity) {
        this.entity = entity;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public String getDatastoreName() {
        return datastoreName;
    }

    public void setDatastoreName(String datastoreName) {
        this.datastoreName = datastoreName;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    public void setFieldType(Class<?> fieldType) {
        this.fieldType = fieldType;
    }

    public String getDatastoreID() {
        return datastoreID;
    }

    public void setDatastoreID(String datastoreID) {
        this.datastoreID = datastoreID;
    }
}
