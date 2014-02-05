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
package at.ac.tuwien.infosys.jcloudscale.datastore.core;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.DatastoreConfiguration;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.DatastoreTemplate;

import java.util.logging.Logger;

public class DatastoreFactory {

    private static final Logger log = JCloudScaleConfiguration.getLogger(DatastoreFactory.class);

    private static DatastoreFactory datastoreFactory;
    private static DatastoreConfiguration datastoreConfiguration = new DatastoreConfiguration();

    public static DatastoreFactory getInstance() {
        if(datastoreFactory == null) {
            datastoreFactory = new DatastoreFactory();
        }
        return datastoreFactory;
    }

    public Datastore getDatastoreByName(String name) {
        DatastoreTemplate datastoreTemplate = datastoreConfiguration.getTemplate(name);
        if(datastoreTemplate == null) {
            log.warning("Could not find datastore with name " + name);
            return null;
        }
        log.info("Found datastore template " + datastoreTemplate);
        return new DatastoreBuilder(datastoreTemplate).build();
    }

    public static void setDatastoreConfiguration(DatastoreConfiguration datastoreConfiguration) {
        DatastoreFactory.datastoreConfiguration = datastoreConfiguration;
    }
}
