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
import at.ac.tuwien.infosys.jcloudscale.annotations.DestructCloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.Local;

@CloudObject
public class MoreComplexTestClass extends MoreComplexTestSuperClass {
	private String filename = "1.yap";
	private long constructTime = System.nanoTime();

	@Local
	private String localFilename = "1.yap";
	
	@Local
	public MoreComplexTestClass() {
//		if (new File(filename).exists())
//			new File(filename).delete();
	}
	
	public MoreComplexTestClass(String filename) {
		this.filename = filename;
//		if (new File(filename).exists())
//			new File(filename).delete();
	}
	
	MoreComplexTestClass(boolean justSomething)
	{
		this();
	}

	private Object config() {
		return new Object();
	}

	public void run() 
	{
		config();
	}

	public void setInCloud(String string) 
	{
		this.localFilename = string;
	}
	
	public @Local String getLocally() 
	{
		return localFilename;
	}
	
	protected String protectedMethod() 
	{
		return filename;
	}
	
	long packageMethod() 
	{
		return constructTime;
	}

	@DestructCloudObject
	public void destroy() {

	}
	
	public static MoreComplexTestClass createInstanceWithProtectedConstructor()
	{
		return new MoreComplexTestClass(true);
	}
	
	public static String invokeProtectedMethod(MoreComplexTestClass obj )
	{
		return obj.protectedMethod();
	}
	
	public static long invokePackageMethod(MoreComplexTestClass obj )
	{
		return obj.packageMethod();
	}
}
