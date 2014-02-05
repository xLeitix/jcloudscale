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
 * {@link CloudObject}'s methods annotated with PreMigration are executed after
 * a successful migration.
 * <p>
 * If multiple methods are annotated they will be executed sequential in no
 * particular order, unless a <code>priority</code> value is specified.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented()
public @interface PreMigration {

	/**
	 * Specifies the execution priority of the annotated method. Methods with a
	 * lower priority value are executed first.
	 */
	int priority() default Integer.MAX_VALUE;

}
