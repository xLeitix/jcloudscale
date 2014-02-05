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

import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.List;

/**
 * HBase Table Methods
 */
public abstract class HbaseTable {

    /**
     * Get the HBase Table for a given datastore
     *
     * @param datastore the given datastore
     * @return the HBase Table
     */
    public static HTable getTable(Datastore datastore) {
        Configuration configuration = HbaseConfig.getConfig(datastore);
        try {
            HTable hTable = new HTable(configuration, datastore.getDataUnit());
            return hTable;
        } catch (IOException e) {
            throw new DatastoreException("Error getting HBase table " + datastore.getDataUnit());
        }
    }

    /**
     * Get the HBase Table with the given name
     *
     * @param datastore the datastore
     * @param tableName the name of the table
     * @return the HBase Table
     */
    public static HTable getTable(Datastore datastore, String tableName) {
        Configuration configuration = HbaseConfig.getConfig(datastore);
        try {
            HTable hTable = new HTable(configuration, tableName);
            return hTable;
        } catch (IOException e) {
            throw new DatastoreException("Error getting HBase table " + tableName);
        }
    }

    /**
     * Check if a table with the given name exists in the given datastore
     *
     * @param  datastore the given datastore
     * @param tableName the name of the table
     * @return true if exists, false otherwise
     */
    public static boolean tableExists(Datastore datastore, String tableName) {
        Configuration configuration = HbaseConfig.getConfig(datastore);
        try {
            HBaseAdmin hBaseAdmin = new HBaseAdmin(configuration);
            return hBaseAdmin.tableExists(tableName);
        } catch (MasterNotRunningException e) {
            throw new DatastoreException("Error checking table: " + e.getMessage());
        } catch (ZooKeeperConnectionException e) {
            throw new DatastoreException("Error checking table: " + e.getMessage());
        } catch (IOException e) {
            throw new DatastoreException("Error checking table: " + e.getMessage());
        }
    }

    /**
     * Create a table in the given datastore
     *
     * @param datastore the given datastore
     * @param tableDescriptor the descriptor of the table to create
     */
    public static void createTable(Datastore datastore, HTableDescriptor tableDescriptor) {
        Configuration configuration = HbaseConfig.getConfig(datastore);
        try {
            HBaseAdmin hBaseAdmin = new HBaseAdmin(configuration);
            hBaseAdmin.createTable(tableDescriptor);
        } catch (MasterNotRunningException e) {
            throw new DatastoreException("Error creating table: " + e.getMessage());
        } catch (ZooKeeperConnectionException e) {
            throw new DatastoreException("Error creating table: " + e.getMessage());
        } catch (IOException e) {
            throw new DatastoreException("Error creating table: " + e.getMessage());
        }
    }

    /**
     * Create a table for the given row in the given datastore if not exists
     *
     * @param datastore given datastore
     * @param hbaseRow given row
     */
    public static void createTableForRowIfNotExists(Datastore datastore, HbaseRow hbaseRow) {
        if(tableExists(datastore, hbaseRow.getTableName())){
           return;
        }
        String tableName = hbaseRow.getTableName();
        HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
        HbaseColumn.addColumnsToDescriptor(hbaseRow, tableDescriptor);
        createTable(datastore, tableDescriptor);
    }

    /**
     * Executes put statement for the given table and datastore
     *
     * @param datastore given datastore
     * @param tableName given table name
     * @param put the put statement
     */
    public static void put(Datastore datastore, String tableName, Put put) {
        HTable hTable = getTable(datastore, tableName);
        try {
            hTable.put(put);
        } catch (IOException e) {
            throw new DatastoreException("Error adding data: " + e.getMessage());
        }
    }

    /**
     * Deletes the row with the given id in the given datastore
     *
     * @param datastore given datastore
     * @param tableName name of the table with the row
     * @param id id of the row to delete
     */
    public static void deleteRow(Datastore datastore, String tableName, String id) {
        HTable hTable = getTable(datastore, tableName);
        Delete delete = new Delete(id.getBytes());
        try {
            hTable.delete(delete);
        } catch (IOException e) {
            throw new DatastoreException("Error deleting row with id " + id + ": " + e.getMessage());
        }
    }

    /**
     * Get the row result for the row with the given id
     *
     * @param datastore given datastore
     * @param tableName name of the table with the row
     * @param id the id ot the row to get
     * @return the result representing the row
     */
    public static Result get(Datastore datastore, String tableName, String id) {
        HTable hTable = getTable(datastore, tableName);
        Get get = new Get(Bytes.toBytes(id));
        try {
            return hTable.get(get);
        } catch (IOException e) {
            throw new DatastoreException("Error getting row with id " + id + " from table " + tableName + ".");
        }
    }

    public static void update(Datastore datastore, String tableName, List<Mutation> mutations) {
        HTable hTable = getTable(datastore, tableName);
        for (Mutation mutation : mutations) {
            try {
                if(mutation instanceof Put) {
                    hTable.put((Put) mutation);
                }

                if(mutation instanceof Delete) {
                    hTable.delete((Delete) mutation);
                }
            } catch (IOException e) {
                throw new DatastoreException("Error updating row from table " + tableName + ".");
            }
        }
    }
}
