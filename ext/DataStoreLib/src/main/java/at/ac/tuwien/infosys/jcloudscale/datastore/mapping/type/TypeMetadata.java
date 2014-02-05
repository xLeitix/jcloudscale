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

import java.util.List;

/**
 * Metadata for a Type
 */
public interface TypeMetadata<T> {

    /**
     * Get the parent object if the type is a field
     *
     * @return the parent object
     */
    Object getParent();

    /**
     * Get the name of the field
     *
     * @return the field name
     */
    String getFieldName();

    /**
     * Get the type of the field
     *
     * @return the field type
     */
    Class<?> getFieldType();

    /**
     * Get the types of the type parameters if generic
     *
     * @return the type parameter types
     */
    List<Class<?>> getTypeParameterTypes();

    /**
     * Get the type adapter for the field
     *
     * @return the type adapter
     */
    TypeAdapter<?, T> getTypeAdapter();

    /**
     * Checks if the field has a custom type adapter
     *
     * @return true if custom type adapter present, false otherwise
     */
    boolean isCustomTypeAdapterPresent();

    /**
     * Get the type adapters for the type parameters if is generic
     *
     * @return the type adapters
     */
    List<TypeAdapter<?, T>> getTypeParameterTypeAdapters();

    /**
     * Checks if the class of the field is java internal
     *
     * @return true if is java internal, false otherwise
     */
    boolean isJavaInternalClass();
}
