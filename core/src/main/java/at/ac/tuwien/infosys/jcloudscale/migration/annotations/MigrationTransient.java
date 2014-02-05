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

/**
 * {@link CloudObject}'s fields annotated with <code>MigrationTransient</code>
 * are set to <code>null</code> prior migration.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MigrationTransient
{

	/**
	 * If a complex type field should be initialized by the framework after
	 * migration an implementation class must be specified otherwise these
	 * fields are initialized with with their declaration value.
	 * <p>
	 * The provided implementation class must define a default constructor
	 * otherwise an exception is raised during initialization.
	 */
	Class<?> initialize() default Class.class;
}
