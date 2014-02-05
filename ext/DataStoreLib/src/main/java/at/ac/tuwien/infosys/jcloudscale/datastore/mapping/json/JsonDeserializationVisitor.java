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

import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigator;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigatorImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.Visitor;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeMetadata;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.json.JsonTypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ReflectionUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonDeserializationVisitor implements Visitor<JsonElement> {

    private JsonObject jsonObject;
    private Object object;

    //Hide default constructor
    private JsonDeserializationVisitor() {
    }

    public JsonDeserializationVisitor(JsonObject jsonObject, Object object) {
        this.jsonObject = jsonObject;
        this.object = object;
    }

    @Override
    public Class<JsonElement> getTargetClass() {
        return JsonElement.class;
    }

    @Override
    public void visit(Object value, TypeMetadata<JsonElement> typeMetadata) {
        JsonElement jsonElement = jsonObject.get(typeMetadata.getFieldName());
        if (jsonElement != null) {
            if (typeMetadata.isJavaInternalClass()) {
                handleJavaInternal(jsonElement, typeMetadata);
            } else {
                handleUserDefined(jsonElement, typeMetadata);
            }
        }
    }

    private void handleJavaInternal(JsonElement jsonElement, TypeMetadata<JsonElement> typeMetadata) {
        TypeAdapter<?, JsonElement> typeAdapter = typeMetadata.getTypeAdapter();
        Object fieldValue = typeAdapter.deserialize(jsonElement, typeMetadata);
        ReflectionUtil.setFieldValue(object, typeMetadata.getFieldName(), fieldValue);
    }

    private void handleUserDefined(JsonElement jsonElement, TypeMetadata<JsonElement> typeMetadata) {
        Object fieldValue = null;
        if (typeMetadata.isCustomTypeAdapterPresent()) {
            TypeAdapter<?, JsonElement> typeAdapter = typeMetadata.getTypeAdapter();
            fieldValue = typeAdapter.deserialize(jsonElement, typeMetadata);
        } else {
            JsonObject jsonObject = (JsonObject) jsonElement;
            fieldValue = ReflectionUtil.createInstanceForField(object, typeMetadata.getFieldName());
            ObjectNavigator objectNavigator = new ObjectNavigatorImpl(new JsonTypeAdapterFactory());
            Visitor visitor = new JsonDeserializationVisitor(jsonObject, fieldValue);
            objectNavigator.navigate(fieldValue, visitor);
        }
        ReflectionUtil.setFieldValue(object, typeMetadata.getFieldName(), fieldValue);
    }
}
