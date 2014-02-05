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
package at.ac.tuwien.infosys.jcloudscale.classLoader.simple;

import java.io.Closeable;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.classLoader.AbstractClassLoaderConfiguration;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SimpleRemoteClassLoaderConfiguration extends AbstractClassLoaderConfiguration 
{
	private static final long serialVersionUID = 1L;
	
	String requestQueue = "CS_Plain_ClassRequest";
	String responseQueue = "CS_Plain_ClassResponse";
	boolean localFirst = true;

	public void setRequestQueue(String requestQueue) {
		this.requestQueue = requestQueue;
	}

	@Override
	public ClassLoader createClassLoader() 
	{
		return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
			@Override
			public ClassLoader run() {
				return new RemoteClassLoader(SimpleRemoteClassLoaderConfiguration.this);
			}});
	}

	@Override
	public Closeable createClassProvider() 
	{
		return new RemoteClassProvider(this);
	}

	public void setLocalFirst(boolean localFirst) {
		this.localFirst = localFirst;
	}
}
