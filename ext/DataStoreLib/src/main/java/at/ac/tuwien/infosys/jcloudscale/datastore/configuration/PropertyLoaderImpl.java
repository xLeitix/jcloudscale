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

import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;

import java.io.InputStream;
import java.util.Properties;

/**
 * Property Loader to read .properties files
 */
public class PropertyLoaderImpl implements PropertyLoader {

    private Properties properties = null;
    private String propertyFile;

    public PropertyLoaderImpl(String propertyFile) {
        this.propertyFile = propertyFile;
    }

    public String load(String propertyName) {
        if(properties == null) {
            init();
        }
        return properties.getProperty(propertyName);
    }

    private void init() {
        properties = new Properties();
        try {
            InputStream propertyFileStream = this.getClass().getClassLoader().getResourceAsStream(propertyFile);
            properties.load(propertyFileStream);
        } catch (Exception e) {
            throw new DatastoreException("Error reading property file " + propertyFile);
        }
    }
}
