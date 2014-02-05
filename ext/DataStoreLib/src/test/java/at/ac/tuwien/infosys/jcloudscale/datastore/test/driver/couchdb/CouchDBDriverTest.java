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

import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.DatastoreProperties;
import at.ac.tuwien.infosys.jcloudscale.datastore.driver.couchdb.CouchDBDriver;
import at.ac.tuwien.infosys.jcloudscale.datastore.driver.couchdb.CouchDBResponse;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.Mapper;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.json.JsonMapperImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.rest.*;
import at.ac.tuwien.infosys.jcloudscale.datastore.test.TestConstants;
import at.ac.tuwien.infosys.jcloudscale.datastore.test.util.TestInjector;

import org.apache.http.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class CouchDBDriverTest {

    public static final String HOST = "localhost";
    public static final Integer PORT = 8080;
    public static final String DATAUNIT = "database";

    private CouchDBDriver driver;
    private RequestHandler requestHandler;
    private Mapper<String> mapper;
    private Datastore datastore;
    private Response response;


    @Before
    public void init() {
        driver = new CouchDBDriver();
        requestHandler = mock(RequestHandler.class);
        mapper = mock(JsonMapperImpl.class);
        datastore = mock(Datastore.class);
        response = mock(Response.class);
        TestInjector.injectByName(driver, "requestHandler", requestHandler);
        TestInjector.injectByName(driver, "jsonMapper", mapper);

        when(datastore.getHost()).thenReturn(HOST);
        when(datastore.getPort()).thenReturn(PORT);
        when(datastore.getProperty(DatastoreProperties.URL_SAVE)).thenReturn("/{0}/");
        when(datastore.getProperty(DatastoreProperties.URL_SAVE_ID)).thenReturn("/{0}/{1}");
        when(datastore.getProperty(DatastoreProperties.URL_FIND)).thenReturn("/{0}/{1}");
        when(datastore.getProperty(DatastoreProperties.URL_COUCHDB_CURRENT_REVISION)).thenReturn("/{0}/{1}");
        when(datastore.getProperty(DatastoreProperties.URL_DELETE)).thenReturn("/{0}/{1}?rev={2}");
        when(datastore.getProperty(DatastoreProperties.URL_UPDATE)).thenReturn("/{0}/{1}");
        when(datastore.getDataUnit()).thenReturn(DATAUNIT);
    }

    @Test
    public void testSaveObject() {
        when(mapper.serialize(TestConstants.PERSON)).thenReturn(TestConstants.PERSON_JSON);
        when(requestHandler.handle(any(Request.class))).thenReturn(response);
        String jsonResponse = "{\"ok\":true, \"id\":\"123BAC\", \"rev\":\"946B7D1C\"}";
        when(response.getContent()).thenReturn(jsonResponse);
        CouchDBResponse couchDBResponse = mock(CouchDBResponse.class);
        when(couchDBResponse.getId()).thenReturn("123BAC");
        when(mapper.deserialize(eq(jsonResponse), eq(CouchDBResponse.class))).thenReturn(couchDBResponse);


        String id = driver.save(datastore, TestConstants.PERSON);
        assertEquals("123BAC", id);

        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(requestHandler).handle(argumentCaptor.capture());
        assertEquals(ProtocolType.HTTP, argumentCaptor.getValue().getProtocolType());
        assertEquals(RequestType.POST, argumentCaptor.getValue().getRequestType());
        assertEquals("/" + DATAUNIT + "/", argumentCaptor.getValue().getUrl());
        assertEquals(HOST, argumentCaptor.getValue().getHost());
        assertEquals(PORT, argumentCaptor.getValue().getPort());
        assertEquals("application/json", argumentCaptor.getValue().getContentType());
        assertEquals(TestConstants.PERSON_JSON, argumentCaptor.getValue().getContent());
        assertTrue(argumentCaptor.getValue().getHeaderFields().isEmpty());
    }

    @Test
    public void testSaveObjectCustomId() {
        when(mapper.serialize(TestConstants.PERSON)).thenReturn(TestConstants.PERSON_JSON);
        when(requestHandler.handle(any(Request.class))).thenReturn(response);
        String jsonResponse = "{\"ok\": true, \"id\": \"some_doc_id\", \"rev\": \"946B7D1C\"}";
        when(response.getContent()).thenReturn(jsonResponse);
        CouchDBResponse couchDBResponse = mock(CouchDBResponse.class);
        when(couchDBResponse.getId()).thenReturn("some_doc_id");
        when(mapper.deserialize(eq(jsonResponse), eq(CouchDBResponse.class))).thenReturn(couchDBResponse);

        driver.save(datastore, "some_doc_id", TestConstants.PERSON);

        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(requestHandler).handle(argumentCaptor.capture());
        assertEquals(ProtocolType.HTTP, argumentCaptor.getValue().getProtocolType());
        assertEquals(RequestType.PUT, argumentCaptor.getValue().getRequestType());
        assertEquals("/" + DATAUNIT + "/some_doc_id", argumentCaptor.getValue().getUrl());
        assertEquals(HOST, argumentCaptor.getValue().getHost());
        assertEquals(PORT, argumentCaptor.getValue().getPort());
        assertEquals("application/json", argumentCaptor.getValue().getContentType());
        assertEquals(TestConstants.PERSON_JSON, argumentCaptor.getValue().getContent());
        assertTrue(argumentCaptor.getValue().getHeaderFields().isEmpty());
    }

    @Test
    public void testFindObject() {
        when(requestHandler.handle(any(Request.class))).thenReturn(response);
        when(response.getContent()).thenReturn(TestConstants.PERSON_JSON);
        when(mapper.deserialize(TestConstants.PERSON_JSON, TestConstants.Person.class)).thenReturn(TestConstants.PERSON);

        TestConstants.Person person = driver.find(datastore, TestConstants.Person.class, "some_doc_id");
        assertEquals(TestConstants.PERSON, person);

        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(requestHandler).handle(argumentCaptor.capture());
        assertEquals(ProtocolType.HTTP, argumentCaptor.getValue().getProtocolType());
        assertEquals(RequestType.GET, argumentCaptor.getValue().getRequestType());
        assertEquals("/" + DATAUNIT + "/some_doc_id", argumentCaptor.getValue().getUrl());
        assertEquals(HOST, argumentCaptor.getValue().getHost());
        assertEquals(PORT, argumentCaptor.getValue().getPort());
        assertNull(argumentCaptor.getValue().getContentType());
        assertNull(argumentCaptor.getValue().getContent());
        assertTrue(argumentCaptor.getValue().getHeaderFields().isEmpty());
    }

    @Test
    public void testDeleteObject() {
        Response headResponse = mock(Response.class);

        when(requestHandler.handle(any(Request.class))).thenReturn(headResponse).thenReturn(response);
        when(headResponse.getHeaderField("ETag")).thenReturn("123");
        when(response.getContent()).thenReturn("{\"ok\":true,\"rev\":\"2839830636\"}");

        driver.delete(datastore, TestConstants.Person.class, "some_doc_id");

        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(requestHandler,times(2)).handle(argumentCaptor.capture());

        Request request = argumentCaptor.getAllValues().get(0);
        assertEquals(ProtocolType.HTTP, request.getProtocolType());
        assertEquals(RequestType.HEAD, request.getRequestType());
        assertEquals("/" + DATAUNIT + "/some_doc_id", request.getUrl());
        assertEquals(HOST, request.getHost());
        assertEquals(PORT, request.getPort());
        assertNull(request.getContentType());
        assertNull(request.getContent());
        assertTrue(request.getHeaderFields().isEmpty());

        request = argumentCaptor.getAllValues().get(1);
        assertEquals(ProtocolType.HTTP, request.getProtocolType());
        assertEquals(RequestType.DELETE, request.getRequestType());
        assertEquals("/" + DATAUNIT + "/some_doc_id?rev=123", request.getUrl());
        assertEquals(HOST, request.getHost());
        assertEquals(PORT, request.getPort());
        assertNull(request.getContentType());
        assertNull(request.getContent());
        assertTrue(request.getHeaderFields().isEmpty());
    }

    @Test
    public void testUpdateObject() {
        Response headResponse = mock(Response.class);

        when(requestHandler.handle(any(Request.class))).thenReturn(headResponse).thenReturn(response);
        when(headResponse.getHeaderField("ETag")).thenReturn("123");
        when(response.getContent()).thenReturn("{\"ok\": true, \"id\": \"some_doc_id\", \"rev\": \"946B7D1C\"}");
        when(mapper.serialize(TestConstants.PERSON)).thenReturn(TestConstants.PERSON_JSON);

        driver.update(datastore, "some_doc_id", TestConstants.PERSON);

        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(requestHandler,times(2)).handle(argumentCaptor.capture());

        Request request = argumentCaptor.getAllValues().get(0);
        assertEquals(ProtocolType.HTTP, request.getProtocolType());
        assertEquals(RequestType.HEAD, request.getRequestType());
        assertEquals("/" + DATAUNIT + "/some_doc_id", request.getUrl());
        assertEquals(HOST, request.getHost());
        assertEquals(PORT, request.getPort());
        assertNull(request.getContentType());
        assertNull(request.getContent());
        assertTrue(request.getHeaderFields().isEmpty());

        request = argumentCaptor.getAllValues().get(1);
        assertEquals(ProtocolType.HTTP, request.getProtocolType());
        assertEquals(RequestType.PUT, request.getRequestType());
        assertEquals("/" + DATAUNIT + "/some_doc_id", request.getUrl());
        assertEquals(HOST, request.getHost());
        assertEquals(PORT, request.getPort());
        assertEquals("application/json", request.getContentType());
        assertTrue(request.getHeaderFields().isEmpty());

        String content = (String) request.getContent();
        assertTrue(content.contains("\"_rev\":\"123\""));
        assertTrue(content.contains("\"_id\":\"some_doc_id\""));
    }

    @Test
    public void testSaveObjectWithError() {
        HttpResponse httpResponse = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_CONFLICT);
        when(statusLine.getReasonPhrase()).thenReturn("Conflict occurred.");
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(httpResponse.getEntity()).thenReturn(mock(HttpEntity.class));

        Response response = new Response(httpResponse);

        when(mapper.serialize(TestConstants.PERSON)).thenReturn(TestConstants.PERSON_JSON);
        when(requestHandler.handle(any(Request.class))).thenReturn(response);

        try {
            driver.save(datastore, TestConstants.PERSON);
            fail("Exception expected!");
        } catch (DatastoreException e) {
            assertEquals("Error handling response: Conflict occurred.", e.getMessage());
        }
    }
}
