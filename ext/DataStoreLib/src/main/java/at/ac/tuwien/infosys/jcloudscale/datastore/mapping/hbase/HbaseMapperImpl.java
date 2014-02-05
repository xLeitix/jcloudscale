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
package at.ac.tuwien.infosys.jcloudscale.datastore.mapping.hbase;

import at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase.HbaseCell;
import at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase.HbaseRow;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.Mapper;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigator;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigatorImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.Visitor;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.hbase.HbaseTypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ReflectionUtil;

public class HbaseMapperImpl implements Mapper<HbaseRow> {

    private TypeAdapterFactory<HbaseCell> typeAdapterFactory;

    public HbaseMapperImpl() {
        this.typeAdapterFactory = new HbaseTypeAdapterFactory();
    }

    @Override
    public HbaseRow serialize(Object object) {
        HbaseRow hbaseRow = new HbaseRow(getTableNameForObject(object));
        ObjectNavigator objectNavigator = new ObjectNavigatorImpl(typeAdapterFactory);
        Visitor vistor = new HbaseSerializationVisitor(hbaseRow);
        objectNavigator.navigate(object, vistor);
        return hbaseRow;
    }

    @Override
    public <K> K deserialize(HbaseRow serializedObject, Class<K> targetClass) {
        K object = ReflectionUtil.createInstance(targetClass);
        ObjectNavigator objectNavigator = new ObjectNavigatorImpl(typeAdapterFactory);
        Visitor visitor = new HbaseDeserializationVisitor(serializedObject, object);
        objectNavigator.navigate(object, visitor);
        return object;
    }

    private String getTableNameForObject(Object object) {
        return object.getClass().getSimpleName();
    }
}
