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

import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.dto.DatastoreFieldDto;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.mapping.DatastoreFieldMapper;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.mapping.DatastoreFieldMapperImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.model.DatastoreModel;
import org.hibernate.Session;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class AddWork extends AbstractWork {

    private Object entity;
    private DatastoreFieldMapper datastoreFieldMapper = new DatastoreFieldMapperImpl();

    public AddWork(Object entity) {
        this.entity = entity;
    }

    @Override
    public void doWork(Session session) {
        List<Field> datastoreFields = getNotNullDatastoreFields(entity);
        List<DatastoreFieldDto> datastoreFieldDtos = map(entity, datastoreFields);
        List<DatastoreModel> datastoreModels = addFieldsToDatastore(datastoreFieldDtos);
        save(session, datastoreModels);
    }

    private List<DatastoreModel> addFieldsToDatastore(List<DatastoreFieldDto> datastoreFieldDtos) {
        List<DatastoreModel> datastoreModels = new ArrayList<DatastoreModel>();
        for(DatastoreFieldDto datastoreFieldDto : datastoreFieldDtos) {
            datastoreModels.add(datastoreFieldHandler.save(datastoreFieldDto));
        }
        return datastoreModels;
    }

    private void save(Session session, List<DatastoreModel> datastoreModels) {
        for(DatastoreModel datastoreModel : datastoreModels) {
            session.save(datastoreModel);
        }
    }
}
