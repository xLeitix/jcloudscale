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
package at.ac.tuwien.infosys.jcloudscale.datastore.test.core;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.core.DatastoreFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.test.util.TestInjector;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatastoreFactoryTest {

    private DatastoreFactory datastoreFactory;

    @Before
    public void init() {
        datastoreFactory = DatastoreFactory.getInstance();
        mockDatastoreList();
    }

    @Test
    public void getDatastoreByName() {
        Datastore datastore = datastoreFactory.getDatastoreByName("riak");
        assertNotNull(datastore);
        assertEquals("riak", datastore.getName());
    }

    @Test
    public void getDatastoreByName_NotFound() {
        Datastore datastore = datastoreFactory.getDatastoreByName("unknown");
        assertNull(datastore);
    }

    private void mockDatastoreList() {
        Datastore couchdb = mock(Datastore.class);
        Datastore riak = mock(Datastore.class);
        Datastore hbase = mock(Datastore.class);

        when(couchdb.getName()).thenReturn("couchdb");
        when(riak.getName()).thenReturn("riak");
        when(hbase.getName()).thenReturn("hbase");

        List<Datastore> datastoreList = new ArrayList<Datastore>();
        datastoreList.addAll(Arrays.asList(couchdb, riak, hbase));
        TestInjector.injectByName(datastoreFactory, "datastores", datastoreList);
    }
}
