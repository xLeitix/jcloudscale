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
package at.ac.tuwien.infosys.jcloudscale.migration.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.migration.serialization.DefaultSerializationProvider;
import at.ac.tuwien.infosys.jcloudscale.migration.serialization.ISerializationProvider;

/**
 * Sets the {@link ISerializationProvider} to use for serializing the
 * {@link CloudObject} during migration.
 * <p>
 * If no <code>ISerializationProvider</code> is set the
 * {@link DefaultSerializationProvider} is used.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SerializationProvider {

	/**
	 * The SerializationProvider to use. The given class must implement
	 * {@link ISerializationProvider}.
	 */
	Class<? extends ISerializationProvider> value() default DefaultSerializationProvider.class;
}
