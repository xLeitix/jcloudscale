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
package at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * HBase Column Methods
 */
public abstract class HbaseColumn {

    /**
     * Adds the columns of the given row to the given descriptor
     *
     * @param row given row
     * @param descriptor descriptor to add column definitions
     */
    public static void addColumnsToDescriptor(HbaseRow row, HTableDescriptor descriptor) {
        for(HbaseCell cell : row.getCells()) {
            HColumnDescriptor columnDescriptor = new HColumnDescriptor(cell.getFamilyName().getBytes());
            descriptor.addFamily(columnDescriptor);
        }
    }

    /**
     * Creates a random ID for usage as row key
     *
     * @return unique ID
     */
    public static String createId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Create the Put statement for a given row
     *
     * @param id id of the row
     * @param row the given row
     * @return the Put Statement or the row
     */
    public static Put createColumnsForRow(String id, HbaseRow row) {
        Put put = new Put(Bytes.toBytes(id));
        put.setId(id);
        for(HbaseCell cell : row.getCells()) {
            put.add(Bytes.toBytes(cell.getFamilyName()),Bytes.toBytes(cell.getColumnName()), cell.getValue());
        }
        return put;
    }

    /**
     * Create the Put statement for a given row using a generated id
     *
     * @param row the given row
     * @return the Put Statement for the row
     */
    public static Put createColumnsForRow(HbaseRow row) {
        return createColumnsForRow(createId(), row);
    }

    /**
     * Create the mutations for the row with the given id
     *
     * @param id the id of the row
     * @param changes the changes
     * @return the mutations
     */
    public static List<Mutation> createMutations(String id, List<HbaseDiff.Change> changes) {
        List<Mutation> mutations = new ArrayList<Mutation>();
        for(HbaseDiff.Change change : changes) {
            switch(change.getOperation()) {
                case ADDED:
                case UPDATED:
                    Put put = new Put(Bytes.toBytes(id));
                    put.add(Bytes.toBytes(change.getCell().getFamilyName()), Bytes.toBytes(change.getCell().getColumnName()), change.getCell().getValue());
                    mutations.add(put);
                    break;
                case DELETED:
                    Delete  delete = new Delete(Bytes.toBytes(id));
                    delete.deleteColumn(Bytes.toBytes(change.getCell().getFamilyName()), Bytes.toBytes(change.getCell().getColumnName()));
                    mutations.add(delete);
                    break;
            }
        }
        return mutations;
    }
}
