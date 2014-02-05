/*
   Copyright 2013 Rene Nowak 

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
package at.ac.tuwien.infosys.jcloudscale.datastore.mapping;

import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.DatastoreId;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeMetadata;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeMetadataImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

public class ObjectNavigatorImpl<T> implements ObjectNavigator {

    private Logger log;
    private TypeAdapterFactory<T> typeAdapterFactory;

    public ObjectNavigatorImpl(TypeAdapterFactory<T> typeAdapterFactory) {
        this.log = JCloudScaleConfiguration.getLogger(this);
        this.typeAdapterFactory = typeAdapterFactory;
    }

    @Override
    public void navigate(Object object, Visitor visitor) {
        Preconditions.notNull(visitor);

        //Ignore null objects
        if(object == null) {
            return;
        }

        log.fine("Navigating through object " + object);

        //iterate fields
        Field[] fields = object.getClass().getDeclaredFields();
        for(Field field : fields) {
            if(!isIdField(field) && !isInnerClassReference(object.getClass(), field) && !isStaticOrFinal(field)) {
                Object fieldValue = ReflectionUtil.getFieldValue(field, object);
                TypeMetadata<T> typeMetadata = new TypeMetadataImpl(object, field, typeAdapterFactory, visitor.getTargetClass());
                visitor.visit(fieldValue, typeMetadata);
            }
        }
    }

    private boolean isIdField(Field field) {
        return field.isAnnotationPresent(DatastoreId.class);
    }

    private boolean isInnerClassReference(Class<?> clazz, Field field) {
        return !Modifier.isStatic(clazz.getModifiers()) && clazz.getEnclosingClass() != null && field.getName().startsWith("this$");
    }

    private boolean isStaticOrFinal(Field field) {
        return Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers());
    }
}
