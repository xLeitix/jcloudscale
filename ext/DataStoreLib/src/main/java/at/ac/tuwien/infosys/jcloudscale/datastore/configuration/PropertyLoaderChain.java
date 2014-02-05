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
package at.ac.tuwien.infosys.jcloudscale.datastore.configuration;


import java.util.ArrayList;
import java.util.List;

/**
 * Property Loader Chain
 *
 * Property will be loaded from first file containing the property
 * otherwise default property is loaded
 */
public class PropertyLoaderChain {

    private static PropertyLoader defaultLoader;

    private List<PropertyLoader> propertyLoaders;

    static {
        defaultLoader = new PropertyLoaderImpl(DatastoreProperties.DEFAULT_PROPERTIES_FILE);
    }

    public PropertyLoaderChain() {
        propertyLoaders = new ArrayList<PropertyLoader>();
    }

    public PropertyLoaderChain(PropertyLoader propertyLoader) {
        propertyLoaders = new ArrayList<PropertyLoader>();
        propertyLoaders.add(propertyLoader);
    }

    public void add(PropertyLoader propertyLoader) {
        propertyLoaders.add(propertyLoader);
    }

    public String load(String propertyName) {
        for(PropertyLoader propertyLoader : propertyLoaders) {
            String property = propertyLoader.load(propertyName);
            if(property != null) {
                return property;
            }
        }
        return loadDefault(propertyName);
    }

    public String loadDefault(String propertyName) {
        return defaultLoader.load(propertyName);
    }
}
