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
package at.ac.tuwien.infosys.jcloudscale.datastore.driver;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreDriver;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.Mapper;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.json.JsonMapperImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.rest.*;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.URLUtil;

/**
 * Basic REST Driver Implementation
 */
public abstract class AbstractRestDriver implements DatastoreDriver {

    protected RequestHandler requestHandler = new RequestHandlerImpl();
    protected Mapper<String> jsonMapper = new JsonMapperImpl();

    @Override
    public String save(Datastore datastore, Object object) {
        String jsonString = jsonMapper.serialize(object);
        Request request = new Request.Builder(ProtocolType.HTTP, RequestType.POST, URLUtil.getSaveURL(datastore), datastore.getHost(), datastore.getPort())
                .contentType("application/json").content(jsonString).build();
        Response response = requestHandler.handle(request);
        response.verify();
        return getIDFromResponse(response);
    }

    @Override
    public void save(Datastore datastore, String id, Object object) {
        String jsonString = jsonMapper.serialize(object);
        Request request = new Request.Builder(ProtocolType.HTTP, RequestType.PUT, URLUtil.getSaveWithIdURL(datastore, id), datastore.getHost(), datastore.getPort())
                .contentType("application/json").content(jsonString).build();
        requestHandler.handle(request).verify();
    }

    @Override
    public <T> T find(Datastore datastore, Class<T> objectClass, String id) {
        Request request = new Request.Builder(ProtocolType.HTTP, RequestType.GET, URLUtil.getFindURL(datastore, id), datastore.getHost(), datastore.getPort()).build();
        Response response = requestHandler.handle(request);
        response.verify();
        return jsonMapper.deserialize(response.getContent(), objectClass);
    }

    @Override
    public void delete(Datastore datastore, Class<?> objectClass, String id) {
        String revision = getLastRevision(datastore, id);
        Request request = new Request.Builder(ProtocolType.HTTP, RequestType.DELETE, URLUtil.getDeleteURL(datastore, id, revision), datastore.getHost(), datastore.getPort()).build();
        requestHandler.handle(request).verify();
    }

    @Override
    public void update(Datastore datastore, String id, Object object) {
        String jsonString = jsonMapper.serialize(object);
        Request request = new Request.Builder(ProtocolType.HTTP, RequestType.PUT, URLUtil.getUpdateURL(datastore, id), datastore.getHost(), datastore.getPort())
                .contentType("application/json").content(jsonString).build();
        requestHandler.handle(request).verify();
    }

    /**
     * Get the datastore ID from the response
     *
     * @param response given response
     * @return the ID in the datastore
     */
    protected abstract String getIDFromResponse(Response response);

    /**
     * Get the last revision of the object
     *
     *
     * @param datastore the datastore
     * @param id the id of the object
     * @return the last revision
     */
    protected abstract String getLastRevision(Datastore datastore, String id);
}
