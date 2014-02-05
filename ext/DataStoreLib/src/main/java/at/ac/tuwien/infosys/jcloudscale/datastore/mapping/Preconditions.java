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
package at.ac.tuwien.infosys.jcloudscale.datastore.mapping;


public final class Preconditions {

    //Hide Constructor
    private Preconditions(){}

    /**
     * Verify that the given object is not null otherwise and exception is thrown
     *
     * @param object the given object
     */
    public static void notNull(Object object) {
        if(object == null) {
            throw new NullPointerException("Object " + object + " is null.");
        }
    }

    /**
     * Verify that the given objects are not null otherwise an exception is thrown
     *
     * @param objects the given objects
     */
    public static void notNull(Object... objects) {
        for(Object object : objects) {
            notNull(object);
        }
    }
}
