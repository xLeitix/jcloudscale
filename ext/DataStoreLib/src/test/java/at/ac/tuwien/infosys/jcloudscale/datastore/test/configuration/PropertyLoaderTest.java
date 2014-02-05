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
package at.ac.tuwien.infosys.jcloudscale.datastore.test.configuration;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.DatastoreProperties;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoaderImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.test.util.TestInjector;

import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PropertyLoaderTest {

    private PropertyLoaderImpl propertyLoader;
    private Properties properties;

    @Before
    public void init() {
        propertyLoader = new PropertyLoaderImpl("default.properties");
        properties = mock(Properties.class);
        TestInjector.injectByName(propertyLoader, "properties", properties);
    }

    @Test
    public void load() {
        String propertyName = "property1";
        String propertyValue = "someValue";

        when(properties.getProperty(propertyName)).thenReturn(propertyValue);

        String saveUrl = propertyLoader.load(propertyName);
        assertNotNull(saveUrl);
        assertEquals(propertyValue, saveUrl);
    }

    @Test
    public void loadUnknownProperty() {
        when(properties.getProperty(anyString())).thenReturn(null);

        String property = propertyLoader.load("unknown");
        assertNull(property);
    }

    @Test(expected = DatastoreException.class)
    public void invalidPropertyFile() {
        PropertyLoaderImpl propertyLoader1 = new PropertyLoaderImpl("invalid");
        propertyLoader1.load(DatastoreProperties.URL_SAVE);
    }
}
