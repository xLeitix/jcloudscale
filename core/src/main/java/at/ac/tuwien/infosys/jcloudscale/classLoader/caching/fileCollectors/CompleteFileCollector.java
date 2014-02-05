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
import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.infosys.jcloudscale.classLoader.RemoteClassLoaderUtils;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderFile;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ContentType;

/**
 * Implements class collector that provides all classes and files that 
 * belong to this project on any request.
 * @author rst
 */
public class CompleteFileCollector extends FileCollectorAbstract 
{

	@Override
	public ClassLoaderOffer collectFilesForClass(String classname) 
	{
			//just load this class to check whether we have it at all...
		Class<?> clazz = RemoteClassLoaderUtils.getClass(classname, classloader);
		if(clazz == null)
		{
			ClassLoaderOffer offer = collectFile(classname);
			if (offer == null) {
				log.severe("Could not find class " + classname + ". Therefore, no classes were provided.");
			}
			return offer;
		}

		log.info("Collecting all items on classpath as answer to request for "+ classname);
		
		String classPath = System.getProperty("java.class.path");
		if(classPath == null || classPath.length() == 0)
		{
			log.severe("Failed to collect files from classpath as classpath could not be retrieved.");
			return null;
		}
		
		String[] classPathElements = classPath.split(":");
		
		List<ClassLoaderFile> result = new ArrayList<ClassLoaderFile>();
		List<String> classesWithFiles = new ArrayList<String>();
		
		for(String pathElement : classPathElements)
		{
			File file = new File(pathElement);
			if(!file.exists())
			{
				log.info("Found not existing path on classpath: "+ file.getAbsolutePath());
				continue;
			}
			
			if(file.isDirectory())
				collectClassesFromDirectory(file, "", classesWithFiles, result);
			else
				if(RemoteClassLoaderUtils.isJarFile(pathElement))
				{
					ClassLoaderFile jarFile = new ClassLoaderFile(file.getName(), file.lastModified(), file.length(), ContentType.JAR);
					jarFile.setTag(file.getAbsolutePath());
					result.add(jarFile);
				}
		}
		
		//checking if the dependencies of the class we were specifically asked for are provided
		if(!classesWithFiles.contains(classname))
		{
			try 
			{
				List<ClassLoaderFile> files = super.collectRequiredFiles(Class.forName(classname));
				if(files != null && files.size() > 0)
				{
					result.addAll(files);
					classesWithFiles.add(classname);
				}
			} 
			catch (ClassNotFoundException e) 
			{
				//should not throw here as we've checked already at the beginning of this method.
			}
		}
		
		sortByDependency(result);
		
		ClassLoaderOffer offer = new ClassLoaderOffer(null, 0, result.toArray(new ClassLoaderFile[result.size()]));
		offer.setClassesWithFiles(classesWithFiles.toArray(new String[classesWithFiles.size()]));
		log.info(String.format("Collected %s files, %sKB total", result.size(), calculateSize(result)/1024));
		return offer;
	}

	private int calculateSize(List<ClassLoaderFile> files) 
	{
		int result = 0;
		for(ClassLoaderFile file : files)
			result += file.getSize();
		
		return result;
	}

	private void collectClassesFromDirectory(File directory, String prefix, List<String> classesWithFiles, List<ClassLoaderFile> result) 
	{
		for(File file : directory.listFiles())
		{
			if(file.isDirectory())
				collectClassesFromDirectory(file, prefix+file.getName()+".", classesWithFiles, result);
			else 
				if(RemoteClassLoaderUtils.isClassName(file.getName()))
					addClassFile(file, prefix, classesWithFiles, result);
		}
	}

	private void addClassFile(File file, String prefix,	List<String> classesWithFiles, List<ClassLoaderFile> result) 
	{
		String fullClassName = RemoteClassLoaderUtils.convertClassPathToClassName(prefix + file.getName());
		
		//adding class itself
		result.add(new ClassLoaderFile(fullClassName, file.lastModified(), file.length(), ContentType.CLASS));
		
		//adding related files
		try
		{
			List<ClassLoaderFile> requiredFiles = collectRequiredFiles(RemoteClassLoaderUtils.getClass(fullClassName, classloader));
			if(requiredFiles != null)
			{
				result.addAll(requiredFiles);
				classesWithFiles.add(fullClassName);
			}
		}
		catch(Exception ex)
		{
			log.info("Exception while collecting files for class "+ fullClassName + ":" +ex);
		}
	}
}
