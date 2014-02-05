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
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreDriver;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoader;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoaderImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.Mapper;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.hbase.HbaseMapperImpl;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;

import java.util.List;

public class HbaseDriver implements DatastoreDriver {

    public static final String HBASE_PROPERTIES_FILE_NAME = "hbase.properties";

    private Mapper<HbaseRow> hbaseMapper = new HbaseMapperImpl();

    @Override
    public PropertyLoader getPropertyLoader() {
        return new PropertyLoaderImpl(HBASE_PROPERTIES_FILE_NAME);
    }

    @Override
    public String save(Datastore datastore, Object object) {
        String id = HbaseColumn.createId();
        save(datastore, id, object);
        return id;
    }

    @Override
    public void save(Datastore datastore, String id, Object object) {
        HbaseRow hbaseRow = hbaseMapper.serialize(object);
        HbaseTable.createTableForRowIfNotExists(datastore, hbaseRow);
        Put put = HbaseColumn.createColumnsForRow(id, hbaseRow);
        HbaseTable.put(datastore, hbaseRow.getTableName(), put);
    }

    @Override
    public <T> T find(Datastore datastore, Class<T> objectClass, String id) {
        Result result = HbaseTable.get(datastore, objectClass.getSimpleName(), id);
        HbaseRow hbaseRow = HbaseRow.createFromResult(objectClass.getSimpleName(), result);
        return hbaseMapper.deserialize(hbaseRow, objectClass);
    }

    @Override
    public void delete(Datastore datastore, Class<?> objectClass, String id) {
        String tableName = objectClass.getSimpleName();
        HbaseTable.deleteRow(datastore, tableName, id);
    }

    @Override
    public void update(Datastore datastore, String id, Object object) {
        Object oldObject = find(datastore, object.getClass(), id);
        HbaseRow hbaseRowOld = hbaseMapper.serialize(oldObject);
        HbaseRow hbaseRowNew = hbaseMapper.serialize(object);
        List<HbaseDiff.Change> changes = HbaseDiff.diff(hbaseRowOld, hbaseRowNew);
        List<Mutation> mutations = HbaseColumn.createMutations(id, changes);
        HbaseTable.update(datastore, object.getClass().getSimpleName(), mutations);
    }
}
