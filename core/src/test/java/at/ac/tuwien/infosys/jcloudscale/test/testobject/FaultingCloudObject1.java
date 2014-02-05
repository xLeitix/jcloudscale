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

@CloudObject
public class FaultingCloudObject1 {
	
	public FaultingCloudObject1(){}
	
	public FaultingCloudObject1(int throwRuntime)
	{
		throw new RuntimeException("Ho-Ho-Ho!");
	}
	
	public FaultingCloudObject1(boolean throwCustomException) throws MyFaultException
	{
		throw new MyFaultException("Ho-Ho-Ho!");
	}
	
	@SuppressWarnings("null")
	public FaultingCloudObject1(String throwNullPointerException)
	{
		FaultingCloudObject1 obj = null;
		obj.faulty();
	}
	
	public void faulty() {
		throw new NullPointerException("Hoho!");
	}
	
	public void nonSerializable(@ByValueParameter NonSerializableDataType dt) {}
	
	public static class MyFaultException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public MyFaultException(String message)
		{
			super(message);
		}
	}
	
}
