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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hbase diff methods
 */
public abstract class HbaseDiff {

    public static List<Change> diff(HbaseRow oldRow, HbaseRow newRow) {
        List<Change> changes = new ArrayList<Change>();
        for(HbaseCell cell : newRow.getCells()) {
            if(oldRow.getCells().contains(cell)) {
                int index = oldRow.getCells().indexOf(cell);
                if(!Arrays.equals(oldRow.getCells().get(index).getValue(), cell.getValue())) {
                    Change change = new Change(cell, Operation.UPDATED);
                    changes.add(change);
                }
            } else {
                Change change = new Change(cell, Operation.ADDED);
                changes.add(change);
            }
        }
        for(HbaseCell cell : oldRow.getCells()) {
            if(!newRow.getCells().contains(cell)) {
                Change change = new Change(cell, Operation.DELETED);
                changes.add(change);
            }
        }
        return changes;
    }

    /**
     * Cell Operations
     */
    public enum Operation {
        ADDED,UPDATED, DELETED
    }

    /**
     * Class for representing cell changes
     */
    public static class Change {

        private HbaseCell cell;
        private Operation operation;

        public Change(HbaseCell cell, Operation operation) {
            this.cell = cell;
            this.operation = operation;
        }

        public HbaseCell getCell() {
            return cell;
        }

        public void setCell(HbaseCell cell) {
            this.cell = cell;
        }

        public Operation getOperation() {
            return operation;
        }

        public void setOperation(Operation operation) {
            this.operation = operation;
        }
    }

}
