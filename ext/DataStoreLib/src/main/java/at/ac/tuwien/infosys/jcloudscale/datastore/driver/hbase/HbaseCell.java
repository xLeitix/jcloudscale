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

/**
 * Represents a single hbase cell
 */
public final class HbaseCell {

    private String familyName;
    private String columnName;
    private byte[] value;

    //Hide default constructor
    private HbaseCell() {}

    public HbaseCell(String familyName, String columnName, byte[] value) {
        this.familyName = familyName;
        this.columnName = columnName;
        this.value = value;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HbaseCell hbaseCell = (HbaseCell) o;

        if (!columnName.equals(hbaseCell.columnName)) return false;
        if (!familyName.equals(hbaseCell.familyName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = familyName.hashCode();
        result = 31 * result + columnName.hashCode();
        return result;
    }
}
