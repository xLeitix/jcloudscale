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
package at.ac.tuwien.infosys.jcloudscale.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author rst
 * Specifies to the classloader that this class depends on 
 * specified set of files to be available. 
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface FileDependency 
{
	/**
	 * List of files that annotated class depends on. 
	 */
	String[] files() default {};
	
	/**
	 * Access type that is required to these files.
	 */
	FileAccess accessType() default FileAccess.ReadOnly;
	
	/**
	 * Dependency provider that provides dynamic information about
	 * required files. If this parameter is specified, others are ignored.
	 */
	Class<? extends IFileDependencyProvider> dependencyProvider() default IFileDependencyProvider.class;
	
	//--------------------------------------------
	
	public interface IFileDependencyProvider
	{
		DependentFile[] getDependentFiles();
	}
	
	public static enum FileAccess
	{
		ReadOnly,
//		ReadWrite,
	}
	
	public static class DependentFile
	{
		public DependentFile(String filePath)
		{
			this.filePath = filePath;
			this.accessType = FileAccess.ReadOnly;
		}
		
		public DependentFile(String filePath, FileAccess accessType)
		{
			this.filePath = filePath;
			this.accessType = accessType;
		}
		/**
		 * Relative path to the file
		 */
		public String filePath;
		
		/**
		 * Access type that is required for this file
		 */
		public FileAccess accessType;
	}
}
