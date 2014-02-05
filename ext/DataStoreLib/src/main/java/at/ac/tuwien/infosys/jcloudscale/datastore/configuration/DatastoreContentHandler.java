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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Content Handler for XML Datastore Configuration
 */
public class DatastoreContentHandler implements ContentHandler {

    //Tag definitions
    public static final String TAG_DATASTORE = "datastore";
    public static final String TAG_NAME = "name";
    public static final String TAG_HOST = "host";
    public static final String TAG_PORT = "port";
    public static final String TAG_DATAUNIT = "dataunit";
    public static final String TAG_DRIVER = "driver";
    public static final String TAG_LIB = "lib";
    public static final String TAG_LIB_NAME = "lib-name";
    public static final String TAG_LIB_WRAPPER = "lib-wrapper";

    //found datastore definitions
    private List<DatastoreTemplate> datastoreTemplates = new ArrayList<DatastoreTemplate>();

    //current tag value
    private String value;

    //datastore properties
    private String name;
    private String host;
    private Integer port;
    private String dataUnit;
    private String datastoreDriverName;
    private Map<String, String> libraries = new HashMap<String, String>();
    private String libName;
    private String libWrapperClass;

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        value = new String(ch,start,length);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        //datastore tag
        if(localName.equals(TAG_DATASTORE)) {
            name = null;
            host = null;
            port = null;
            dataUnit = null;
            datastoreDriverName = null;
            libraries = new HashMap<String, String>();
        }

        //lib tag
        if(localName.equals(TAG_LIB)) {
            libName = null;
            libWrapperClass = null;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if(localName.equals(TAG_NAME)) {
            name = value;
        } else if(localName.equals(TAG_HOST)) {
            host = value;
        } else if(localName.equals(TAG_PORT)) {
            port = Integer.valueOf(value);
        } else if(localName.equals(TAG_DATAUNIT)) {
            dataUnit = value;
        } else if(localName.equals(TAG_DRIVER)) {
            datastoreDriverName = value;
        } else if(localName.equals(TAG_LIB_NAME)) {
            libName = value;
        } else if(localName.equals(TAG_LIB_WRAPPER)) {
            libWrapperClass = value;
        } else if(localName.equals(TAG_LIB)) {
            libraries.put(libName, libWrapperClass);
        } else if(localName.equals(TAG_DATASTORE)) {
            DatastoreTemplate template = new DatastoreTemplate.Builder(name, host, datastoreDriverName).port(port).dataUnit(dataUnit).addLibraries(libraries).build();
            datastoreTemplates.add(template);
        }
    }

    public List<DatastoreTemplate> getDatastoreTemplates() {
        return datastoreTemplates;
    }

    @Override
    public void setDocumentLocator(Locator locator) {}

    @Override
    public void startDocument() throws SAXException {}

    @Override
    public void endDocument() throws SAXException {}

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {}

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {}

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {}

    @Override
    public void processingInstruction(String target, String data) throws SAXException {}

    @Override
    public void skippedEntity(String name) throws SAXException {}
}
