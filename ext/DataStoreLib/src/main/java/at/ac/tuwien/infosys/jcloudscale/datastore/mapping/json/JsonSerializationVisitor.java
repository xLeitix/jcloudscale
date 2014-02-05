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
package at.ac.tuwien.infosys.jcloudscale.datastore.mapping.json;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigator;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigatorImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.Visitor;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeMetadata;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.json.JsonTypeAdapterFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Visitor for mapping objects to JSON
 */
public class JsonSerializationVisitor implements Visitor<JsonElement> {

    private JsonObject jsonObject;

    //Hide default constructor
    private JsonSerializationVisitor() {
    }

    public JsonSerializationVisitor(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @Override
    public Class<JsonElement> getTargetClass() {
        return JsonElement.class;
    }

    @Override
    public void visit(Object value, TypeMetadata<JsonElement> typeMetadata) {
        if (value != null) {
            JsonElement jsonElement;
            if (typeMetadata.isJavaInternalClass()) {
                jsonElement = handleJavaInternal(value, typeMetadata);
            } else {
                jsonElement = handleUserDefined(value, typeMetadata);
            }
            jsonObject.add(typeMetadata.getFieldName(), jsonElement);
        }
    }

    private JsonElement handleJavaInternal(Object value, TypeMetadata<JsonElement> typeMetadata) {
        TypeAdapter typeAdapter = typeMetadata.getTypeAdapter();
        if(typeAdapter == null) {
            throw new DatastoreException("Not type adapter found for type: " + typeMetadata.getFieldType());
        }
        return (JsonElement) typeAdapter.serialize(value, typeMetadata);
    }


    private JsonElement handleUserDefined(Object value, TypeMetadata<JsonElement> typeMetadata) {
        if (typeMetadata.isCustomTypeAdapterPresent()) {
            TypeAdapter typeAdapter = typeMetadata.getTypeAdapter();
            return (JsonElement) typeAdapter.serialize(value, typeMetadata);
        } else {
            JsonObject jsonObject = new JsonObject();
            ObjectNavigator objectNavigator = new ObjectNavigatorImpl(new JsonTypeAdapterFactory());
            Visitor visitor = new JsonSerializationVisitor(jsonObject);
            objectNavigator.navigate(value, visitor);
            return jsonObject;
        }
    }
}
