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
package at.ac.tuwien.infosys.jcloudscale.datastore.rest;

import java.util.HashMap;
import java.util.Map;

/**
 * Single HTTP Request
 */
public class Request {

    private final ProtocolType protocolType;
    private final RequestType requestType;
    private final String url;
    private final String host;
    private final Integer port;
    private final String contentType;
    private final Object content;
    private final Map<String, String> headerFields;

    public static class Builder {
        //required params
        private final ProtocolType protocolType;
        private final RequestType requestType;
        private final String url;
        private final String host;
        private final Integer port;

        //optional params
        private String contentType = null;
        private Object content = null;
        private Map<String, String> headerFields = new HashMap<String, String>();

        public Builder(ProtocolType protocolType, RequestType requestType, String url, String host, Integer port) {
            this.protocolType = protocolType;
            this.requestType = requestType;
            this.url = url;
            this.host = host;
            this.port = port;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder content(Object content) {
            this.content = content;
            return this;
        }

        public Builder addHeader(String name, String value) {
            this.headerFields.put(name, value);
            return this;
        }

        public Request build() {
            return new Request(this);
        }
    }

    private Request(Builder builder) {
        this.protocolType = builder.protocolType;
        this.requestType = builder.requestType;
        this.url = builder.url;
        this.host = builder.host;
        this.port = builder.port;
        this.contentType = builder.contentType;
        this.content = builder.content;
        this.headerFields = builder.headerFields;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public String getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getContentType() {
        return contentType;
    }

    public Object getContent() {
        return content;
    }

    public Map<String, String> getHeaderFields() {
        return headerFields;
    }
}
