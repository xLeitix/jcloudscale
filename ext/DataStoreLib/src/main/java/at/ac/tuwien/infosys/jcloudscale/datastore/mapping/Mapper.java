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
package at.ac.tuwien.infosys.jcloudscale.datastore.mapping;

/**
 * General Mapper Interface
 */
public interface Mapper<T> {

    /**
     * Serialize the given object
     *
     * @param object the given object
     * @return the serialized object
     */
    T serialize(Object object);

    /**
     * Deserialize the given serialized object
     *
     * @param serializedObject the serialized object
     * @param targetClass the class of the target object
     * @return the deserialized object
     */
    <K> K deserialize(T serializedObject, Class<K> targetClass);
}
