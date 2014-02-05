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
package at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.json;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeMetadata;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class JsonEnumTypeAdapter implements TypeAdapter<Enum, JsonElement> {

    @Override
    public JsonElement serialize(Enum object, TypeMetadata<JsonElement> typeMetadata) {
        return new JsonPrimitive(object.name());
    }

    @Override
    public Enum deserialize(JsonElement element, TypeMetadata<JsonElement> typeMetadata) {
        JsonPrimitive jsonPrimitive = (JsonPrimitive) element;
        if(!jsonPrimitive.isString()) {
            throw new DatastoreException("Invalid value for enum type.");
        }
        Class<? extends Enum> enumClass = (Class<? extends Enum>) typeMetadata.getFieldType();
        return Enum.valueOf(enumClass, jsonPrimitive.getAsString());
    }
}
