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
import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreFieldHandler;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.dto.DatastoreFieldDto;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.dto.EntityFieldDto;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.mapping.DatastoreFieldMapper;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.mapping.DatastoreFieldMapperImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.model.DatastoreModel;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ObjectUtils;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ReflectionUtil;

import java.util.logging.Logger;

public class DatastoreFieldHandlerImpl implements DatastoreFieldHandler {

    private Logger log;
    private DatastoreFieldMapper datastoreFieldMapper;

    public DatastoreFieldHandlerImpl() {
        log = JCloudScaleConfiguration.getLogger(this);
        datastoreFieldMapper = new DatastoreFieldMapperImpl();
    }

    @Override
    public DatastoreModel save(DatastoreFieldDto datastoreFieldDto) {
        //get datastore
        log.info("Getting Datastore");
        String datastoreName = datastoreFieldDto.getDatastoreName();
        Datastore datastore = DatastoreFactory.getInstance().getDatastoreByName(datastoreName);

        //save value
        log.info("Saving Field in Datastore");
        datastore.save(datastoreFieldDto.getValue());

        //create mapper
        log.info("Adding fieldID to DTO");
        DatastoreModel datastoreModel = datastoreFieldMapper.convertToDatastoreModel(datastoreFieldDto);
        String datastoreID = (String) ReflectionUtil.getFieldValueOfFieldWithAnnotation(datastoreFieldDto.getValue(), DatastoreId.class);
        datastoreModel.setDatastoreID(datastoreID);

        return datastoreModel;
    }

    @Override
    public void load(EntityFieldDto entityFieldDto) {
        //get datastore
        log.info("Getting Datastore");
        String datastoreName = entityFieldDto.getDatastoreName();
        Datastore datastore = DatastoreFactory.getInstance().getDatastoreByName(datastoreName);

        //get value
        log.info("Get Value from Datastore");
        Object fieldValue = datastore.find(entityFieldDto.getFieldType(), entityFieldDto.getDatastoreID());

        //set value
        log.info("Setting Value on Entity");
        ObjectUtils.setFieldValue(entityFieldDto.getField(), entityFieldDto.getEntity(), fieldValue);
    }

    @Override
    public void update(DatastoreFieldDto datastoreFieldDto, DatastoreModel datastoreModel) {
        //get datastore
        log.info("Getting Datastore");
        String datastoreName = datastoreFieldDto.getDatastoreName();
        Datastore datastore = DatastoreFactory.getInstance().getDatastoreByName(datastoreName);

        //update value
        log.info("Updating Field in Datastore");
        datastore.update(datastoreFieldDto.getValue());
    }

    @Override
    public void delete(DatastoreFieldDto datastoreFieldDto, DatastoreModel datastoreModel) {
        //get datastore
        log.info("Getting Datastore");
        String datastoreName = datastoreFieldDto.getDatastoreName();
        Datastore datastore = DatastoreFactory.getInstance().getDatastoreByName(datastoreName);

        //delete value
        log.info("Deleting Field in Datastore");
        datastore.deleteById(datastoreFieldDto.getValue().getClass(), datastoreModel.getDatastoreID());
    }
}
