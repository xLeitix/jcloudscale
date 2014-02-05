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
package at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

public class HbaseConfig {

    /**
     * Create the HBase Configuration for a given datastore
     *
     * @param datastore the given datastore
     * @return the HBase Configuration
     */
    public static Configuration getConfig(Datastore datastore) {
        Configuration configuration = HBaseConfiguration.create();
        configuration.clear();
        configuration.set("hbase.zookeeper.quorum", datastore.getHost());
        return configuration;
    }
}
