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
package at.ac.tuwien.infosys.jcloudscale.datastore.hibernate;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.work.AbstractWork;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.work.WorkFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.EntityUtil;
import org.hibernate.event.*;

import java.util.logging.Logger;

public class DatastoreEventListener implements PostInsertEventListener, PostLoadEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private Logger log;
    private DatastoreProcess datastoreProcess = new DatastoreProcess();

    public DatastoreEventListener() {
        this.log = JCloudScaleConfiguration.getLogger(this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        Object entity = event.getEntity();
        log.info("Processing post insert for Entity " + entity.getClass().getSimpleName() + " with id " + EntityUtil.getID(entity));
        addWorkerIfHasDatastoreField(event, entity);
    }

    @Override
    public void onPostLoad(PostLoadEvent postLoadEvent) {
        Object entity = postLoadEvent.getEntity();
        log.info("Processing load event for Entity " + entity.getClass().getSimpleName() + " with id " + EntityUtil.getID(entity));
        addWorkerIfHasDatastoreField(postLoadEvent, entity);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent postUpdateEvent) {
        Object entity = postUpdateEvent.getEntity();
        log.info("Processing post update event for Entity " + entity.getClass().getSimpleName() + " with id " + EntityUtil.getID(entity));
        addWorkerIfHasDatastoreField(postUpdateEvent, entity);
    }

    @Override
    public void onPostDelete(PostDeleteEvent postDeleteEvent) {
        Object entity = postDeleteEvent.getEntity();
        log.info("Processing post delete event for Entity " + entity.getClass().getSimpleName() + " with id " + EntityUtil.getID(entity));
        addWorkerIfHasDatastoreField(postDeleteEvent, entity);
    }

    private void addWorkerIfHasDatastoreField(AbstractEvent event, Object entity) {
        if(!EntityUtil.hasDatastoreField(entity)) {
            log.info("Entity has no datastore field. Skipping process.");
            return;
        }
        log.info("Adding Worker to datastore process.");
        AbstractWork work = WorkFactory.forEvent(event, entity);
        datastoreProcess.addWork(work);
        event.getSession().getActionQueue().registerProcess(datastoreProcess);
    }
}
