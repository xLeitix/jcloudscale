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
package at.ac.tuwien.infosys.jcloudscale.datastore.validation;

import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.DatastoreId;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ReflectionUtil;

/**
 * Checks if the given object has an ID set
 */
public class HasIdValidator implements Validator {

    private Object object;

    public HasIdValidator(Object object) {
        this.object = object;
    }

    @Override
    public boolean validate() {
        Object id = ReflectionUtil.getFieldValueOfFieldWithAnnotation(object, DatastoreId.class);
        return id != null;
    }

    @Override
    public String getErrorMessage() {
        return "ID of object is null.";
    }
}
