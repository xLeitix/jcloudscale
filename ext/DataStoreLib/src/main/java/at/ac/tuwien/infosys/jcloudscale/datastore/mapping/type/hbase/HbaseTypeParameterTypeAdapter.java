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
package at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.hbase;

import at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase.HbaseCell;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.json.JsonMapperImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeMetadata;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeMetadataImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.json.JsonMapTypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.json.JsonTypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ReflectionUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.hadoop.hbase.util.Bytes;

public class HbaseTypeParameterTypeAdapter implements TypeAdapter<Object, HbaseCell> {

    private int typeParameterIndex;

    public HbaseTypeParameterTypeAdapter(int typeParameterIndex) {
        this.typeParameterIndex = typeParameterIndex;
    }

    @Override
    public HbaseCell serialize(Object object, TypeMetadata<HbaseCell> typeMetadata) {
        String className = typeMetadata.getParent().getClass().getSimpleName();
        String jsonString = new JsonMapperImpl().serialize(object);
        return new HbaseCell(className, typeMetadata.getFieldName(), Bytes.toBytes(jsonString));
    }

    @Override
    public Object deserialize(HbaseCell element, TypeMetadata<HbaseCell> typeMetadata) {
        String jsonString = Bytes.toString(element.getValue());
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(jsonString);
        TypeMetadata<JsonElement> jsonTypeMetadata = new TypeMetadataImpl(typeMetadata, ReflectionUtil.getFieldByName(typeMetadata.getParent(), typeMetadata.getFieldName()), new JsonTypeAdapterFactory(), JsonElement.class);
        return new JsonMapTypeAdapter().deserialize(jsonElement, jsonTypeMetadata);
    }
}
