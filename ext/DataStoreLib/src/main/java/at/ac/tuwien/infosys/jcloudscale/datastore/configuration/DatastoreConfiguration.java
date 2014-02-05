/*
   Copyright 2014 Philipp Leitner 

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Datastore Configuration Management
 */
public class DatastoreConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public static final String DATASTORES_FILE_NAME = "META-INF/datastores.xml";
    private Map<String, DatastoreTemplate> datastoreTemplates = new HashMap<String, DatastoreTemplate>();

    public void readFromClasspath(String file) {
        ClassLoaderService classLoaderService = new ClassLoaderServiceImpl();
        InputStream inputStream = classLoaderService.locateResourceStream(file);
        readFromStream(inputStream);
    }

    public void readFromFile(File file) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(file);
        readFromStream(inputStream);
    }

    public void readFromStream(InputStream inputStream) {
        SaxParser saxParser = new SaxParser();
        addTemplates(saxParser.readConfig(inputStream));
    }

    public DatastoreTemplate getTemplate(String name) {
        //fallback if nothing specified
        if(datastoreTemplates.isEmpty()) {
            readFromClasspath(DATASTORES_FILE_NAME);
        }
        return datastoreTemplates.get(name);
    }

    public void addTemplate(DatastoreTemplate datastoreTemplate) {
        datastoreTemplates.put(datastoreTemplate.getName(), datastoreTemplate);
    }

    public void addTemplates(List<DatastoreTemplate> templates) {
        for (DatastoreTemplate datastoreTemplate : templates) {
            datastoreTemplates.put(datastoreTemplate.getName(), datastoreTemplate);
        }
    }
}
