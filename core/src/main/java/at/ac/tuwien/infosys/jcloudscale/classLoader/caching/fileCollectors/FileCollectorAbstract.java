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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.core.io.ClassPathResource;

import at.ac.tuwien.infosys.jcloudscale.annotations.FileDependency;
import at.ac.tuwien.infosys.jcloudscale.annotations.FileDependency.DependentFile;
import at.ac.tuwien.infosys.jcloudscale.annotations.FileDependency.IFileDependencyProvider;
import at.ac.tuwien.infosys.jcloudscale.classLoader.RemoteClassLoaderUtils;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderFile;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ContentType;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;

import com.google.common.io.Files;

/**
 * @author rst
 * Abstract class that is responsible for dependencies collection for the provided class.
 */
public abstract class FileCollectorAbstract 
{
	protected Logger log = null;
	protected ClassLoader classloader;
	
	public FileCollectorAbstract()
	{
		this.log = JCloudScaleConfiguration.getLogger(this);
		classloader = this.getClass().getClassLoader();
	}
	
	//--------------------------------------------------------
	
	/**
	 * Collects the list of required files without content.
	 * @param clazz the class that related files should be collected for. (joda style)
	 * @return List of files that are required for specified class or 
	 * null if none are required. 
	 */
	protected List<ClassLoaderFile> collectRequiredFiles(Class<?> clazz) 
	{
		FileDependency fileDependency = clazz.getAnnotation(FileDependency.class);
		
		if(fileDependency == null)
			return null;
		
		List<ClassLoaderFile> files = new ArrayList<ClassLoaderFile>();
		
		if(fileDependency.dependencyProvider().equals(IFileDependencyProvider.class))
		{// we have static files
//			ContentType contentType = fileDependency.accessType() == FileAccess.ReadOnly ? 
//												ContentType.ROFILE : ContentType.RWFILE;
			ContentType contentType = ContentType.ROFILE;
			
			String[] fileNames = fileDependency.files();
			
			if(fileNames == null || fileNames.length == 0)
				return null;
			
			for(String filename : fileNames)
			{
				File file = RemoteClassLoaderUtils.getRelativePathFile(filename);
				if(!file.exists())
				{
					log.severe("Class "+clazz.getName()+" set file "+filename+" as required, but the file is missing at "+file.getAbsolutePath());
					continue;
				}
				
				files.add(new ClassLoaderFile(filename, file.lastModified(),file.length(), contentType));
			}
		}
		else
		{// we have dynamic file list, let's process it.
			Class<? extends IFileDependencyProvider> dependentFilesProviderClass = fileDependency.dependencyProvider();
			if(dependentFilesProviderClass == null)
				return null;
			
			//checking if we can create this class
			if(dependentFilesProviderClass.isInterface() ||  Modifier.isAbstract( dependentFilesProviderClass.getModifiers()))
				throw new JCloudScaleException("Class "+clazz.getName()+"is anotated with @FileDependency and has class "+dependentFilesProviderClass.getName()+" as dependency provider. However, dependency provider class is either abstract or interface.");
			
			if(dependentFilesProviderClass.getEnclosingClass() != null && !Modifier.isStatic(dependentFilesProviderClass.getModifiers()))
				throw new JCloudScaleException("Class "+clazz.getName()+"is anotated with @FileDependency and has class "+dependentFilesProviderClass.getName()+" as dependency provider. However, dependency provider class is internal and not static. The class has to be static in this case.");
			
			Constructor<? extends IFileDependencyProvider> constructor=null;
			try
			{
				constructor = dependentFilesProviderClass.getDeclaredConstructor();
			}
			catch(NoSuchMethodException ex)
			{
				throw new JCloudScaleException(ex, "Class "+clazz.getName()+"is anotated with @FileDependency and has class "+dependentFilesProviderClass.getName()+" as dependency provider. However, dependency provider class cannot be created as it has no parameterless constructor.");
			}
			
			try
			{
				if(!constructor.isAccessible())
					constructor.setAccessible(true);
				
				IFileDependencyProvider provider = constructor.newInstance();
				
				for(DependentFile dependentFile : provider.getDependentFiles())
				{
					File file = RemoteClassLoaderUtils.getRelativePathFile(dependentFile.filePath);
					if(!file.exists())
					{
						log.severe("Class "+clazz.getName()+" set file "+dependentFile.filePath+" as required, but the file is missing.");
						continue;
					}
					
//					ContentType contentType = dependentFile.accessType == FileAccess.ReadOnly ? 
//							ContentType.ROFILE : ContentType.RWFILE;
					ContentType contentType = ContentType.ROFILE;
					
					files.add(new ClassLoaderFile(file.getPath(), file.lastModified(),file.length(), contentType));
				}
			}
			catch(Exception ex)
			{
				log.severe("Dependent files provider "+dependentFilesProviderClass.getName()+" for class " +clazz.getName()+" threw exception during execution:"+ex.toString());
				throw new JCloudScaleException(ex, "Dependent files provider "+dependentFilesProviderClass.getName()+" for class " +clazz.getName()+" threw exception during execution.");
			}
		}
		
		return files;
	}
	
