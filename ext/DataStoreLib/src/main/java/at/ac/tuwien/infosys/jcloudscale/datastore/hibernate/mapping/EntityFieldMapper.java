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

import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.dto.EntityFieldDto;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.model.DatastoreModel;

public interface EntityFieldMapper {

    /**
     * Map to entity field DTO
     *
     * @param entity given entity
     * @param datastoreModel corresponding datastore model entry
     * @return entity field DTO
     */
    EntityFieldDto map(Object entity, DatastoreModel datastoreModel);
}
