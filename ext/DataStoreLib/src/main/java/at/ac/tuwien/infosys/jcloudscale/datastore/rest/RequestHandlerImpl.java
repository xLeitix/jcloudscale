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

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.FileUtil;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.URLUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

public class RequestHandlerImpl implements RequestHandler {

    private Logger log;
    private HttpClient httpClient = new DefaultHttpClient();
    private static final String CONTENT_TYPE_FIELD = "Content-Type";

    public RequestHandlerImpl() {
        this.log = JCloudScaleConfiguration.getLogger(this);
    }

    @Override
    public Response handle(Request request) throws DatastoreException {
        try {
            HttpUriRequest httpUriRequest = buildRequest(request);

            setHeaders(httpUriRequest, request);
            setContent(httpUriRequest, request);

            HttpResponse httpResponse = httpClient.execute(httpUriRequest);
            return new Response(httpResponse);
        } catch (Exception e) {
            throw new DatastoreException(e.getMessage());
        }

    }

    private void setContent(HttpUriRequest httpUriRequest, Request request) throws UnsupportedEncodingException {
        if (request.getContent() == null || !vaildRequestTypeForContent(request)) {
            return;
        }
        HttpEntityEnclosingRequestBase entityRequest = (HttpEntityEnclosingRequestBase) httpUriRequest;
        HttpEntity httpEntity = null;
        if(FileUtil.isFile(request.getContent().getClass())) {
            File file = (File) request.getContent();
            httpEntity = new FileEntity(file, request.getContentType());
        } else {
            String content = (String) request.getContent();
            httpEntity = new StringEntity(content);
        }
        entityRequest.setEntity(httpEntity);
        setContentType(httpUriRequest, request.getContentType());
    }

    private void setContentType(HttpUriRequest httpUriRequest, String contentType) {
        if (contentType != null) {
            httpUriRequest.setHeader(CONTENT_TYPE_FIELD, contentType);
        }
    }

    private boolean vaildRequestTypeForContent(Request request) {
        return request.getRequestType() == RequestType.POST || request.getRequestType() == RequestType.PUT;
    }

    private void setHeaders(HttpUriRequest httpUriRequest, Request request) {
        if (request.getHeaderFields() == null || request.getHeaderFields().isEmpty()) {
            return;
        }
        for (String fieldName : request.getHeaderFields().keySet()) {
            String fieldValue = request.getHeaderFields().get(fieldName);
            httpUriRequest.setHeader(fieldName, fieldValue);
        }
    }

    private HttpUriRequest buildRequest(Request request) {
        String requestURL = URLUtil.buildURLForRequest(request);
        log.info("Request URL: " + requestURL);
        log.info("Request Type: " + request.getRequestType());
        HttpUriRequest httpUriRequest = null;
        switch (request.getRequestType()) {
            case GET:
                httpUriRequest = new HttpGet(requestURL);
                break;
            case POST:
                httpUriRequest = new HttpPost(requestURL);
                break;
            case PUT:
                httpUriRequest = new HttpPut(requestURL);
                break;
            case DELETE:
                httpUriRequest = new HttpDelete(requestURL);
                break;
            case HEAD:
                httpUriRequest = new HttpHead(requestURL);
                break;
        }
        return httpUriRequest;
    }
}
