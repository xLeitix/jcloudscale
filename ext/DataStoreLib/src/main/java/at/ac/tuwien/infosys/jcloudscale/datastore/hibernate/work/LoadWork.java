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

import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.dto.EntityFieldDto;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.mapping.EntityFieldMapper;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.mapping.EntityFieldMapperImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.model.DatastoreModel;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.List;

public class LoadWork extends AbstractWork {

    private Object entity;
    private EntityFieldMapper entityFieldMapper = new EntityFieldMapperImpl();

    public LoadWork(Object entity) {
        this.entity = entity;
    }

    @Override
    public void doWork(Session session) {
        List<DatastoreModel> datastoreModels = getDatastoreModelsForEntity(session, entity);
        List<EntityFieldDto> entityFieldDtos = map(datastoreModels);
        load(entityFieldDtos);
    }

    private List<EntityFieldDto> map(List<DatastoreModel> datastoreModels) {
        List<EntityFieldDto> entityFieldDtos = new ArrayList<EntityFieldDto>();
        for(DatastoreModel datastoreModel : datastoreModels) {
            entityFieldDtos.add(entityFieldMapper.map(entity, datastoreModel));
        }
        return entityFieldDtos;
    }

    private void load(List<EntityFieldDto> entityFieldDtos) {
        for(EntityFieldDto entityFieldDto : entityFieldDtos) {
            datastoreFieldHandler.load(entityFieldDto);
        }
    }

}
