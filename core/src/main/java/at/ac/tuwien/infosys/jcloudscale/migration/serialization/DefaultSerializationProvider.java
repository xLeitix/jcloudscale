/*
   Copyright 2013 Fritz Schrogl

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
package at.ac.tuwien.infosys.jcloudscale.migration.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.server.CustomCLObjectInputStream;

/**
 * Default implementation of an {@link ISerializationProvider} used to serialize
 * a {@link CloudObject} during migration.
 * <p>
 * If a <code>CloudObject</code> isn't annotated with
 * {@link SerializationProvider} to specify an {@link ISerializationProvider} to
 * use for serialization this default implementation is used.
 */
public class DefaultSerializationProvider implements ISerializationProvider 
{
	/**
	 * Classloader that has to be used to deserialize classes.
	 */
	private ClassLoader classLoader = ClassLoader.getSystemClassLoader();
	
	public DefaultSerializationProvider(){}
	
	public DefaultSerializationProvider(Class<?> clazz)
	{
		if(clazz != null)
			classLoader = clazz.getClassLoader();
	}
	
	/**
	 * @see ISerializationProvider#serialize(Object)
	 */
	@Override
	public InputStream serialize(Object data) throws IOException 
	{
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);)
		{
			oos.writeObject(data);
			return new ByteArrayInputStream(bos.toByteArray());
		}
	}

	/**
	 * @see ISerializationProvider#deserialize(InputStream)
	 */
	@Override
	public Object deserialize(InputStream data) throws IOException,
			ClassNotFoundException {
		//
		// now we use here custom ObjectInputStream to be able to provide classes
		// from custom classloader.
		//
		try(ObjectInputStream ois = new CustomCLObjectInputStream(data, classLoader))
		{
			return ois.readObject();
		}
	}

}
