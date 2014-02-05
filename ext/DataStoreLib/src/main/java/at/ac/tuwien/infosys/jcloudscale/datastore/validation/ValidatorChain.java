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

/**
 * Basic validator chain interface
 */
public interface ValidatorChain {

    /**
     * Add the given validator to the chain
     *
     * @param validator the validator to add
     */
    void add(Validator validator);

    /**
     * Validate the chain
     */
    void validate();
}
