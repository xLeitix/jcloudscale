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
import at.ac.tuwien.infosys.jcloudscale.datastore.api.MigrationCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A worker thread for migration tasks
 */
public class MigrationWorker<T> implements Runnable {

    private Logger log;
    private Datastore from;
    private Datastore to;
    private Class<T> objectClass;
    private MigrationCallback<T> callback;
    private String[] ids;

    public MigrationWorker(Datastore from, Datastore to, Class<T> objectClass, MigrationCallback<T> callback, String[] ids) {
        this.log = JCloudScaleConfiguration.getLogger(this);
        this.from = from;
        this.to = to;
        this.objectClass = objectClass;
        this.callback = callback;
        this.ids = ids;
    }

    @Override
    public void run() {
        List<T> result = new ArrayList<T>();
        try {
            for(String id : ids) {
                log.fine("Migrating object with ID " + id + " from " + from + " to " + to);
                T object = from.find(objectClass, id);
                to.getDatastoreDriver().save(to, id, object);
                result.add(object);
            }
            callback.onSuccess(result);
        } catch (Exception e) {
            log.warning("Error migrating objects " + e.getMessage());
            callback.onError();
        }
    }
}