	/**
	 * @param files
	 * Sorts files in order: RO/RW files, Jars, Classes (keeping dependency order)
	 */
	protected void sortByDependency(List<ClassLoaderFile> files)
	{
		List<ClassLoaderFile> relatedFiles = new ArrayList<ClassLoaderFile>();
		List<ClassLoaderFile> jars = new ArrayList<ClassLoaderFile>();
		List<ClassLoaderFile> classes = new ArrayList<ClassLoaderFile>();
		
		//at first we categorize all files.
		for(ClassLoaderFile file : files)
		{
			switch(file.getType())
			{
				case CLASS:
					classes.add(file);
					break;
					
				case JAR:
					jars.add(file);
					break;
					
				case ROFILE:
//				case RWFILE:
				default:
					relatedFiles.add(file);
			}
		}
		
		files.clear();
		
		//then we add all related files and jars to result.
		files.addAll(relatedFiles);
		files.addAll(jars);
		
		// and now we try to sort classes in order of their dependencies.
		// as we can't use sorting for this (mostly classes are incomparable), 
		// we just go through classes and swap classes that are dependent.
		// TODO: find out some more optimal and better way to maintain this order.
		int cur = 0;
		while(cur < classes.size())
		{
			boolean conflict = false;//if we have conflict, we swap files and start again.
			for(int next = cur + 1; next < classes.size(); ++next)
			{
				if(isDependingOnOtherClass(classes.get(cur).getName(), classes.get(next).getName()))
				{
					conflict = true;
					
					ClassLoaderFile tmp = classes.get(cur);
					classes.set(cur,  classes.get(next));
					classes.set(next, tmp);
					
					break;
				}
			}
			
			if(!conflict)
				cur++;
		}		
		
		files.addAll(classes);
	}
	
	private boolean isDependingOnOtherClass(String current, String other) 
	{
		try 
		{
			Class<?> currentClass = Class.forName(current);
			Class<?> otherClass = Class.forName(other);
			
			if(otherClass.isAssignableFrom(currentClass))
				return true;
			
			if(currentClass.getEnclosingClass() != null)
				return isDependingOnOtherClass(currentClass.getEnclosingClass().getName(), other);

			return false;
		} 
		catch (ClassNotFoundException e) 
		{
			return false;
		}
	}
	
	//------------------------------------------------------------------------

	/**
	 * Collects all related files to the provided classname.
	 * @param classname The name of the class that files should be collected for
	 * @return The array of related files or null if collection failed.
	 */
	public abstract ClassLoaderOffer collectFilesForClass(String classname);
	
	/**
	 * Loads content of the file that is described by this instance.
	 * @throws IOException 
	 */
	public void loadContent(ClassLoaderFile file) throws IOException 
	{
		if(file.getContent() != null)
			return;

		switch(file.getType())
		{
		case CLASS:
			try(InputStream classByteStream = classloader.getResourceAsStream(RemoteClassLoaderUtils.convertClassToPath(file.getName())))
			{
				file.setContent(RemoteClassLoaderUtils.getByteArray(classByteStream));
			}
			break;
			
		case JAR:
			//we expect that path to the jar will be stored in the tag of the file.
			String filename = (String)file.getTag();
			if(filename == null || filename.length() == 0)
				filename = file.getName();
			try(InputStream stream = new FileInputStream(filename))
			{
				file.setContent(RemoteClassLoaderUtils.getByteArray(stream));
			}
			break;
			
		case ROFILE:
//		case RWFILE:
			try(InputStream stream = new FileInputStream(file.getName()))
			{
				file.setContent(RemoteClassLoaderUtils.getByteArray(stream));
			}
			break;
			
			default:
				log.severe("Unexpected type of file is asked to be loaded: " + file.getType());
		}
	}

	/**
	 * Returns a {@link ClassLoaderOffer} containing the requested file in the classpath, if possible.<br/>
	 * If the resource cannot be located, {@code null} is returned instead.
	 * 
	 * @param fileName the name of the resource
	 * @return the offer (can be {@code null})
	 */
	protected ClassLoaderOffer collectFile(String fileName) {
		try {
			ClassPathResource resource = new ClassPathResource(fileName);
			if (resource.exists()) {
				File file = resource.getFile();
				ClassLoaderFile classLoaderFile = new ClassLoaderFile(fileName, file.lastModified(), ContentType.ROFILE, Files.toByteArray(file));
				return new ClassLoaderOffer(null, 0, new ClassLoaderFile[]{classLoaderFile});
			}
		} catch (IOException e) {
			// ignore
		}
		return null;
	}
}
