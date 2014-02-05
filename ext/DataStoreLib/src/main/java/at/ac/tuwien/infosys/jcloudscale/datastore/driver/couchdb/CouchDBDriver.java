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
package at.ac.tuwien.infosys.jcloudscale.datastore.driver.couchdb;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoader;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoaderImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.driver.AbstractRestDriver;
import at.ac.tuwien.infosys.jcloudscale.datastore.rest.ProtocolType;
import at.ac.tuwien.infosys.jcloudscale.datastore.rest.Request;
import at.ac.tuwien.infosys.jcloudscale.datastore.rest.RequestType;
import at.ac.tuwien.infosys.jcloudscale.datastore.rest.Response;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.URLUtil;
import org.apache.commons.lang3.StringUtils;

public class CouchDBDriver extends AbstractRestDriver {

    public static final String COUCHDB_PROPERTIES_FILE_NAME = "couchdb.properties";

    @Override
    public PropertyLoader getPropertyLoader() {
        return new PropertyLoaderImpl(COUCHDB_PROPERTIES_FILE_NAME);
    }

    @Override
    protected String getIDFromResponse(Response response) {
        CouchDBResponse couchDBResponse = jsonMapper.deserialize(response.getContent(), CouchDBResponse.class);
        return couchDBResponse.getId();
    }

    @Override
    protected String getLastRevision(Datastore datastore, String id) {
        Request request = new Request.Builder(ProtocolType.HTTP, RequestType.HEAD, URLUtil.getCouchDBCurrentRevision(datastore, id), datastore.getHost(), datastore.getPort()).build();
        Response response = requestHandler.handle(request);
        response.verify();
        return StringUtils.remove(response.getHeaderField("ETag"), '"');
    }

    @Override
    public void update(Datastore datastore, String id, Object object) {
        String jsonString = jsonMapper.serialize(object);
        Request request = new Request.Builder(ProtocolType.HTTP, RequestType.PUT, URLUtil.getUpdateURL(datastore, id), datastore.getHost(), datastore.getPort())
                .contentType("application/json").content(jsonString).build();
        String revision = getLastRevision(datastore, id);
        request = CouchDBUtil.addIdAndRevisionToContent(request, id, revision);
        requestHandler.handle(request).verify();
    }
}
