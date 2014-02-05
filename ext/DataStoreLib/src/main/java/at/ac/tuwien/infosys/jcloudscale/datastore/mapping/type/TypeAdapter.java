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
package at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type;

/**
 * API for Type Adapters (v 2.0)
 *
 * @param <T> the type of the field
 * @param <K> the type of the datastore element
 */
public interface TypeAdapter<T, K> {

    /**
     * Serialize the given object to an element of the type K
     *
     * @param object the object to serialize
     * @param typeMetadata the metadata for the field
     * @return the serialized object
     */
    K serialize(T object, TypeMetadata<K> typeMetadata);

    /**
     * Deserialize an element of the type K to an object of the type T
     *
     * @param element the element to deserialize
     * @param typeMetadata the metadata for the field
     * @return the deserialized object
     */
    T deserialize(K element, TypeMetadata<K> typeMetadata);

}
