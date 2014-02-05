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

import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.ClassLoaderService;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.ClassLoaderServiceImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.SaxParser;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.DatastoreTemplate;
import junit.framework.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SaxParserTest {

    private static final String XMLFILE = "META-INF/datastores.xml";
    private SaxParser saxParser = new SaxParser();


    @Test
    public void testSimpleParserKeyValue() {
        List<DatastoreTemplate> datastoreTemplates = saxParser.readConfig(getInputStream());

        assertEquals(3, datastoreTemplates.size());
        DatastoreTemplate template = datastoreTemplates.get(0);

        assertEquals("riak", template.getName());
        assertEquals("127.0.0.1", template.getHost());
        assertTrue(8098 == template.getPort());
        assertEquals("test", template.getDataUnit());
        Assert.assertEquals("at.ac.tuwien.infosys.jcloudscale.datastore.driver.riak.RiakDriver", template.getDatastoreDriver());
    }

    @Test
    public void testSimpleParserDocument() {
        List<DatastoreTemplate> datastoreTemplates = saxParser.readConfig(getInputStream());

        assertEquals(3, datastoreTemplates.size());
        DatastoreTemplate template = datastoreTemplates.get(1);

        assertEquals("couchdb", template.getName());
        assertEquals("127.0.0.1", template.getHost());
        assertTrue(5984 == template.getPort());
        assertEquals("test", template.getDataUnit());
        assertEquals("at.ac.tuwien.infosys.jcloudscale.datastore.driver.couchdb.CouchDBDriver", template.getDatastoreDriver());
        assertEquals(1, template.getLibraries().size());
        assertTrue(template.getLibraries().containsKey("lightcouch"));
        assertEquals("at.ac.tuwien.infosys.jcloudscale.datastore.ext.LightCouchWrapper", template.getLibraries().get("lightcouch"));
    }

    @Test
    public void testSimpleColumn() {
        List<DatastoreTemplate> datastoreTemplates = saxParser.readConfig(getInputStream());

        assertEquals(3, datastoreTemplates.size());
        DatastoreTemplate template = datastoreTemplates.get(2);

        assertEquals("hbase", template.getName());
        assertEquals("192.168.56.101", template.getHost());
        assertEquals("at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase.HbaseDriver", template.getDatastoreDriver());
    }

    private InputStream getInputStream() {
        return new ClassLoaderServiceImpl().locateResourceStream(XMLFILE);
    }
}
