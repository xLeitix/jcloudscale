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
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeMetadata;
import org.apache.hadoop.hbase.util.Bytes;

public class HbaseIntegerTypeAdapter implements TypeAdapter<Integer, HbaseCell> {

    @Override
    public HbaseCell serialize(Integer object, TypeMetadata<HbaseCell> typeMetadata) {
        String className = typeMetadata.getParent().getClass().getSimpleName();
        return new HbaseCell(className, typeMetadata.getFieldName(), Bytes.toBytes(object));
    }

    @Override
    public Integer deserialize(HbaseCell element, TypeMetadata<HbaseCell> typeMetadata) {
        byte[] value = element.getValue();
        return Bytes.toInt(value);
    }
}
