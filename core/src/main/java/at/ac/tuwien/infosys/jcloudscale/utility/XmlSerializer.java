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
package at.ac.tuwien.infosys.jcloudscale.utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public class XmlSerializer 
{
	public static void serialize(Object obj, String xmlFile, String classesFile)
	{
		if(obj == null)
			throw new InvalidParameterException("object to serialize is null.");
		
		if(xmlFile == null)
			throw new InvalidParameterException("file path to serialize into is null");
		
		if(classesFile == null)
			throw new InvalidParameterException("file path to serialize class information into is null");
		
		// if we need to save them to a single file
		if(classesFile.equals(xmlFile))
		{
			serializeToFile(obj, xmlFile);
			return;
		}
		
		Class<?>[] knownClasses = collectClasses(obj);
		
		//
		// writing classes file
		//
		try(FileWriter writer = new FileWriter(classesFile))
		{
			writer.write(serializeKnownClasses(knownClasses));
		}
		catch(IOException ex)
		{
			throw new RuntimeException("Failed to save class descriptor file to "+classesFile,ex);
		}
		
		//
		// writing classes file
		//
		try(FileWriter writer = new FileWriter(xmlFile))
		{
			JAXBContext context = JAXBContext.newInstance(knownClasses);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			
			marshaller.marshal(obj, writer);
		}
		catch(JAXBException ex)
		{
			throw new RuntimeException("Failed to serialize object:", ex);
		}
		catch(IOException ex)
		{
			throw new RuntimeException("Failed to save object to file "+classesFile,ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T deserialize(String xmlFile, String classesFile, Class<T> clazz)
	{
		if(xmlFile == null)
			throw new InvalidParameterException("xml filename is null.");
		
		if(classesFile == null)
			throw new InvalidParameterException("class descriptor filename is null.");
		
		if(clazz == null)
			throw new InvalidParameterException("class is null.");
		
		if(xmlFile.equals(classesFile))
			return deserializeFromFile(xmlFile, clazz);
		
		//
		// Deserializing classes
		//
		Class<?>[] knownClasses = null;
		try(BufferedReader reader = new BufferedReader(new FileReader(classesFile)))
		{
			String classesLine = reader.readLine();
			if(classesLine == null)
				throw new RuntimeException("Failed to read classes descriptor: either file "+classesFile+" is damaged or empty.");
			
			knownClasses = deserializeKnownClasses(classesLine);
		}
		catch(IOException | ClassNotFoundException ex)
		{
			throw new RuntimeException("Failed to decode class description from file  "+ classesFile, ex);
		}

		//
		// Deserializing xml
		//
		try(FileReader reader = new FileReader(xmlFile))
		{
			JAXBContext context = JAXBContext.newInstance(knownClasses);
			
			Object obj = context.createUnmarshaller().unmarshal(reader);
			if(!obj.getClass().equals(clazz))
				throw new RuntimeException("Deserialized object is of type "+obj.getClass()+" while "+clazz +" is expected.");
			
			return (T)obj;
		}
		catch(IOException | JAXBException ex)
		{
			throw new RuntimeException("Failed to deserialize xml data from file "+xmlFile, ex);
		}
	}
	
	public static void serializeToFile(Object obj, String file)
	{
		if(obj == null)
			throw new InvalidParameterException("object to serialize is null.");
		
		if(file == null)
			throw new InvalidParameterException("file path to serialize into is null");
		
		Class<?>[] knownClasses = collectClasses(obj);
		
		try(FileWriter writer = new FileWriter(file))
		{
			// writing collected classes.
			writer.write(serializeKnownClasses(knownClasses));
			writer.write(System.lineSeparator());
			
			// writing xml.
			JAXBContext context = JAXBContext.newInstance(knownClasses);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			
			marshaller.marshal(obj, writer);
		}
		catch(JAXBException ex)
		{
			throw new RuntimeException("Failed to serialize provided object "+obj +" of type "+obj.getClass(), ex);
		}
		catch(IOException ex)
		{
			throw new RuntimeException("Failed to save data to file " +file);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T deserializeFromFile(String filename, Class<T> clazz) 
	{
		if(filename == null)
			throw new InvalidParameterException("filename is null.");
		
		try(BufferedReader reader = new BufferedReader(new FileReader(filename)))
		{
			String knownClassesLine = reader.readLine();
			if(knownClassesLine == null)
				throw new RuntimeException("Failed to read classes descriptor: either file "+filename+" is damaged or empty.");
			
			Class<?>[] knownClasses = deserializeKnownClasses(knownClassesLine);
			
			JAXBContext context = JAXBContext.newInstance(knownClasses);
			
			Object obj = context.createUnmarshaller().unmarshal(reader);
			if(!obj.getClass().equals(clazz))
				throw new RuntimeException("Deserialized object is of type "+obj.getClass()+" while "+clazz +" is expected.");
			
			return (T)obj;
		}
		catch(JAXBException | ClassNotFoundException ex)
		{
			throw new RuntimeException("Failed to deserialize data from file "+ filename, ex);
		}
		catch(IOException ex)
		{
			throw new RuntimeException("Failed to read data from file "+filename);
		}
	}
	
	private static String serializeKnownClasses(Class<?>[] knownClasses) 
	{
		if(knownClasses.length == 0)
			return "";
		
		StringBuilder builder = new StringBuilder();
		builder.append(knownClasses[0].getName());
		
		for(int i=1; i<knownClasses.length; ++i)
		{
			builder.append(", ");
			builder.append(knownClasses[i].getName());
		}
		
		return builder.toString();
		
	}
	
	private static Class<?>[] deserializeKnownClasses(String knownClasses) throws ClassNotFoundException
	{
		List<Class<?>> result = new ArrayList<Class<?>>();
		
		for(String className : knownClasses.split(", "))
			result.add(Class.forName(className));
		
		return result.toArray(new Class<?>[result.size()]);
	}

	private static Class<?>[] collectClasses(Object obj) 
	{
		try
		{
			Set<Class<?>> collectedClasses = new HashSet<>();
			collectedClasses.add(obj.getClass());
			collectClassesRec(obj, collectedClasses, new HashSet<>());
			
			
			return collectedClasses.toArray(new Class<?>[collectedClasses.size()]);
		}
		catch(IllegalAccessException ex)
		{
			throw new RuntimeException("Failed to collect classes from object "+obj+" of type "+obj.getClass(), ex);
		}
	}

	private static void collectClassesRec(Object obj, Set<Class<?>> collectedClasses, Set<Object> visitedObjects) throws IllegalArgumentException, IllegalAccessException 
	{
		if(visitedObjects.contains(obj))
			return;
		
		visitedObjects.add(obj);
			
		Field[] fields = obj.getClass().getDeclaredFields();
		
		for(Field field : fields)
		{
			if(!field.isAccessible())
				field.setAccessible(true);
			
			Object fieldValue = field.get(obj);
			if(fieldValue == null)
				continue;
			
			Class<?> fieldValueClass = fieldValue.getClass();
			
			if(isSystemClass(fieldValueClass) || Modifier.isTransient(field.getModifiers()))
				continue;
			
			if(!fieldValueClass.equals(field.getType()))
				collectedClasses.add(fieldValueClass);
			
			collectClassesRec(fieldValue, collectedClasses, visitedObjects);
		}
	}

	private static final Set<Class<?>> systemClasses = collectSystemClasses();
	
	private static Set<Class<?>> collectSystemClasses() 
	{
		Set<Class<?>> res = new HashSet<Class<?>>();
		// primitive wrappers classes:
		res.add(Boolean.class);
        res.add(Character.class);
        res.add(Byte.class);
        res.add(Short.class);
        res.add(Integer.class);
        res.add(Long.class);
        res.add(Float.class);
        res.add(Double.class);
        res.add(Void.class);
        res.add(String.class);
        
        // collections:
        res.add(List.class);
        res.add(ArrayList.class);
        res.add(Map.class);
        res.add(HashMap.class);
        res.add(Set.class);
        res.add(HashSet.class);
        
        return res;   
	}
	
	private static boolean isSystemClass(Class<?> clazz) 
	{
		
		if(systemClasses.contains(clazz))
			return true;
		
		if(clazz.isEnum() || clazz.isPrimitive())
			return true;
		
		return false;
	}
}
