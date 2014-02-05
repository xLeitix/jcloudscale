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
package at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

import at.ac.tuwien.infosys.jcloudscale.classLoader.RemoteClassLoaderUtils;

public class ClassLoaderFile implements Serializable 
{
	private static final long serialVersionUID = -8448515060351954162L;

	private String name;
	private long lastModifiedDate;
	private long size;
	private ContentType type;
	private byte[] content;
	
	/**
	 * Special field that might be used to store additional information.
	 * Field is transient.
	 */
	private transient Object tag;
	
	//----------------------------------------------------------------
	public ClassLoaderFile(){}
	
	public ClassLoaderFile(String name, long lastModifiedDate, long size, ContentType type)
	{
		this.lastModifiedDate = lastModifiedDate;
		this.size = size;
		this.type = type;
		setName(name);
	}
	
	public ClassLoaderFile(String name, long lastModifiedDate, ContentType type, byte[] content)
	{
		this(name, lastModifiedDate, content.length, type);
		this.content = content;
	}
	
	/**
	 * Creates classloaderfile instance for class file. If the class is declared in jar, returns null.
	 */
	public static ClassLoaderFile forClass(Class<?> clazz)
	{
		String filename = RemoteClassLoaderUtils.getContainingFile(clazz);
		
		if(RemoteClassLoaderUtils.isJarFile(filename))
			return null;
		
		File classFile = new File(filename);
		return new ClassLoaderFile(clazz.getName(), classFile.lastModified(),classFile.length(), ContentType.CLASS);
	}
	
	//----------------------------------------------------------------

	public String getName() {
		return RemoteClassLoaderUtils.unescapeFilePath(this.name);// we escape/unescape filename here to avoid cross-platform problems.
	}

	public void setName(String name) 
	{
		this.name = RemoteClassLoaderUtils.escapeFilePath(name);// we escape/unescape filename here to avoid cross-platform problems.
	}

	public long getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(long lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public ContentType getType() {
		return type;
	}

	public void setType(ContentType type) {
		this.type = type;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}
	
	public Object getTag() {
		return tag;
	}

	public void setTag(Object tag) {
		this.tag = tag;
	}
	
	//----------------------------------------------------------------

	@Override
	public String toString() 
	{
		return String.format("%1$s (%2$s, %3$sB): %4$s, last modified %5$s", 
								name, type, size, 
								content == null ? "NO CONT": "WITH CONT", 
								new Date(lastModifiedDate));
	}
}
