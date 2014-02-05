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
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.AbstractTypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeToken;

import java.util.List;
import java.util.Map;

public class HbaseTypeAdapterFactory extends AbstractTypeAdapterFactory<HbaseCell> {

    public HbaseTypeAdapterFactory() {
        typeTokens.add(new TypeToken<String, HbaseCell>(String.class, new HbaseStringTypeAdapter()));
        typeTokens.add(new TypeToken<Integer, HbaseCell>(Integer.class, new HbaseIntegerTypeAdapter()));
        typeTokens.add(new TypeToken<Double, HbaseCell>(Double.class, new HbaseDoubleTypeAdapter()));
        typeTokens.add(new TypeToken<Boolean, HbaseCell>(Boolean.class, new HbaseBooleanTypeAdapter()));
        typeTokens.add(new TypeToken<List<?>, HbaseCell>(List.class, new HbaseListTypeAdapter()));
        typeTokens.add(new TypeToken<Map<?, ?>, HbaseCell>(Map.class, new HbaseMapTypeAdapter()));
        typeTokens.add(new TypeToken<Enum, HbaseCell>(Enum.class, new HbaseEnumTypeAdapter()));
        typeTokens.add(new TypeToken<Byte, HbaseCell>(Byte.class, new HbaseByteTypeAdapter()));
        typeTokens.add(new TypeToken<Character, HbaseCell>(Character.class, new HbaseCharacterTypeAdapter()));
        typeTokens.add(new TypeToken<Float, HbaseCell>(Float.class, new HbaseFloatTypeAdapter()));
        typeTokens.add(new TypeToken<Long, HbaseCell>(Long.class, new HbaseLongTypeAdapter()));
        typeTokens.add(new TypeToken<Short, HbaseCell>(Short.class, new HbaseShortTypeAdapter()));
    }

    @Override
    public TypeAdapter<Object, HbaseCell> getTypeParameterTypeAdapter(int typeParameterIndex) {
        return new HbaseTypeParameterTypeAdapter(typeParameterIndex);
    }
}
