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
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigator;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigatorImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.Visitor;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeMetadata;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.hbase.HbaseTypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ReflectionUtil;

public class HbaseDeserializationVisitor implements Visitor<HbaseCell> {

    private HbaseRow hbaseRow;
    private Object object;

    //Hide default constructor
    public HbaseDeserializationVisitor() {}

    public HbaseDeserializationVisitor(HbaseRow hbaseRow, Object object) {
        this.hbaseRow = hbaseRow;
        this.object = object;
    }

    @Override
    public Class<HbaseCell> getTargetClass() {
        return HbaseCell.class;
    }

    @Override
    public void visit(Object value, TypeMetadata<HbaseCell> typeMetadata) {
        if(typeMetadata.isJavaInternalClass()) {
            handleJavaInternal(typeMetadata);
        } else {
            handleUserDefined(typeMetadata);
        }
    }

    private void handleJavaInternal(TypeMetadata<HbaseCell> typeMetadata) {
        TypeAdapter<?, HbaseCell> typeAdapter = typeMetadata.getTypeAdapter();
        HbaseCell hbaseCell = hbaseRow.getCellForFieldName(typeMetadata.getFieldName());
        if(hbaseCell != null) {
            Object fieldValue = typeAdapter.deserialize(hbaseCell, typeMetadata);
            ReflectionUtil.setFieldValue(object, typeMetadata.getFieldName(), fieldValue);
        }
    }

    private void handleUserDefined(TypeMetadata<HbaseCell> typeMetadata) {
        TypeAdapter<?, HbaseCell> typeAdapter = typeMetadata.getTypeAdapter();
        if(typeAdapter != null) {
            HbaseCell hbaseCell = hbaseRow.getCellForFieldName(typeMetadata.getFieldName());
            Object fieldValue = typeAdapter.deserialize(hbaseCell, typeMetadata);
            ReflectionUtil.setFieldValue(object, typeMetadata.getFieldName(), fieldValue);
        } else {
            Object fieldValue = ReflectionUtil.createInstanceForField(object, typeMetadata.getFieldName());
            ObjectNavigator objectNavigator = new ObjectNavigatorImpl(new HbaseTypeAdapterFactory());
            Visitor visitor = new HbaseDeserializationVisitor(hbaseRow, fieldValue);
            objectNavigator.navigate(fieldValue, visitor);
            ReflectionUtil.setFieldValue(object, typeMetadata.getFieldName(), fieldValue);
        }
    }
}
