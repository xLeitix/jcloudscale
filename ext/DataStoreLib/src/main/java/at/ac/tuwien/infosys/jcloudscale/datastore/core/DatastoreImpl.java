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

import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.DatastoreId;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.*;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoader;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoaderChain;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ReflectionUtil;
import at.ac.tuwien.infosys.jcloudscale.datastore.validation.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Map;
import java.util.logging.Logger;

public class DatastoreImpl implements Datastore {

    private final Logger log;

    private final String name;
    private final String host;
    private final Integer port;
    private final String dataUnit;
    private final DatastoreDriver datastoreDriver;
    private final PropertyLoaderChain propertyLoaderChain;
    private final Map<String, LibWrapper> libraries;

    public DatastoreImpl(String name, String host, Integer port, String dataUnit, DatastoreDriver datastoreDriver, Map<String, LibWrapper> libraries) {
        this.log = JCloudScaleConfiguration.getLogger(this);
        this.name = name;
        this.host = host;
        this.port = port;
        this.dataUnit = dataUnit;
        this.datastoreDriver = datastoreDriver;
        this.propertyLoaderChain = new PropertyLoaderChain();
        this.libraries = libraries;

        initProperties();
    }

    private void initProperties() {
        PropertyLoader propertyLoader = datastoreDriver.getPropertyLoader();
        if(propertyLoader !=null) {
            this.propertyLoaderChain.add(propertyLoader);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public String getDataUnit() {
        return dataUnit;
    }

    @Override
    public DatastoreDriver getDatastoreDriver() {
        return datastoreDriver;
    }

    @Override
    public String getProperty(String propertyName) {
        return propertyLoaderChain.load(propertyName);
    }

    @Override
    public void addExternalLibrary(String name, LibWrapper libWrapper) {
        libraries.put(name, libWrapper);
    }

    @Override
    public LibWrapper getExternalLibrary(String name) {
        return libraries.get(name);
    }

    @Override
    public void save(Object object) throws DatastoreException {
        log.info("Saving object " + object);

        performSaveValidations(object);

        String id = null;
        if(useManualIdStrategy(object)) {
            id = String.valueOf(ReflectionUtil.getFieldValueOfFieldWithAnnotation(object, DatastoreId.class));
            log.info("Using Manual ID Strategy with ID " + id);
            datastoreDriver.save(this, id, object);
        } else {
            log.info("Using Auto ID Strategy.");
            id = datastoreDriver.save(this, object);
            ReflectionUtil.setFieldValueForFieldWithAnnotation(DatastoreId.class, object, id);
        }

        log.info("Finished saving object " + object);
    }

    @Override
    public <T> T find(Class<T> objectClass, String id) throws DatastoreException {
        log.info("Searching for object with ID " + id);

        performFindValidations(objectClass, id);

        T object = datastoreDriver.find(this, objectClass, id);
        ReflectionUtil.setFieldValueForFieldWithAnnotation(DatastoreId.class, object, id);

        log.info("Finished searching for Object.");
        return object;
    }

    @Override
    public void delete(Object object) {
        performDeleteValidations(object);

        String id = String.valueOf(ReflectionUtil.getFieldValueOfFieldWithAnnotation(object, DatastoreId.class));
        log.info("Deleting object with ID " + id);

        datastoreDriver.delete(this, object.getClass(), id);

        log.info("Finished deleting Object.");
    }

    @Override
    public <T> void deleteById(Class<T> clazz, String id) {
        ValidatorChain validatorChain = new ValidatorChainImpl();
        validatorChain.add(new NotNullValidator(id));
        validatorChain.validate();

        log.info("Deleting object with ID " + id);

        datastoreDriver.delete(this, clazz.getClass(), id);

        log.info("Finished deleting Object.");
    }

    @Override
    public void update(Object object) {
        performUpdateValidations(object);

        String id = String.valueOf(ReflectionUtil.getFieldValueOfFieldWithAnnotation(object, DatastoreId.class));
        log.info("Updating object with ID " + id);

        datastoreDriver.update(this, id, object);

        log.info("Finished updating object with ID " + id);
    }

    @Override
    public <T> void migrate(Datastore to, Class<T> objectClass, String id) {
        log.info("Migrating object with ID " + id + " from datastore " + this + " to " + to);

        performMigrateValidations(to, objectClass, id);

        //get object
        T object = find(objectClass, id);

        //save object
        to.getDatastoreDriver().save(to, id, object);

        log.info("Finished migrating object with ID " + id + " from datastore " + this + " to " + to);
    }

    @Override
    public <T> void migrate(Datastore to, Class<T> objectClass, MigrationCallback<T> callback, String... ids) {
        log.info("Migrating objects with IDs " + ids + " from datastore " + this + " to " + to);

        MigrationWorker<T> migrationWorker = new MigrationWorker<T>(this, to, objectClass, callback, ids);
        Thread migrationThread = new Thread(migrationWorker);
        migrationThread.start();
    }

    private boolean useManualIdStrategy(Object object) {
        return ReflectionUtil.getAnnotationFromField(object, DatastoreId.class).strategy() == IdStrategy.MANUAL;
    }

    private void performSaveValidations(Object object) {
        ValidatorChain validatorChain = new ValidatorChainImpl();
        validatorChain.add(new NotNullValidator(object));
        validatorChain.add(new HasValidIdStrategyValidator(object));
        validatorChain.validate();
    }

    private <T> void performFindValidations(Class<T> objectClass, String id) {
        ValidatorChain validatorChain = new ValidatorChainImpl();
        validatorChain.add(new NotNullValidator(objectClass));
        validatorChain.add(new NotNullValidator(id));
        validatorChain.validate();
    }

    private void performDeleteValidations(Object object) {
        performUpdateValidations(object);
    }

    private void performUpdateValidations(Object object) {
        ValidatorChain validatorChain = new ValidatorChainImpl();
        validatorChain.add(new NotNullValidator(object));
        validatorChain.add(new HasIdValidator(object));
        validatorChain.validate();
    }

    private <T> void performMigrateValidations(Datastore to, Class<T> objectClass, String id) {
        ValidatorChain validatorChain = new ValidatorChainImpl();
        validatorChain.add(new NotNullValidator(to));
        validatorChain.add(new NotNullValidator(objectClass));
        validatorChain.add(new NotNullValidator(id));
        validatorChain.validate();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("name", name)
                .append("host", host)
                .append("port", port)
                .append("dataUnit", dataUnit)
                .toString();
    }
}
