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


import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;



public class SaxParser {

    private Logger log;

    public SaxParser() {
        this.log = JCloudScaleConfiguration.getLogger(this);
    }

    public List<DatastoreTemplate> readConfig(InputStream inputStream) {

        DatastoreContentHandler contentHandler = new DatastoreContentHandler();

        //validate configuration
        //validateConfiguration(configFileName);

        try {

            XMLReader xmlReader = XMLReaderFactory.createXMLReader();

            InputSource inputSource = new InputSource(inputStream);
            xmlReader.setContentHandler(contentHandler);
            xmlReader.parse(inputSource);

            inputStream.close();

            return contentHandler.getDatastoreTemplates();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    private void validateConfiguration(InputStream xmlStream) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream schemaStream = classLoader.getResourceAsStream("META-INF/datastores.xsd");

        Source schemaSource = new StreamSource(schemaStream);
        Source xmlSource = new StreamSource(xmlStream);

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = null;
        try {
            schema = schemaFactory.newSchema(schemaSource);
        } catch (SAXException e) {
            throw new DatastoreException("Error validating XML configuration: " + e.getMessage());
        }
        Validator validator = schema.newValidator();
        try {
            validator.validate(xmlSource);
            schemaStream.close();
            xmlStream.close();
        } catch (SAXException e) {
            throw new DatastoreException("Invalid XML configuration: " + e.getMessage());
        } catch (IOException e) {
            throw new DatastoreException("Error reading XML configuration: " + e.getMessage());
        }
    }
}
