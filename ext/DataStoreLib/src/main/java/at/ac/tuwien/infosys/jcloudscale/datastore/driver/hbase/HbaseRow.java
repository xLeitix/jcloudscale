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

import org.apache.hadoop.hbase.client.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

/**
 * Represents a single hbase row
 */
public final class HbaseRow {

    private String tableName;
    private List<HbaseCell> cells;

    //Hide default constructor
    private HbaseRow() {}

    public HbaseRow(String tableName) {
        this.tableName = tableName;
        this.cells = new ArrayList<HbaseCell>();
    }

    public List<HbaseCell> getCells() {
        return cells;
    }

    public String getTableName() {
        return tableName;
    }

    /**
     * Get the hbase cell for the field with the given name
     *
     * @param fieldName the name of the field
     * @return the corresponding hbase cell
     */
    public HbaseCell getCellForFieldName(String fieldName) {
        for(HbaseCell cell : cells) {
            if(cell.getColumnName().equals(fieldName)) {
                return cell;
            }
        }
        return null;
    }

    /**
     * Create a new hbase row from the given result
     *
     * @param tableName the table name
     * @param result the given result
     * @return the hbase row
     */
    public static HbaseRow createFromResult(String tableName, Result result) {
        HbaseRow hbaseRow = new HbaseRow(tableName);
        NavigableMap<byte[], NavigableMap<byte[], byte[]>> resultMap = result.getNoVersionMap();
        for (byte[] familyName : resultMap.keySet()) {
            NavigableMap<byte[], byte[]> subResultMap = resultMap.get(familyName);
            for(byte[] columnName : subResultMap.keySet()) {
                HbaseCell hbaseCell = new HbaseCell(new String(familyName), new String(columnName), subResultMap.get(columnName));
                hbaseRow.getCells().add(hbaseCell);
            }
        }
        return hbaseRow;
    }
}
