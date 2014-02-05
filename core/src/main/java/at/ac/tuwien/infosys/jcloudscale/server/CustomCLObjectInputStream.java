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
package at.ac.tuwien.infosys.jcloudscale.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;


public class CustomCLObjectInputStream extends ObjectInputStream {

	private ClassLoader classLoader;
	
	public CustomCLObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException 
	{
		super(in);
		this.classLoader = classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;
	}
	
	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException, IOException {
		
		try
		{
			return super.resolveClass(desc);
		}
		catch(ClassNotFoundException ex){}
		
		try
		{
			return Class.forName(desc.getName(), false, classLoader);//classLoader.loadClass(desc.getName());
		}
		catch(ClassNotFoundException ex)
		{
			ex.printStackTrace(); 
			throw ex;
		}
		
	}

}
