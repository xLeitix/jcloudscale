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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.HashMap;
import java.util.Map;

/**
 * Template Class for building datastore objects
 */
public class DatastoreTemplate {

    private final String name;
    private final String host;
    private final Integer port;
    private final String dataUnit;
    private final String datastoreDriver;
    private final Map<String, String> libraries;

    public static class Builder {
        //required params
        private final String name;
        private final String host;
        private final String datastoreDriver;

        //optional params
        private Integer port = null;
        private String dataUnit = null;
        private Map<String, String> libraries = new HashMap<String, String>();

        public Builder(String name, String host, String datastoreDriver) {
            this.name = name;
            this.host = host;
            this.datastoreDriver = datastoreDriver;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder dataUnit(String dataUnit) {
            this.dataUnit = dataUnit;
            return this;
        }

        public Builder addLibrary(String name, String wrapperClass) {
            this.libraries.put(name, wrapperClass);
            return this;
        }

        public Builder addLibraries(Map<String, String> libraries) {
            this.libraries.putAll(libraries);
            return this;
        }

        public DatastoreTemplate build() {
            return new DatastoreTemplate(this);
        }
    }


    private DatastoreTemplate(Builder builder) {
        this.name = builder.name;
        this.host = builder.host;
        this.port = builder.port;
        this.dataUnit = builder.dataUnit;
        this.datastoreDriver = builder.datastoreDriver;
        this.libraries = builder.libraries;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getDataUnit() {
        return dataUnit;
    }

    public String getDatastoreDriver() {
        return datastoreDriver;
    }

    public Map<String, String> getLibraries() {
        return libraries;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("name", name)
                .append("host", host)
                .append("port", port)
                .append("dataUnit", dataUnit)
                .append("datastoreDriver", datastoreDriver)
                .append("libraries", libraries).toString();
    }
}
