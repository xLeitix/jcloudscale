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
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObjectId;

@CloudObject
public class ObjectWithMultipleIdFields {
	
	@CloudObjectId
	public UUID id1 = null;
	
	@CloudObjectId
	public String id2 = null;
	
	@CloudObjectId
	public Object id3 = null;
	
	
	public ObjectWithMultipleIdFields() {}
	
	public @ByValueParameter UUID getId1() {
		return id1;
	}
	
	public String getId2() {
		return id2;
	}
	
	public @ByValueParameter Object getId3() {
		return id3;
	}
	
}
