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

/**
 * Datastore API v1.0
 */
public interface Datastore {

    /**
     * Get the name of the datastore
     *
     * @return name of the datastore
     */
    String getName();

    /**
     * Get the host of the datastore
     *
     * @return the host of the datastore
     */
    String getHost();

    /**
     * Get the port of the datastore
     *
     * @return the port of the datastore
     */
    Integer getPort();

    /**
     * Get the name of the data unit
     *
     * @return name of the data unit
     */
    String getDataUnit();

    /**
     * Get the driver for the datastore
     *
     * @return the driver for the datastore
     */
    DatastoreDriver getDatastoreDriver();

    /**
     * Get given property for datastore
     *
     * @param propertyName name of the property
     * @return the datastore property
     */
    String getProperty(String propertyName);

    /**
     * Adds an external library for the given datastore
     *
     * @param name the name to use for the library
     * @param libWrapper the wrapper for the external library
     */
    void addExternalLibrary(String name, LibWrapper libWrapper);

    /**
     * Get the external library with the given name
     *
     * @param name the given name
     * @return the external library
     */
    LibWrapper getExternalLibrary(String name);

    /**
     * Save an object in the datastore
     *
     * @param object object to store in datastore
     */
    void save(Object object);

    /**
     * Find an object by the given id.
     *
     * @param objectClass class of the wanted object
     * @param id id to search for
     * @param <T> type of the searched object
     * @return object resulting from the search
     */
    <T> T find(Class<T> objectClass, String id);

    /**
     * Delete an object from the datastore
     *
     * @param object the object to delete
     */
    void delete(Object object);

    /**
     * Delete the object with the given ID from the datastore
     *
     * @param clazz the class of the object to delete
     * @param id the given ID
     */
    <T> void deleteById(Class<T> clazz, String id);

    /**
     * Update an object in the datastore
     *
     * @param object the object to update
     */
    void update(Object object);

    /**
     * Migrate the object with the given id to the given datastore
     *
     * @param to the datastore to migrate to
     * @param objectClass the class of the object to migrate
     * @param id the id of the object to migrate
     */
    <T> void migrate(Datastore to, Class<T> objectClass, String id);

    /**
     * Migrate the objects with the given ids to the given datastore
     *
     * @param to the datastore to migrate to
     * @param objectClass the class of the objects to migrate
     * @param callback the callback object
     * @param ids the ids of the objects to migrate
     */
    <T> void migrate(Datastore to, Class<T> objectClass, MigrationCallback<T> callback, String... ids);
}
