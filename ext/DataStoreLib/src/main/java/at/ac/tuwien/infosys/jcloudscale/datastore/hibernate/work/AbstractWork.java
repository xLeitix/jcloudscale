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
package at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.work;

import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreFieldHandler;
import at.ac.tuwien.infosys.jcloudscale.datastore.core.DatastoreFieldHandlerImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.dto.DatastoreFieldDto;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.mapping.DatastoreFieldMapper;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.mapping.DatastoreFieldMapperImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.model.DatastoreModel;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ClassUtil;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.EntityUtil;
import org.hibernate.Query;
import org.hibernate.Session;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWork implements Work{

    protected DatastoreFieldHandler datastoreFieldHandler = new DatastoreFieldHandlerImpl();
    protected DatastoreFieldMapper datastoreFieldMapper = new DatastoreFieldMapperImpl();

    protected List<Field> getNotNullDatastoreFields(Object entity) {
        List<Field> result = new ArrayList<Field>();
        List<Field> datastoreFields = ClassUtil.getFieldsWithAnnotation(entity.getClass(), Datastore.class);

        for (Field field : datastoreFields) {
            if(!EntityUtil.fieldIsNull(field, entity)) {
                result.add(field);
            }
        }
        return result;
    }

    protected List<DatastoreFieldDto> map(Object entity, List<Field> datastoreFields) {
        List<DatastoreFieldDto> datastoreFieldDtos = new ArrayList<DatastoreFieldDto>();
        for(Field field : datastoreFields) {
            datastoreFieldDtos.add(datastoreFieldMapper.map(field, entity, field.getAnnotation(Datastore.class)));
        }
        return datastoreFieldDtos;
    }

    protected List<DatastoreModel> getDatastoreModelsForEntity(Session session, Object entity) {
        Query query = session.getNamedQuery("datastoreModel.getModelForEntity");
        query.setParameter("entityClassName", entity.getClass().getSimpleName());
        query.setParameter("entityID", String.valueOf(EntityUtil.getID(entity)));
        return query.list();
    }

    protected DatastoreModel getDatastoreModelForDto(List<DatastoreModel> datastoreModels, DatastoreFieldDto datastoreFieldDto) {
        for(DatastoreModel datastoreModel : datastoreModels) {
            if(datastoreModel.getEntityFieldName().equals(datastoreFieldDto.getFieldName())) {
                return datastoreModel;
            }
        }
        return null;
    }
}
