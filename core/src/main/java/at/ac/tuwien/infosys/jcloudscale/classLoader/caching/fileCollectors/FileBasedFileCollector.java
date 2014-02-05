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
package at.ac.tuwien.infosys.jcloudscale.classLoader.caching.fileCollectors;

import java.io.File;
import java.util.List;

import at.ac.tuwien.infosys.jcloudscale.classLoader.RemoteClassLoaderUtils;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderFile;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ContentType;

/**
 * @author rst
 * This class collection collects only the file where requested class is stored 
 * and related files.
 */
public class FileBasedFileCollector extends FileCollectorAbstract 
{
	@Override
	public ClassLoaderOffer collectFilesForClass(String classname)
	{
		// retrieving class itself
		Class<?> clazz = RemoteClassLoaderUtils.getClass(classname, classloader);
		if (clazz == null) {
			ClassLoaderOffer offer = collectFile(classname);
			if (offer == null) {
				log.severe("Class " + classname + " was requested, but we don't have such class here. Refusing all offers.");
			}
			return offer;
		}
		
		// checking if it has file dependencies
		List<ClassLoaderFile> requiredFiles = collectRequiredFiles(clazz);
		
		String filename = RemoteClassLoaderUtils.getContainingFile(clazz);
		File classFile = new File(filename);
		ClassLoaderFile classRelatedFile = new ClassLoaderFile(
				RemoteClassLoaderUtils.isJarFile(filename) ? classFile.getName() : clazz.getName(),//if it is jar, we use jar name. otherwise -- class name.
				classFile.lastModified(), classFile.length(), 
				RemoteClassLoaderUtils.isJarFile(filename)? ContentType.JAR : ContentType.CLASS);
		
		// if this is a jar file, we place full path into the tag.
		if(RemoteClassLoaderUtils.isJarFile(filename))
			classRelatedFile.setTag(classFile.getAbsolutePath());
			
		if(requiredFiles != null)
		{
			requiredFiles.add(classRelatedFile);
			
			ClassLoaderOffer offer = new ClassLoaderOffer(null, 0, requiredFiles.toArray(new ClassLoaderFile[requiredFiles.size()]));
			offer.setClassesWithFiles(new String[]{classRelatedFile.getName()});
			
			return offer;
		}
		else
			return new ClassLoaderOffer(null, 0, new ClassLoaderFile[]{classRelatedFile});
	}
}
