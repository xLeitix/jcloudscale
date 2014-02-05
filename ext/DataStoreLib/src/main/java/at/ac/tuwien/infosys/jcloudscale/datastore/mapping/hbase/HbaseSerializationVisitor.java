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

import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase.HbaseCell;
import at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase.HbaseRow;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigator;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.ObjectNavigatorImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.Visitor;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeMetadata;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.hbase.HbaseTypeAdapterFactory;

public class HbaseSerializationVisitor implements Visitor<HbaseCell> {

    private HbaseRow hbaseRow;

    //Hide default constructor
    private HbaseSerializationVisitor(){}

    public HbaseSerializationVisitor(HbaseRow hbaseRow) {
        this.hbaseRow = hbaseRow;
    }

    @Override
    public Class<HbaseCell> getTargetClass() {
        return HbaseCell.class;
    }

    @Override
    public void visit(Object value, TypeMetadata<HbaseCell> typeMetadata) {
        if(value != null) {
            if(typeMetadata.isJavaInternalClass()) {
                handleJavaInternal(value, typeMetadata);
            } else {
                handleUserDefined(value, typeMetadata);
            }
        }
    }

    private void handleJavaInternal(Object value, TypeMetadata<HbaseCell> typeMetadata) {
        TypeAdapter typeAdapter = typeMetadata.getTypeAdapter();
        if(typeAdapter == null) {
            throw new DatastoreException("Not type adapter found for type: " + typeMetadata.getFieldType());
        }
        HbaseCell hbaseCell = (HbaseCell) typeAdapter.serialize(value, typeMetadata);
        hbaseRow.getCells().add(hbaseCell);
    }

    private void handleUserDefined(Object value, TypeMetadata<HbaseCell> typeMetadata) {
        TypeAdapter typeAdapter = typeMetadata.getTypeAdapter();
        if(typeAdapter != null) {
            HbaseCell hbaseCell = (HbaseCell) typeAdapter.serialize(value, typeMetadata);
            hbaseRow.getCells().add(hbaseCell);
        } else {
            ObjectNavigator objectNavigator = new ObjectNavigatorImpl(new HbaseTypeAdapterFactory());
            Visitor visitor = new HbaseSerializationVisitor(hbaseRow);
            objectNavigator.navigate(value, visitor);
        }
    }
}
