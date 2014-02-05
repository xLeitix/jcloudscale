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

import java.io.Serializable;
import java.util.Date;

import at.ac.tuwien.infosys.jcloudscale.annotations.ByValueParameter;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;

@CloudObject
public class FieldCloudObject extends FieldCloudObjectParent {
	
	private String privateField = "private";
	protected String protectedField = "protected";
	public String publicField = "public";
	public Object publicRefField = new Date(100);
	
	@ByValueParameter
	public Serializable serializable = null;
	
	@ByValueParameter
	public Serializable getSerializable() {
		return serializable;
	}

	public void setSerializable(@ByValueParameter Serializable serializable) {
		this.serializable = serializable;
	}

	public static void setPrivateField(FieldCloudObject obj, String value) {
		obj.privateField = value;
	}
	
	public static void setProtectedField(FieldCloudObject obj, String value) {
		obj.protectedField = value;
	}
	
	public Object getPublicRefField() {
		return publicRefField;
	}
	public void setPublicRefField(Object publicRefField) {
		this.publicRefField = publicRefField;
	}
	public String getPrivateField() {
		return privateField;
	}
	public void setPrivateField(String privateField) {
		this.privateField = privateField;
	}
	public String getProtectedField() {
		return protectedField;
	}
	public void setProtectedField(String protectedField) {
		this.protectedField = protectedField;
	}
	public String getPublicField() {
		return publicField;
	}
	public void setPublicField(String publicField) {
		this.publicField = publicField;
	}
	
}
