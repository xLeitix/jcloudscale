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

import java.io.IOException;
import java.io.InputStream;

import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;

/**
 * A {@link SerializationProvider} used to serialize and deserialize a
 * {@link CloudObject} during migration must implement this interface.
 * <p>
 * After preparation of the <code>CloudObject</code> for migration it is handed
 * over to the <code>SerializationProvider</code> specified for serialization.
 * The framework prepares a <code>CloudObject</code> for migration by calling
 * all of its {@link PreMigration} annotated methods and reseting all of its
 * {@link MigrationTransient} annotated fields. The <code>CloudObject</code> is
 * not altered in any other way, so annotations specific to the used
 * <code>SerializationProvider</code> are still in place and can be.
 * <p>
 * If a <code>CloudObject</code> doesn't specify a
 * <code>SerializationProvider</code> the default implementation is used.
 * 
 * @see DefaultSerializationProvider
 */
public interface ISerializationProvider {

	/**
	 * Used to serialize an <code>CloudObject</code>.
	 * 
	 * @param data
	 *            Object to be serialized.
	 * @return <code>InputStream</code> of the serialized object.
	 * @throws IOException
	 *             Thrown if an <code>IOException</code> occurs during
	 *             serialization.
	 */
	public InputStream serialize(Object data) throws IOException;

	/**
	 * Used to deserialize an previously serialized <code>CloudObject</code>.
	 * 
	 * @param data
	 *            <code>InputStream</code> to be deserialized.
	 * @return The deserialized object.
	 * @throws IOException
	 *             Thrown if an <code>IOException</code> occurs during
	 *             deserialization.
	 * @throws ClassNotFoundException
	 *             Thrown if the class of the object to derserialize isn't
	 *             found.
	 */
	public Object deserialize(InputStream data) throws IOException,
			ClassNotFoundException;

}
