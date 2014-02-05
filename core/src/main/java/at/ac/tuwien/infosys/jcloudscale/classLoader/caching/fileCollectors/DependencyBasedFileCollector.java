/*
   Copyright 2014 Philipp Leitner 

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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.infosys.jcloudscale.classLoader.RemoteClassLoaderUtils;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderFile;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ContentType;

/**
 * @author RST
 * This class collector provides jar for requested class if it is declared in jar.
 * Otherwise, it provides class with dependencies and everything else that is available from the same package.
 */
public class DependencyBasedFileCollector extends FileCollectorAbstract {

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
		
		// declaring some necessary stuff.
		String filename = RemoteClassLoaderUtils.getContainingFile(clazz);
		File classFile = new File(filename);
		ClassLoaderFile classRelatedFile = null;
		
		List<ClassLoaderFile> collectedFiles = new ArrayList<>();
		Set<String> collectedClasses = new HashSet<String>();
		
		if(RemoteClassLoaderUtils.isJarFile(filename))
		{
			classRelatedFile = new ClassLoaderFile(classFile.getName(), classFile.lastModified(), classFile.length(), ContentType.JAR);
			// if this is a jar file, we place full path into the tag.
			classRelatedFile.setTag(classFile.getAbsolutePath());
		}
		else
		{
			classRelatedFile = new ClassLoaderFile(clazz.getName(), classFile.lastModified(), classFile.length(), ContentType.CLASS);
			collectDependentFiles(clazz, collectedClasses);
		}

		// collecting file dependencies for collected classes
		List<String> classesWithFiles = new ArrayList<>();
		try 
		{
			for(String clz : collectedClasses)
			{
				// adding class as file to the collected files list.
				ClassLoaderFile file = clz.equals(classname) ? classRelatedFile : ClassLoaderFile.forClass(Class.forName(clz));
				if(file == null)
					continue;// this class is declared in jar and this is not our class. Should we capture the whole jar? No for now.
				
				collectedFiles.add(file);
				
				// adding dependencies.
				List<ClassLoaderFile> requiredFiles = collectRequiredFiles(clazz);
				if(requiredFiles != null)
				{
					classesWithFiles.add(clz);
					collectedFiles.addAll(requiredFiles);
				}
			}
		} 
		catch (ClassNotFoundException e) 
		{//should not happen.
		}
		
		super.sortByDependency(collectedFiles);
		
		// preparing offer
		ClassLoaderOffer offer = new ClassLoaderOffer(null, 0, collectedFiles.toArray(new ClassLoaderFile[collectedFiles.size()]));
		offer.setClassesWithFiles(classesWithFiles.toArray(new String[classesWithFiles.size()]));
		
		return offer;
	}

	/**
	 * Collects files this class depends to
	 */
	private void collectDependentFiles(Class<?> clazz, Set<String> collectedFiles) 
	{
		if(clazz.isArray())
			clazz = clazz.getComponentType();
		
		if(collectedFiles.contains(clazz.getName()))//we collected it already.
				return;
		
		collectedFiles.add(clazz.getName());
		
		if(clazz.isAnnotation() || clazz.isEnum())
			return;
		
		// collecting parent class(es)
		Class<?> parent = clazz.getSuperclass();
		if(!isJdkClass(parent) && sameSource(clazz, parent))
			collectDependentFiles(parent, collectedFiles);
		
		// collecting interfaces
		for(Class<?> interf : clazz.getInterfaces())
			if(!isJdkClass(interf) && sameSource(clazz, interf))
				collectDependentFiles(interf, collectedFiles);
		
		// collecting internal classes
		for(Class<?> cls : clazz.getDeclaredClasses())
			collectDependentFiles(cls, collectedFiles);
		
		// collecting classes declared by fields
		for(Field fld : clazz.getDeclaredFields())
		{
			Class<?> cls = fld.getType();
			
			if(!isJdkClass(cls) && sameSource(clazz, cls))
				collectDependentFiles(cls, collectedFiles);
		}
	}

	private static boolean sameSource(Class<?> clazz1, Class<?> clazz2) 
	{
		return clazz1.getProtectionDomain().getCodeSource().getLocation().getPath()
			.equals(clazz2.getProtectionDomain().getCodeSource().getLocation().getPath());
	}

	private static boolean isJdkClass(Class<?> clazz) 
	{
		if(clazz == null)
			return true;//null is also a jdk class, why not? ;-)
		
		if(clazz.getClassLoader() == null)
			return true;
		
		return clazz.getClassLoader().equals("".getClass().getClassLoader());
	}
}
