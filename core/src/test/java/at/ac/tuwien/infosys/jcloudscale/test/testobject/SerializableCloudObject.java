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
import java.util.concurrent.TimeUnit;

import at.ac.tuwien.infosys.jcloudscale.annotations.ByValueParameter;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.DestructCloudObject;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.cache.CacheType;

@CloudObject
public class SerializableCloudObject implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public static boolean isLocal = false;
	
	private String testField = null;
	private SerializableDataType serial = null;
	
	public SerializableCloudObject() {}
	
	public SerializableCloudObject(String testField) {
		this.testField = testField;
	}
	
	public void run(int timeout) throws InterruptedException {
		Thread.sleep(timeout);
	}
	
	public String getTestField() {
		return testField;
	}

	public void setTestField(String testField) {
		this.testField = testField;
	}

	public String multiplyAndConvert(int a, int b) {
		return String.valueOf(a*b);
	}
	
	public boolean executingLocal() {
		return isLocal;
	}
	
	public SerializableDataType getSerial() {
		return serial;
	}

	public void setSerial(@ByValueParameter SerializableDataType serial) {
		this.serial = serial;
	}
	
	public String convertToStringEnum(TimeUnit timeUnit)
	{
		return timeUnit.toString();
	}
	
	public String converToStringUserEnum(CacheType cacheType)
	{
		return cacheType.toString();
	}
	
	public enum MyEnum {Small, Big }
	
	public MyEnum getObjectSize(String objectName)
	{
		return "universe".equalsIgnoreCase(objectName) ? MyEnum.Big : MyEnum.Small;
	}
	
	@DestructCloudObject
	public void killMeSoftly(){}
}
