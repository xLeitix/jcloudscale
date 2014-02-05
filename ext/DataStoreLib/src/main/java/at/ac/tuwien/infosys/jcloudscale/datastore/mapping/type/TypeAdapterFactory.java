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
 * Factory for creating type adapters for elements of type T
 *
 * @param <T> datastore element type
 */
public interface TypeAdapterFactory<T> {

    /**
     * Create a type adapter for the given class
     *
     * @param clazz the given class
     * @return the type adapter for the class
     */
    TypeAdapter<?, T> get(Class<?> clazz);

    /**
     * Get the type adapter for type parameters
     *
     * @param typeParameterIndex the index of the type parameter
     * @return the type adapter
     */
    TypeAdapter<Object, T> getTypeParameterTypeAdapter(int typeParameterIndex);
}
