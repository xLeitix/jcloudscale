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
import com.google.gson.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.HashMap;
import java.util.Map;

public class JsonMapTypeAdapter implements TypeAdapter<Map<?,?>, JsonElement> {

    @Override
    public JsonElement serialize(Map<?, ?> object, TypeMetadata<JsonElement> typeMetadata) {
        JsonObject jsonObject = new JsonObject();
        TypeAdapter valueTypeAdapter = typeMetadata.getTypeParameterTypeAdapters().get(1);
        for(Object key : object.keySet()) {
            JsonElement valueElement = (JsonElement) valueTypeAdapter.serialize(object.get(key), typeMetadata);
            jsonObject.add(String.valueOf(key), valueElement);
        }
        return jsonObject;
    }

    @Override
    public Map<?, ?> deserialize(JsonElement element, TypeMetadata<JsonElement> typeMetadata) {
        JsonObject jsonObject = (JsonObject) element;
        TypeAdapter valueTypeAdapter = typeMetadata.getTypeParameterTypeAdapters().get(1);
        Map<Object, Object> map = new HashMap<Object, Object>();
        for(Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            PropertyEditor editor = PropertyEditorManager.findEditor(typeMetadata.getTypeParameterTypes().get(0));
            editor.setAsText(entry.getKey());
            Object key = editor.getValue();
            Object value = valueTypeAdapter.deserialize(entry.getValue(), typeMetadata);
            map.put(key, value);
        }
        return map;
    }
}
