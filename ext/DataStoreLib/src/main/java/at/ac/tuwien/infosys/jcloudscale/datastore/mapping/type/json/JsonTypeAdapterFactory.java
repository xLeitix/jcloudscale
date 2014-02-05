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

import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.AbstractTypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeToken;
import com.google.gson.JsonElement;

import java.util.List;
import java.util.Map;

public class JsonTypeAdapterFactory extends AbstractTypeAdapterFactory<JsonElement> {

    public JsonTypeAdapterFactory() {
        typeTokens.add(new TypeToken<String, JsonElement>(String.class, new JsonStringTypeAdapter()));
        typeTokens.add(new TypeToken<Integer, JsonElement>(Integer.class, new JsonIntegerTypeAdapter()));
        typeTokens.add(new TypeToken<Double, JsonElement>(Double.class, new JsonDoubleTypeAdapter()));
        typeTokens.add(new TypeToken<Boolean, JsonElement>(Boolean.class, new JsonBooleanTypeAdapter()));
        typeTokens.add(new TypeToken<List<?>, JsonElement>(List.class, new JsonListTypeAdapter()));
        typeTokens.add(new TypeToken<Map<?,?>, JsonElement>(Map.class, new JsonMapTypeAdapter()));
        typeTokens.add(new TypeToken<Enum, JsonElement>(Enum.class, new JsonEnumTypeAdapter()));
        typeTokens.add(new TypeToken<Byte, JsonElement>(Byte.class, new JsonByteTypeAdapter()));
        typeTokens.add(new TypeToken<Character, JsonElement>(Character.class, new JsonCharacterTypeAdapter()));
        typeTokens.add(new TypeToken<Float, JsonElement>(Float.class, new JsonFloatTypeAdapter()));
        typeTokens.add(new TypeToken<Long, JsonElement>(Long.class, new JsonLongTypeAdapter()));
        typeTokens.add(new TypeToken<Short, JsonElement>(Short.class, new JsonShortTypeAdapter()));
    }

    @Override
    public TypeAdapter<Object, JsonElement> getTypeParameterTypeAdapter(int typeParameterIndex) {
        return new JsonTypeParameterTypeAdapter(typeParameterIndex);
    }
}
