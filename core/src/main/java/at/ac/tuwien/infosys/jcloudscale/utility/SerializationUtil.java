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
package at.ac.tuwien.infosys.jcloudscale.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.server.CustomCLObjectInputStream;

public class SerializationUtil {
	
	public static Object[] getObjectArrayFromBytes(byte[] bytes, ClassLoader classLoader) throws IOException, ClassNotFoundException 
	{
		try(ObjectInputStream objectstream = new CustomCLObjectInputStream(new ByteArrayInputStream(bytes), classLoader))
		{
			return (Object[]) objectstream.readObject();
		}
	}
	
	public static Object getObjectFromBytes(byte[] bytes, ClassLoader classLoader) throws IOException, ClassNotFoundException 
	{
		try(ObjectInputStream objectstream = new CustomCLObjectInputStream(new ByteArrayInputStream(bytes), classLoader))
		{
			return objectstream.readObject();
		}
	}
	
	public static byte[] serializeToByteArray(Object object) throws IOException 
	{
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput out = new ObjectOutputStream(bos))
		{
		    out.writeObject(object);
		    return bos.toByteArray();
		}
	}
	
	public static byte[] serializeToByteArray(Object[] objects) throws IOException {
		
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
		ObjectOutput out = new ObjectOutputStream(bos);)
		{
			out.writeObject(objects);
		    return bos.toByteArray();
		}
	}
	
	public static String base64Encode(byte[] bytes) {
		return DatatypeConverter.printBase64Binary(bytes);
	}
	
	public static byte[] base64Decode(String base64) {
		return DatatypeConverter.parseBase64Binary(base64);
	} 
	
	public static String serializeToString(Object obj) throws IOException
	{
		if(obj == null || !obj.getClass().isAnnotationPresent(XmlRootElement.class))
			return null;
		
		try(StringWriter writer = new StringWriter())
		{
			try
			{
				JAXBContext ctx = JAXBContext.newInstance(obj.getClass());
				ctx.createMarshaller().marshal(obj, writer);
				return writer.toString();
			}
			catch(JAXBException ex)
			{//let's just ignore.
			}
		}
		
		return null;
	}
	
	public static Object deserialize(String data, String className)
	{
		if(className == null || className.length() == 0)
			return null;
		
		if(data != null && data.length() > 0)
		{			
			try(StringReader reader = new StringReader(data))
			{
				try
				{
					JAXBContext ctx = JAXBContext.newInstance(Class.forName(className));
					Unmarshaller um = ctx.createUnmarshaller();
					return um.unmarshal(reader);
				}
				catch(JAXBException | ClassNotFoundException ex)
				{//let's just ignore.
				}
			}
		} 
		else
		{
			try 
			{
				return Class.forName(className).newInstance();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) 
			{
			}
		}
		
		return null;
	}
	
}
