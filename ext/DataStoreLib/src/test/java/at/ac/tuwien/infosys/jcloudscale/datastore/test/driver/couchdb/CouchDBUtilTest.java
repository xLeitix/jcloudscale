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
package at.ac.tuwien.infosys.jcloudscale.datastore.test.driver.couchdb;

import at.ac.tuwien.infosys.jcloudscale.datastore.driver.couchdb.CouchDBUtil;
import at.ac.tuwien.infosys.jcloudscale.datastore.rest.ProtocolType;
import at.ac.tuwien.infosys.jcloudscale.datastore.rest.Request;
import at.ac.tuwien.infosys.jcloudscale.datastore.rest.RequestType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CouchDBUtilTest {

    @Test
    public void testAddIdAndRevisionToContent() {
        String content = "{\"field\":\"value\"}";
        String id = "some_doc_id";
        String rev = "123";

        Request request = mock(Request.class);
        when(request.getProtocolType()).thenReturn(ProtocolType.HTTP);
        when(request.getRequestType()).thenReturn(RequestType.PUT);
        when(request.getHost()).thenReturn("localhost");
        when(request.getPort()).thenReturn(8080);
        when(request.getUrl()).thenReturn("/database/some_doc_id");
        when(request.getContentType()).thenReturn("application/json");
        when(request.getContent()).thenReturn(content);

        Request modifiedRequest = CouchDBUtil.addIdAndRevisionToContent(request, id, rev);
        assertEquals(ProtocolType.HTTP, modifiedRequest.getProtocolType());
        assertEquals(RequestType.PUT, modifiedRequest.getRequestType());
        assertEquals("localhost", modifiedRequest.getHost());
        assertEquals(Integer.valueOf(8080), modifiedRequest.getPort());
        assertEquals("/database/some_doc_id", modifiedRequest.getUrl());
        assertEquals("application/json", modifiedRequest.getContentType());
        assertEquals("{\"_rev\":\"123\",\"field\":\"value\",\"_id\":\"some_doc_id\"}", modifiedRequest.getContent());
    }
}
