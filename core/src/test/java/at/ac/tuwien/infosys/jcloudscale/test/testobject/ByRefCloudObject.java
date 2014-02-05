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

import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;

@CloudObject
public class ByRefCloudObject {
	
	private TestByRefParameter param;
	
	private String name = "none";
	public static String value = "remote";
	private TestByValueParameter byValueParameter = null;
	
	
	public ByRefCloudObject() {}
	
	public ByRefCloudObject(String name) {
		this.name = name;
	}
	
	public ByRefCloudObject(String name, TestByValueParameter param)
	{
		this(name);
		if(!param.getClass().equals(TestByValueParameter.class))
			throw new RuntimeException("Incorrect parameter type! Expecting it to be passed by value!");
		
		byValueParameter = param;
	}
	
	public ByRefCloudObject(TestByRefParameter test) {
		this.param = test;
	}
	
	public String passMeThis(TestByRefParameter test) {
		if(test != null)
			return test.callByRef();
		else
			return null;
	}
	
	public String passMeThis(TestByRefParameter2 test) {
		return test.callByRef();
	}
	
//	public void testInstanceof(TestByRefParameter param)
//	{
//		if(param instanceof TestByRefParameter)
//			return;
//		
//		throw new RuntimeException("Unexpected type of the input parameter: "+param.getClass() +" instead of "+ TestByRefParameter.class.getName());
//	}
	
	public TestByValueParameter swapByValueParameter(TestByValueParameter newValue)
	{
		TestByValueParameter oldParam = byValueParameter;
		
		if(newValue == null || !newValue.getClass().equals(TestByValueParameter.class))
			throw new RuntimeException("Incorrect parameter type! Expecting it to be passed by value!");
		
		byValueParameter = newValue;
		
		return oldParam;
	}
	
	public void testInstanceofInterface(ITestByRefParameter param)
	{
		if(param instanceof TestByRefParameter)
			return;
		
		throw new RuntimeException("Unexpected type of the input parameter: "+param.getClass() +" instead of "+ TestByRefParameter.class.getName());
	}
	
	public String passMeThis(TestByRefParameter test, TestByRefParameter test2) {
		return test.callByRef() + ":" + test2.callByRef();
	}
	
	public String passMeThis() {
		return param.callByRef();
	}
	
	public String thereAndBackAgain(TestByRefParameter test) {
		TestByRefParameter thisIsOnTheCloud = new TestByRefParameter();
		return test.fromTheCloud(thisIsOnTheCloud);
	}
	
	public String thereAndBackAgainWithThis(TestByRefParameter test) {
		return test.fromTheCloud(this);
	}
	
	public String withOtherCO(ByRefCloudObject other) {
		return other.getName();
	}
	
	public TestByRefParameter giveMeAParam() {
		return new TestByRefParameter();
	}
	
	public TestByRefParameter giveMeANullParam() {
		return null;
	}
	
	public ByRefCloudObject returnYourself() {
		return this;
	}
	
	public void invoke(TestByRefParameter testByRefParameter) {
		testByRefParameter.theVoid();
	}
	
	public String getValue() {
		return value;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public boolean equals(Object other) {
		return this.hashCode() == other.hashCode();
	}
	
}
