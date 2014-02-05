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

/**
 * Riak Util functions
 */
public final class RiakUtil {

    //Hide default constructor
    private RiakUtil() {}

    /**
     * Return id of stored object by given location string
     *
     * @param locationString given location string
     * @return id of the new stored object
     */
    public static String getIdFromLocationString(String locationString) {
        if(locationString == null) return null;
        String[] locationStringParts = locationString.split("/");
        if(locationStringParts != null && locationStringParts.length > 0) {
            return locationStringParts[locationStringParts.length -1];
        }
        return locationString;
    }
}
