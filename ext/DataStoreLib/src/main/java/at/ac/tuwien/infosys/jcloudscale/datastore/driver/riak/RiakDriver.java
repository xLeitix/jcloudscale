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
package at.ac.tuwien.infosys.jcloudscale.datastore.driver.riak;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoader;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoaderImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.driver.AbstractRestDriver;
import at.ac.tuwien.infosys.jcloudscale.datastore.rest.Response;

public class RiakDriver extends AbstractRestDriver {

    public static final String RIAK_PROPERTIES_FILE_NAME = "riak.properties";

    @Override
    public PropertyLoader getPropertyLoader() {
        return new PropertyLoaderImpl(RIAK_PROPERTIES_FILE_NAME);
    }

    @Override
    protected String getIDFromResponse(Response response) {
        String locationString = response.getHeaderField("Location");
        return RiakUtil.getIdFromLocationString(locationString);
    }

    @Override
    protected String getLastRevision(Datastore datastore, String id) {
        //riak doesn't manage revisions
        return null;
    }
}
