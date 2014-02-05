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

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.rest.Request;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.logging.Logger;


/**
 * CouchDB Util functions
 */
public final class CouchDBUtil {

    private static Logger log = JCloudScaleConfiguration.getLogger(CouchDBUtil.class);

    //Hide default constructor
    private CouchDBUtil() {}

    /**
     * Add id and revision to the content of a given request
     *
     * @param request the given request
     * @param id the id to add
     * @param revision the revision to add
     * @return the modified request
     */
    public static Request addIdAndRevisionToContent(Request request, String id, String revision) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) parser.parse((String) request.getContent());
            jsonObject.put("_id", id);
            jsonObject.put("_rev", revision);
            String jsonString = jsonObject.toJSONString();
            log.info("Updated Request content with id and rev: " + jsonString);
            return new Request.Builder(request.getProtocolType(), request.getRequestType(), request.getUrl(), request.getHost(), request.getPort())
                    .contentType(request.getContentType()).content(jsonString).build();
        } catch (ParseException e) {
            throw new DatastoreException("Error parsing JSON to add revision: " + e.getMessage());
        }
    }


}