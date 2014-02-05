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
package at.ac.tuwien.infosys.jcloudscale.datastore.configuration;

import at.ac.tuwien.infosys.jcloudscale.datastore.rest.ProtocolType;

/**
 * Datastore Properties Mapping
 */
public final class DatastoreProperties {

    //Hide default constructor
    private DatastoreProperties() {}

    /**
     * Default Protocol Type
     */
    public static final ProtocolType DEFAULT_PROTOCOL_TYPE = ProtocolType.HTTP;

    /**
     * Default properties file name
     */
    public static final String DEFAULT_PROPERTIES_FILE = "default.properties";

    /**
     * Request URL for persisting new objects
     */
    public static final String URL_SAVE = "datastore.url.save";

    /**
     * Request URL for persisting new object with given id
     */
    public static final String URL_SAVE_ID = "datastore.url.save.id";

    /**
     * Request URL for finding objects
     */
    public static final String URL_FIND = "datastore.url.find";

    /**
     * Request URL for deleting objects
     */
    public static final String URL_DELETE = "datastore.url.delete";

    /**
     * Request URL for updating objects
     */
    public static final String URL_UPDATE = "datastore.url.update";

    /**
     * Request URL for finding CouchDB attachments
     */
    public static final String URL_FIND_COUCHDB_ATTACHMENT = "datastore.url.couchdb.find.attachment";

    /**
     * Request URL for getting the current revision of a CouchDB document
     */
    public static final String URL_COUCHDB_CURRENT_REVISION = "datastore.url.couchdb.current.revision";

    /**
     * Determines if class type information should be stored
     */
    public static final String SAVE_CLASS_TYPE = "datastore.save.class.type";

    /**
     * The default path for tem files
     */
    public static final String TEMP_FILE_PATH = "temp.file.path";
}
