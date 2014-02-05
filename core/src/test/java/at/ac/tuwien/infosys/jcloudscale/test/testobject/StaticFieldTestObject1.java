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

import at.ac.tuwien.infosys.jcloudscale.annotations.CloudGlobal;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;

@CloudObject
public class StaticFieldTestObject1 {
	
	@CloudGlobal
	private static String myTestField;
	
	@CloudGlobal
	public static String myTestField2 = "local";
	
	public static String myTestField3 = "local";
	
	public static void resetStaticFieldsToDefaults()
	{
		myTestField = "";
		myTestField2 = "local";
		myTestField3 = "local";
	}
	
	public void resetStaticFieldsToDefaultsRemote()
	{
	    resetStaticFieldsToDefaults();
	}
	
	public void setField(String val) {
		myTestField = val;
		myTestField2 = val;
		myTestField3 = val;
	}
	
	public String getField() {
		return myTestField;
	}
	
	public String getOtherField() {
		return myTestField2;
	}
	
	public String getYetAnotherField() {
		return myTestField3;
	}
	
}
