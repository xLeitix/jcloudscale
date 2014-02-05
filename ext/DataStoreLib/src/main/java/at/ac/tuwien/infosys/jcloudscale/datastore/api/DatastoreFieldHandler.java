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
package at.ac.tuwien.infosys.jcloudscale.datastore.api;

import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.dto.EntityFieldDto;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.dto.DatastoreFieldDto;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.model.DatastoreModel;

/**
 * API to handle datastore field references
 */
public interface DatastoreFieldHandler {


    /**
     * Save a single datastore field
     *
     *
     * @param datastoreFieldDto field to save
     * @return Datastore Model Entry of the field
     */
    DatastoreModel save(DatastoreFieldDto datastoreFieldDto);

    /**
     * Load a single datastore field
     *
     * @param entityFieldDto dto with field and entity information
     */
    void load(EntityFieldDto entityFieldDto);

    /**
     * Update a single datastore field
     *
     * @param datastoreFieldDto dto with field information
     * @param datastoreModel the datastore model for the field
     */
    void update(DatastoreFieldDto datastoreFieldDto, DatastoreModel datastoreModel);

    /**
     * Delete a single datastore field
     *
     * @param datastoreFieldDto
     * @param datastoreModel
     */
    void delete(DatastoreFieldDto datastoreFieldDto, DatastoreModel datastoreModel);
}
