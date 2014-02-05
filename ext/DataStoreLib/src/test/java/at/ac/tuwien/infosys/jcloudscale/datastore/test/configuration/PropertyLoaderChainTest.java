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

import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoaderChain;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoaderImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.test.util.TestInjector;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class PropertyLoaderChainTest {

    private PropertyLoaderChain propertyLoaderChain;
    private PropertyLoaderImpl defaultLoader;

    @Before
    public void init() {
        propertyLoaderChain = new PropertyLoaderChain();
        defaultLoader = mock(PropertyLoaderImpl.class);
        TestInjector.injectByName(propertyLoaderChain, "defaultLoader", defaultLoader);
    }

    @Test
    public void onlyDefaultProperties() {
        String propertyName = "defaultonly";
        String propertyValue = "test";

        when(defaultLoader.load(propertyName)).thenReturn(propertyValue);

        String defaultonly = propertyLoaderChain.load(propertyName);
        assertNotNull(defaultonly);
        assertEquals(propertyValue, defaultonly);
    }

    @Test
    public void onlyDefaultProperties_InvalidProperty() {
        String property = propertyLoaderChain.load("unknown");
        assertNull(property);
    }

    @Test
    public void simplePropertyLoaderChain_DefaultProperty() {
        PropertyLoaderImpl propertyLoader = mock(PropertyLoaderImpl.class);
        propertyLoaderChain.add(propertyLoader);

        String propertyName = "defaultonly";
        String propertyValue = "test";

        when(defaultLoader.load(propertyName)).thenReturn(propertyValue);

        String defaultonly = propertyLoaderChain.load(propertyName);
        assertNotNull(defaultonly);
        assertEquals(propertyValue, defaultonly);

        verify(propertyLoader, times(1)).load(propertyName);
        verify(defaultLoader, times(1)).load(propertyName);
    }

    @Test
    public void simplePropertyLoaderChain() {
        PropertyLoaderImpl propertyLoader = mock(PropertyLoaderImpl.class);
        propertyLoaderChain.add(propertyLoader);

        String propertyName = "property1";
        String propertyValue = "someValue";

        when(propertyLoader.load(propertyName)).thenReturn(propertyValue);

        String findUrl = propertyLoaderChain.load(propertyName);
        assertNotNull(findUrl);
        assertEquals(propertyValue, findUrl);

        verify(propertyLoader, times(1)).load(propertyName);
        verifyZeroInteractions(defaultLoader);
    }

    @Test
    public void loadDefault() {
        PropertyLoaderImpl propertyLoader = mock(PropertyLoaderImpl.class);
        propertyLoaderChain.add(propertyLoader);

        String propertyName = "property1";
        String propertyValue = "someValue";

        when(defaultLoader.load(propertyName)).thenReturn(propertyValue);

        String findUrl = propertyLoaderChain.loadDefault(propertyName);
        assertNotNull(findUrl);
        assertEquals(propertyValue, findUrl);

        verify(defaultLoader, times(1)).load(propertyName);
        verifyZeroInteractions(propertyLoader);
    }

    @Test
    public void loadDefault_InvalidProperty() {
        when(defaultLoader.load(anyString())).thenReturn(null);

        String findUrl = propertyLoaderChain.loadDefault("unknown");
        assertNull(findUrl);
    }
}
