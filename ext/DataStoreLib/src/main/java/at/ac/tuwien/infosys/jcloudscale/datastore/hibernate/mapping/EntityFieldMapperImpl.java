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

import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.dto.EntityFieldDto;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.model.DatastoreModel;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ClassUtil;

import java.lang.reflect.Field;

public class EntityFieldMapperImpl implements EntityFieldMapper {

    @Override
    public EntityFieldDto map(Object entity, DatastoreModel datastoreModel) {
        EntityFieldDto entityFieldDto = new EntityFieldDto();
        entityFieldDto.setEntity(entity);
        Field field = ClassUtil.getField(datastoreModel.getEntityFieldName(), entity.getClass());
        entityFieldDto.setField(field);
        Datastore datastoreAnnotation = field.getAnnotation(Datastore.class);
        entityFieldDto.setDatastoreName(datastoreAnnotation.value());
        entityFieldDto.setFieldType(field.getType());
        entityFieldDto.setDatastoreID(datastoreModel.getDatastoreID());
        return entityFieldDto;
    }
}
