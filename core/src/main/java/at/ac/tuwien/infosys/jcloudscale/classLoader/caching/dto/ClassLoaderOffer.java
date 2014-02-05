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

import java.io.Serializable;

public class ClassLoaderOffer implements Serializable 
{
	private static final long serialVersionUID = 7891840631595347811L;
	
	private String name;
	private long creationDate;
	private ClassLoaderFile[] files;
	private String[] classesWithFiles;
	
	//-----------------------------------------------------
	
	public ClassLoaderOffer(){}
	
	public ClassLoaderOffer(String name, long creationDate)
	{
		this.name = name;
		this.creationDate = creationDate;
	}
	
	public ClassLoaderOffer(String name, long creationDate, ClassLoaderFile[] files)
	{
		this(name, creationDate);
		this.files = files;
	}
	
	//-----------------------------------------------------

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public ClassLoaderFile[] getFiles() {
		return files;
	}

	public void setFiles(ClassLoaderFile[] files) {
		this.files = files;
	}
	
	public boolean hasName()
	{
		return this.name != null && this.name.length() > 0;
	}
	
	public boolean hasFiles()
	{
		return this.files != null && this.files.length > 0;
	}
	
	public String[] getClassesWithFiles() {
		return classesWithFiles;
	}

	public void setClassesWithFiles(String[] classesWithFiles) {
		this.classesWithFiles = classesWithFiles;
	}

	//-----------------------------------------------------

	@Override
	public String toString() 
	{
		return String.format("%1$s (%2$s): %3$s file(s)", 
						name, creationDate, 
						files != null ? files.length : "no");
	}

}
