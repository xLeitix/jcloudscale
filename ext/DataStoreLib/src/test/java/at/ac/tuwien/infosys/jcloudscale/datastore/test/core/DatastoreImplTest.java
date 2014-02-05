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

import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.DatastoreId;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreDriver;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.IdStrategy;
import at.ac.tuwien.infosys.jcloudscale.datastore.core.DatastoreImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.test.TestConstants;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DatastoreImplTest {

    private DatastoreImpl datastore;
    private DatastoreDriver datastoreDriver;

    @Before
    public void init() {
        datastoreDriver = mock(DatastoreDriver.class);
        datastore = new DatastoreImpl("test", "localhost", 8080, "test", datastoreDriver, null);
    }

    @Test
    public void save_IdAuto() {
        PersonAuto personAuto = new PersonAuto("John", "Doe");
        datastore.save(personAuto);

        verify(datastoreDriver).save(datastore, personAuto);
    }

    @Test
    public void save_IdManual() {
        String id = "SomeID";
        PersonManual personManual = new PersonManual(id, "John", "Doe");
        datastore.save(personManual);

        verify(datastoreDriver).save(datastore, id, personManual);
    }

    @Test
    public void save_Null() {
        try {
            datastore.save(null);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void save_NoID() {
        try {
            datastore.save(TestConstants.PERSON);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void save_ManualNoID() {
        PersonManual personManual = new PersonManual("John", "Doe");
        try {
            datastore.save(personManual);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void find() {
        String id = "SomeID";
        when(datastoreDriver.find(datastore, PersonManual.class, id)).thenReturn(new PersonManual());

        PersonManual personManual = datastore.find(PersonManual.class, id);
        assertEquals(id, personManual.id);
    }

    @Test
    public void find_NoClass() {
        try {
            datastore.find(null, "SomeID");
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void find_NoId() {
        try {
            datastore.find(PersonManual.class, null);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void update() {
        String id = "SomeID";
        PersonManual personManual = new PersonManual(id, "John", "Doe");
        datastore.update(personManual);

        verify(datastoreDriver).update(datastore, id, personManual);
    }

    @Test
    public void update_Null() {
        try {
            datastore.update(null);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void update_IdNull() {
        PersonManual personManual = new PersonManual("John", "Doe");
        try {
            datastore.update(personManual);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void update_NoId() {
        try {
            datastore.update(TestConstants.PERSON);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void delete() {
        String id = "SomeID";
        PersonManual personManual = new PersonManual(id, "John", "Doe");
        datastore.delete(personManual);

        verify(datastoreDriver).delete(datastore, PersonManual.class, id);
    }

    @Test
    public void delete_Null() {
        try {
            datastore.delete(null);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void delete_IdNull() {
        PersonManual personManual = new PersonManual("John", "Doe");
        try {
            datastore.delete(personManual);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void delete_NoId() {
        try {
            datastore.delete(TestConstants.PERSON);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void migrate() {
        Datastore to = mock(Datastore.class);
        DatastoreDriver toDriver = mock(DatastoreDriver.class);
        String id = "SomeID";
        PersonManual personManual = new PersonManual("John", "Doe");
        when(datastoreDriver.find(datastore, PersonManual.class, id)).thenReturn(personManual);
        when(to.getDatastoreDriver()).thenReturn(toDriver);

        datastore.migrate(to, PersonManual.class, id);
        verify(toDriver).save(to, id, personManual);
    }

    @Test
    public void migrate_DatastoreNull() {
        try {
            datastore.migrate(null, PersonManual.class, "SomeID");
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void migrate_ClassNull() {
        try {
            datastore.migrate(mock(Datastore.class), null, "SomeID");
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void migrate_IdNull() {
        try {
            datastore.migrate(mock(Datastore.class), PersonManual.class, null);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    public static class PersonAuto {
        public PersonAuto() {}
        public PersonAuto(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
        @DatastoreId(strategy = IdStrategy.AUTO)
        public String id;
        public String firstName;
        public String lastName;
    }

    public static class PersonManual {
        public PersonManual() {}
        public PersonManual(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
        public PersonManual(String id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
        }
        @DatastoreId(strategy = IdStrategy.MANUAL)
        public String id;
        public String firstName;
        public String lastName;
    }
}
