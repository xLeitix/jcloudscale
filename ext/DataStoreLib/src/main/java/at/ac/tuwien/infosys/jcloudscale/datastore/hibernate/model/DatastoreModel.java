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
package at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.model;

import javax.persistence.*;

@NamedQueries({
        @NamedQuery(
                name = "datastoreModel.getModelForEntity",
                query = "select m from DatastoreModel m where m.entityClassName = :entityClassName and m.entityID = :entityID"
        )
})
@Entity
@Table(name = "CLOUDSCALE_DATASTORE_MODEL")
public class DatastoreModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String entityClassName;

    private String entityFieldName;

    private String entityID;

    private String datastoreID;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityClassName() {
        return entityClassName;
    }

    public void setEntityClassName(String entityClassName) {
        this.entityClassName = entityClassName;
    }

    public String getEntityFieldName() {
        return entityFieldName;
    }

    public void setEntityFieldName(String entityFieldName) {
        this.entityFieldName = entityFieldName;
    }

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }

    public String getDatastoreID() {
        return datastoreID;
    }

    public void setDatastoreID(String datastoreID) {
        this.datastoreID = datastoreID;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DatastoreModel [");
        stringBuilder.append("entityClassName=" + entityClassName + ", ");
        stringBuilder.append("entityFieldName=" + entityFieldName + ", ");
        stringBuilder.append("entityID=" + entityID + ", ");
        stringBuilder.append("datastoreID=" + datastoreID);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
