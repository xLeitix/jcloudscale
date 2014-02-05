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
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.dto.DatastoreFieldDto;
import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.model.DatastoreModel;

import java.lang.reflect.Field;

public interface DatastoreFieldMapper {

    /**
     * Map a given {java.lang.reflect.Field} to a DatastoreFieldDto
     *
     * @param field field to map
     * @param object Object containing the field
     * @param datastoreAnnotation Datastore Annotation for the given field
     * @return DatastoreFieldDto for given field
     */
    DatastoreFieldDto map(Field field, Object object, Datastore datastoreAnnotation);

    /**
     * Create a datastore model object from the given DatastoreFieldDto
     *
     * @param datastoreFieldDto given DatastoreFieldDto
     * @return datastore model object
     */
    DatastoreModel convertToDatastoreModel(DatastoreFieldDto datastoreFieldDto);
}
