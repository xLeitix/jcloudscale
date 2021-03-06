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

import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.Mapper;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigator;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigatorImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.Visitor;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.json.JsonTypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ReflectionUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonMapperImpl implements Mapper<String> {

    private TypeAdapterFactory<JsonElement> typeAdapterFactory;
    private Gson gson = new Gson();

    public JsonMapperImpl() {
        this.typeAdapterFactory = new JsonTypeAdapterFactory();
    }

    @Override
    public String serialize(Object object) {
        JsonObject jsonObject = new JsonObject();
        ObjectNavigator objectNavigator = new ObjectNavigatorImpl(typeAdapterFactory);
        Visitor visitor = new JsonSerializationVisitor(jsonObject);
        objectNavigator.navigate(object, visitor);
        return gson.toJson(jsonObject);
    }

    @Override
    public <K> K deserialize(String serializedObject, Class<K> targetClass) {
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject) parser.parse(serializedObject);
        K object = ReflectionUtil.createInstance(targetClass);
        ObjectNavigator objectNavigator = new ObjectNavigatorImpl(typeAdapterFactory);
        Visitor visitor = new JsonDeserializationVisitor(jsonObject, object);
        objectNavigator.navigate(object, visitor);
        return object;
    }
}
