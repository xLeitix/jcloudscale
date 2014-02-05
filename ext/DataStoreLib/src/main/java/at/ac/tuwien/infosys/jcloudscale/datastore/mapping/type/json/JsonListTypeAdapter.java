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

import at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase.HbaseCell;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeMetadata;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.ArrayList;
import java.util.List;

public class JsonListTypeAdapter implements TypeAdapter<List<?>, JsonElement> {

    @Override
    public JsonElement serialize(List<?> object, TypeMetadata<JsonElement> typeMetadata) {
        JsonArray jsonArray = new JsonArray();
        TypeAdapter typeAdapter = typeMetadata.getTypeParameterTypeAdapters().get(0);
        for(Object item : object) {
            JsonElement jsonElement = (JsonElement) typeAdapter.serialize(item, typeMetadata);
            jsonArray.add(jsonElement);
        }
        return jsonArray;
    }

    @Override
    public List<?> deserialize(JsonElement element, TypeMetadata<JsonElement> typeMetadata) {
        JsonArray jsonArray = (JsonArray) element;
        List<Object> list = new ArrayList<Object>();
        TypeAdapter typeAdapter = typeMetadata.getTypeParameterTypeAdapters().get(0);
        for(JsonElement item : jsonArray) {
            Object object = typeAdapter.deserialize(item, typeMetadata);
            list.add(object);
        }
        return list;
    }
}
