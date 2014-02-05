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

/**
 * DTO for a datastore field
 */
public class DatastoreFieldDto {

    /**
     * Name of the datastore field
     */
    private String fieldName;

    /**
     * Value of the datastore field
     */
    private Object value;

    /**
     * Name of the datastore where field is located
     */
    private String datastoreName;

    /**
     * Name of the corresponding entity class
     */
    private String entityClassName;

    /**
     * ID of the corresponding entity
     */
    private String entityID;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getDatastoreName() {
        return datastoreName;
    }

    public void setDatastoreName(String datastoreName) {
        this.datastoreName = datastoreName;
    }

    public String getEntityClassName() {
        return entityClassName;
    }

    public void setEntityClassName(String entityClassName) {
        this.entityClassName = entityClassName;
    }

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DatastoreFieldDto [");
        stringBuilder.append("fieldName=" + fieldName + ", ");
        stringBuilder.append("value=" + value + ", ");
        stringBuilder.append("datastoreName=" + datastoreName + ", ");
        stringBuilder.append("entityClassName=" + entityClassName + ", ");
        stringBuilder.append("entityID=" + entityID);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
