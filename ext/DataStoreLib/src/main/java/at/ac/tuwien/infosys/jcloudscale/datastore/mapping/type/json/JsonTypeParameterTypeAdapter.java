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

import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigator;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigatorImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.Visitor;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.json.JsonDeserializationVisitor;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.json.JsonSerializationVisitor;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeMetadata;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ReflectionUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Special Type Adapter necessary to handle type parameters with user defined objects
 * Necessary because Java uses Type Erasure to handle generics
 */
public class JsonTypeParameterTypeAdapter implements TypeAdapter<Object, JsonElement> {

    private int typeParameterIndex;
    private TypeAdapterFactory<JsonElement> typeAdapterFactory;

    public JsonTypeParameterTypeAdapter(int typeParameterIndex) {
        this.typeAdapterFactory = new JsonTypeAdapterFactory();
        this.typeParameterIndex = typeParameterIndex;
    }

    @Override
    public JsonElement serialize(Object object, TypeMetadata<JsonElement> typeMetadata) {
        JsonObject jsonObject = new JsonObject();
        ObjectNavigator objectNavigator = new ObjectNavigatorImpl(typeAdapterFactory);
        Visitor visitor = new JsonSerializationVisitor(jsonObject);
        objectNavigator.navigate(object, visitor);
        return jsonObject;
    }

    @Override
    public Object deserialize(JsonElement element, TypeMetadata<JsonElement> typeMetadata) {
        JsonObject jsonObject = (JsonObject) element;
        List<Class<?>> typeParameterTypes = typeMetadata.getTypeParameterTypes();
        Object object = ReflectionUtil.createInstance(typeParameterTypes.get(typeParameterIndex));
        ObjectNavigator objectNavigator = new ObjectNavigatorImpl(typeAdapterFactory);
        Visitor visitor = new JsonDeserializationVisitor(jsonObject, object);
        objectNavigator.navigate(object, visitor);
        return object;
    }
}
