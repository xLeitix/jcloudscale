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
package at.ac.tuwien.infosys.jcloudscale.datastore.ext;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.LibWrapper;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.DatastoreProperties;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;

/**
 * Wrapper for the LightCouch Library
 */
public class LightCouchWrapper implements LibWrapper {

    @Override
    public Object getLib(Datastore datastore) {
        CouchDbProperties properties = new CouchDbProperties();
        properties.setDbName(datastore.getDataUnit());
        properties.setHost(datastore.getHost());
        properties.setPort(datastore.getPort());
        properties.setCreateDbIfNotExist(false);
        properties.setProtocol(DatastoreProperties.DEFAULT_PROTOCOL_TYPE.toString());
        return new CouchDbClient(properties);
    }
}
