/*
   Copyright 2013 Philipp Leitner

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
package at.ac.tuwien.infosys.jcloudscale.test.testobject;

import java.util.UUID;

import at.ac.tuwien.infosys.jcloudscale.annotations.ByValueParameter;
import at.ac.tuwien.infosys.jcloudscale.annotations.ClientObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObjectId;
import at.ac.tuwien.infosys.jcloudscale.annotations.Local;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;

@CloudObject
public class ObjectWithIdFieldAndCCOField {
	
	@CloudObjectId
	public UUID id = null;
	
	@ClientObject
	@Local
	// note: this needs to be both @Local and public
	// Similarly, the getter for this field should also be @Local
	public ClientCloudObject cco;
	
	public ObjectWithIdFieldAndCCOField() {}
	
	public @ByValueParameter UUID getId() {
		return id;
	}
	
	public @Local ClientCloudObject getCCO() {
		return cco;
	}
	
}
