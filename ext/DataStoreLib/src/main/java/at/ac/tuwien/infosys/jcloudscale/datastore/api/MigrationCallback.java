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
package at.ac.tuwien.infosys.jcloudscale.datastore.api;

import java.util.List;

/**
 * Interface for objects used as migration callback
 */
public interface MigrationCallback<T> {

    /**
     * Called when migration is successful
     *
     * @param result the list of migrated objects
     */
    void onSuccess(List<T> result);

    /**
     * Called when an error during migration occurs
     */
    void onError();
}
