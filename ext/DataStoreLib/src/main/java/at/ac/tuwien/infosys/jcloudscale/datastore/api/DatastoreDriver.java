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

import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoader;

/**
 * Interface which has to be implemented by a datastore driver for use in
 * DatastoreManager
 */
public interface DatastoreDriver {

    /**
     * Get property loader for datastore
     *
     * @return the property loader or null if none exists
     */
    PropertyLoader getPropertyLoader();

    /**
     * Store a new object in the given datastore
     *
     *
     * @param datastore given datastore
     * @param object the object to persist
     * @return id of the new stored object
     */
    String save(Datastore datastore, Object object);

    /**
     * Store a new object with the given id in the datastore
     *
     * @param datastore given datastore
     * @param id the id to use in the datastore
     * @param object the object to persist
     */
    void save(Datastore datastore, String id, Object object);

    /**
     * Find an object in the given datastore by the given id.
     *
     * @param datastore given datastore
     * @param objectClass class of the searched object
     * @param id the id to search for
     * @return the found object
     */
    <T> T find(Datastore datastore, Class<T> objectClass, String id);

    /**
     * Delete an object with the given id from the given datastore
     *
     * @param datastore given datastore
     * @param objectClass class of the object to delete
     * @param id id of the object to delete
     */
    void delete(Datastore datastore, Class<?> objectClass, String id);

    /**
     * Update an object with the given id in the given datastore
     *
     * @param datastore given datastore
     * @param id id of the object to update
     * @param object object to update
     */
    void update(Datastore datastore, String id, Object object);
}
