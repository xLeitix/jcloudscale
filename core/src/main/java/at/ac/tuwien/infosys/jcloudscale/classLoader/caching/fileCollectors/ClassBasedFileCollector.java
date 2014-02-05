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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import at.ac.tuwien.infosys.jcloudscale.classLoader.RemoteClassLoaderUtils;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderFile;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ClassLoaderOffer;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto.ContentType;

/**
 * @author rst
 * This file collector collects only the requested class and files it relates on.
 */
public class ClassBasedFileCollector extends FileCollectorAbstract
{
	@Override
	public ClassLoaderOffer collectFilesForClass(String classname) 
	{
		// retrieving class itself
		Class<?> clazz = RemoteClassLoaderUtils.getClass(classname, classloader);
		if(clazz == null)
		{
			ClassLoaderOffer offer = collectFile(classname);
			if (offer == null) {
				log.severe("Class " + classname + " was requested, but we don't have such class here. Refusing all offers.");
			} 
			return offer;
		}

		// preparing bytecode
		byte[] bytecode = null;
		try 
		{
			InputStream classCodeStream = classloader.getResourceAsStream(
							RemoteClassLoaderUtils.convertClassToPath(clazz));
			if(classCodeStream == null)
			{// should not happen.
				log.severe("Class "+classname+" was requested, but can't get resource stream. Looks like a bug... Refusing all offers.");
				return null;
			}
			bytecode = RemoteClassLoaderUtils.getByteArray(classCodeStream);
		} 
		catch (IOException e) 
		{
			log.severe("Class "+classname+" was requested, but can't get bytecode from resource stream. Exception:"+e.toString());
			return null;
		}

		// checking if this class has file dependencies
		List<ClassLoaderFile> requiredFiles = collectRequiredFiles(clazz);
		
		// creating class loader file object that describes our class. 
		ClassLoaderFile classDescribingFile = new ClassLoaderFile(classname, 
				new File(RemoteClassLoaderUtils.getContainingFile(clazz)).lastModified(),
				ContentType.CLASS, bytecode);
		
		// if we have other files, we add our file there. Otherwise we return it alone.
		if(requiredFiles != null)
		{
			requiredFiles.add(classDescribingFile);
			
			ClassLoaderOffer offer = new ClassLoaderOffer(null, 0, requiredFiles.toArray(new ClassLoaderFile[requiredFiles.size()]));
			offer.setClassesWithFiles(new String[]{classDescribingFile.getName()});
			
			return offer;
		}
		else
			return new ClassLoaderOffer(null, 0, new ClassLoaderFile[]{classDescribingFile});
	}
}
