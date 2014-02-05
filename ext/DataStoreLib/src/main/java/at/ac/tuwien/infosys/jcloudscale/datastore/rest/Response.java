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

import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple Http Response
 */
public class Response {

    private Map<String, String> headers;
    private String content;
    private int code;
    private String reason;

    //Hide default constructor
    private Response() {}

    public Response(HttpResponse httpResponse) {
        setHeaders(httpResponse);
        setContent(httpResponse);
        setStatus(httpResponse);
    }

    private void setHeaders(HttpResponse httpResponse) {
        headers = new HashMap<String, String>();
        for(Header header : httpResponse.getAllHeaders()) {
            headers.put(header.getName(), header.getValue());
        }
    }

    private void setContent(HttpResponse httpResponse) {
        HttpEntity httpEntity = httpResponse.getEntity();
        try {
            if(httpEntity != null && httpEntity.getContent() != null) {
                content = IOUtils.toString(httpEntity.getContent()).trim();
            }
            EntityUtils.consume(httpEntity);
        } catch (Exception e) {
            throw new DatastoreException("Error reading Response: " + e.getMessage());
        }
    }

    private void setStatus(HttpResponse httpResponse) {
        StatusLine statusLine = httpResponse.getStatusLine();
        code = statusLine.getStatusCode();
        reason = statusLine.getReasonPhrase();
    }

    /**
     * Returns the value of the first header field with the given name
     *
     * @param fieldName name of the header field
     * @return value of the header field
     */
    public String getHeaderField(String fieldName) {
        return headers.get(fieldName);
    }

    /**
     * Returns the response content as string
     *
     * @return the response content
     */
    public String getContent() {
        return content;
    }

    /**
     * Verify the response (throws Exception when Response Code not 200)
     */
    public void verify() {
        if(code >= 400) {
            throw new DatastoreException("Error handling response: " + reason);
        }
    }
}
