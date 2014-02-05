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
package at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.mapping;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.dto.DatastoreFieldDto;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.model.DatastoreModel;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.EntityUtil;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.logging.Logger;

public class DatastoreFieldMapperImpl implements DatastoreFieldMapper {

    private Logger log;

    public DatastoreFieldMapperImpl() {
        this.log = JCloudScaleConfiguration.getLogger(this);
    }

    @Override
    public DatastoreFieldDto map(Field field, Object object, Datastore datastoreAnnotation) {
        DatastoreFieldDto datastoreFieldDto = new DatastoreFieldDto();
        datastoreFieldDto.setFieldName(field.getName());
        Object fieldValue = ObjectUtils.getFieldValue(field, object);
        datastoreFieldDto.setValue(fieldValue);
        datastoreFieldDto.setDatastoreName(datastoreAnnotation.value());
        datastoreFieldDto.setEntityClassName(object.getClass().getSimpleName());
        Object entityID = EntityUtil.getID(object);
        datastoreFieldDto.setEntityID(String.valueOf(entityID));
        log.info("Created DTO: " + datastoreFieldDto);
        return datastoreFieldDto;
    }

    @Override
    public DatastoreModel convertToDatastoreModel(DatastoreFieldDto datastoreFieldDto) {
        DatastoreModel datastoreModel = new DatastoreModel();
        datastoreModel.setEntityClassName(datastoreFieldDto.getEntityClassName());
        datastoreModel.setEntityFieldName(datastoreFieldDto.getFieldName());
        datastoreModel.setEntityID(datastoreFieldDto.getEntityID());
        return datastoreModel;
    }
}
