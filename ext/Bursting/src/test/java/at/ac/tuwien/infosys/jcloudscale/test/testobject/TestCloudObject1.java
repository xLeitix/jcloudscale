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

import at.ac.tuwien.infosys.jcloudscale.annotations.ByValueParameter;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.DestructCloudObject;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.ITestObject;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.SerializableDataType;

@CloudObject
public class TestCloudObject1 implements ITestObject {
	
	public static boolean isLocal = false;
	
	private String testField = null;
	private SerializableDataType serial = null;
	
	public TestCloudObject1() {}
	
	public TestCloudObject1(String testField) {
		this.testField = testField;
	}
	
	@Override
	public String getTestField() {
		return testField;
	}

	@Override
	public void setTestField(String testField) {
		this.testField = testField;
	}

	@Override
	public String multiplyAndConvert(int a, int b) {
		return String.valueOf(a*b);
	}
	
	@Override
	public boolean executingLocal() {
		return isLocal;
	}
	
	public SerializableDataType getSerial() {
		return serial;
	}

	public void setSerial(@ByValueParameter SerializableDataType serial) {
		this.serial = serial;
	}
	
	@Override
	@DestructCloudObject
	public void killMeSoftly(){}
	
}
