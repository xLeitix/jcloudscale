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
package at.ac.tuwien.infosys.jcloudscale.migration.utility;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.infosys.jcloudscale.CloudInvocationInfos;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObjectId;
import at.ac.tuwien.infosys.jcloudscale.annotations.EventSink;
import at.ac.tuwien.infosys.jcloudscale.exception.IllegalDefinitionException;
import at.ac.tuwien.infosys.jcloudscale.migration.annotations.MigrationTransient;
import at.ac.tuwien.infosys.jcloudscale.migration.annotations.NoMigration;
import at.ac.tuwien.infosys.jcloudscale.migration.annotations.PostMigration;
import at.ac.tuwien.infosys.jcloudscale.migration.annotations.PreMigration;
import at.ac.tuwien.infosys.jcloudscale.migration.annotations.SerializationProvider;
import at.ac.tuwien.infosys.jcloudscale.migration.serialization.DefaultSerializationProvider;
import at.ac.tuwien.infosys.jcloudscale.migration.serialization.ISerializationProvider;

/**
 * Caches results of previously made calls to {@link MigrationUtil} for faster
 * subsequent lookups.
 */
public class MigrationUtilCacheDto implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Indicates if the class fulfills all legal requirements, as
	 * definied/checked in {@link MigrationUtil#isLegalMigrationDef(Class)}
	 */
	protected Boolean isLegal = null;

	/**
	 * Stores the exception thrown by
	 * {@link MigrationUtil#isLegalMigrationDef(Class)}, if any
	 */
	protected IllegalDefinitionException legalError = null;

	/**
	 * Indicates if the class is annotated with {@link NoMigration}
	 */
	protected Boolean isNoMigrate = null;

	/**
	 * Stores all methods of the class annotated with {@link PreMigration}
	 * ordered by their defined priority
	 * 
	 * @see PreMigration
	 */
	// Can't use a SortedSet because methods with the same priority are seen as
	// equal and thus can't be added to the set
	protected List<Method> sortedPreMigrate = null;

	/**
	 * Stores all methods of the class annotated with {@link PostMigration}
	 * ordered by their defined priority
	 * 
	 * @see PostMigration
	 */
	// Can't use a SortedSet because methods with the same priority are seen as
	// equal and thus can't be added to the set
	protected List<Method> sortedPostMigrate = null;

	/**
	 * Stores all fields of the class annotated with {@link MigrationTransient}
	 */
	protected Set<Field> transientFields = null;

	/**
	 * Stores all fields of the class NOT annotated with
	 * {@link MigrationTransient}
	 */
	protected Set<Field> nonTransientFields = null;

	/**
	 * Stores all fields of the class annotated with JCloudScale annotations.
	 * 
	 * @see CloudObjectId
	 * @see CloudInvocationInfos
	 * @see EventSink
	 */
	protected Set<Field> cloudScaleFields = null;

	/**
	 * Stores the serialization provider class to use for the class as specified
	 * by the {@link SerializationProvider} annotation. If the class isn't
	 * annotated with this annotation {@link DefaultSerializationProvider} is
	 * stored
	 */
	protected Class<? extends ISerializationProvider> serializationProvider = null;

}
