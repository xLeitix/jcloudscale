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
package at.ac.tuwien.infosys.jcloudscale.classLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RemoteClassLoaderUtils
{
	//------------------------------------------------------------
	
	/**
	 * Converts input steam to byte array. (Is there any other simpler way?)
	 */
	public static byte[] getByteArray(InputStream is) throws IOException
	{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = is.read(data, 0, data.length)) != -1)
		{
			buffer.write(data, 0, nRead);
		}

		buffer.flush();

		return buffer.toByteArray();
	}
	
	//-----------------------------------------------------------
	
	private static final char CLASSPATH_SEPARATOR_CHAR = '/';
	
	/**
	 * Detects whether provided path represents jar.
	 */
	public static boolean isJarFile(String filePath)
	{
		if(filePath == null || filePath.length() == 0)
			return false;
		
		return filePath.toLowerCase().endsWith(".jar");
	}
	
	/**
	 * Detects whether provided path represents class.
	 */
	public static boolean isClassName(String objectName)
	{
		if(objectName == null)
			return false;
		
		return objectName.endsWith(".class");
	}
	
	/**
	 * Converts the class to the path by which it can be retrieved.
	 */
	public static String convertClassToPath(Class<?> clazz)
	{
		return clazz.getName().replace('.', CLASSPATH_SEPARATOR_CHAR) + ".class";
	}
	
	/**
	 * Converts the name of the provided class to the path by which it can be retrieved.
	 */
	public static String convertClassToPath(String className)
	{
		return className.replace('.', CLASSPATH_SEPARATOR_CHAR) + ".class";
	}
	
	/**
	 * Converts the path to the file to the class name.
	 */
	public static String convertClassPathToClassName(String classPath)
	{
		return classPath.replace(CLASSPATH_SEPARATOR_CHAR, '.').replaceAll(".class$", "");
	}
	
	/**
	 * Converts the provided class to the full class name with the ".class" at the end.
	 */
	public static String convertClassToFullName(Class<?> clazz)
	{
		return clazz.getName()+".class";
	}
	
	public static String getContainingFile(Class<?> clazz)
	{
		URL location = clazz.getProtectionDomain().getCodeSource().getLocation();
		
		if(location == null)
			return null;
		
		String path = location.getPath();
		if (!isJarFile(path))
			path += convertClassToPath(clazz);

		return path;
	}
	
	/**
	 * Loads the class with the provided classloader.
	 * @param className The full-name of the class to load
	 * @param classloader The classloader to use for classloading
	 * @return The class object that represents the specified class or 
	 * null if class loading failed.
	 */
	public static Class<?> getClass(String className, ClassLoader classloader)
	{
		try 
		{
			// using this version causes problems with arrays, 
			// as they are provided as [LclassName, what breaks class loader.
			//classloader.loadClass(className); http://bugs.sun.com/view_bug.do?bug_id=6434149
			return Class.forName(className, false, classloader);

		} 
		catch (ClassNotFoundException e) 
		{
			return null;//we will just return null.
		}
	}
	
	//-----------------------------------------------------------

	/**
	 * Deletes recursively specified folder or file.
	 * If folder cannot be deleted, its deletion is postponed to the JavaMV shutdown.
	 * @param dir  The folder to delete recursively.
	 */
	public static void deleteFolderRec(File dir)
	{
		if(dir == null || !dir.exists() || !dir.isDirectory())
			return;
		
		for (File file : dir.listFiles())
			if (file.isDirectory())
				deleteFolderRec(file);
			else if (!file.delete())
				file.deleteOnExit();

		if (!dir.delete())
			dir.deleteOnExit();
	}

	/**
	 * Creates all required directories for the provided file.
	 */
	public static boolean mkDirRec(File file)
	{
		if(file == null)
			return true;
		
		if (!file.exists())
		{
			if (!mkDirRec(file.getParentFile()))
				return false;

			if (!file.mkdir())
				return false;
		}

		if (!file.isDirectory())
			return false;

		return true;
	}
	
	/**
	 * Makes provided path relative to the current working directory.
	 */
	public static File getRelativePathFile(String fileName) 
	{
		Path filePath = Paths.get(fileName);
		if(filePath.isAbsolute())
		{
			Path currentPath = Paths.get(".");
			return currentPath.relativize(filePath).toFile();
		}
		else
			return filePath.toFile();
	}
	
	private static final CharSequence SEPARATOR_REPLACEMENT = "$%$";
	
	public static String escapeFilePath(String fileName)
	{
		return fileName.replace(File.separator, SEPARATOR_REPLACEMENT)
				.replace("/", SEPARATOR_REPLACEMENT);
	}
	
	public static String unescapeFilePath(String escapedFileName)
	{
		return escapedFileName.replace(SEPARATOR_REPLACEMENT, File.separator);
	}
}
