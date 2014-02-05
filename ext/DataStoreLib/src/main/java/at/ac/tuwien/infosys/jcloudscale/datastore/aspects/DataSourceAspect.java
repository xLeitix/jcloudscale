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
package at.ac.tuwien.infosys.jcloudscale.datastore.aspects;


import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.DataSource;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.core.DatastoreFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.lang.reflect.Field;

/**
 * Aspect to inject datastore into fields annotated with @DataSource
 */
@Aspect
public class DataSourceAspect {

    @Before("get(@at.ac.tuwien.infosys.jcloudscale.datastore.annotations.DataSource * *.*)")
    public void injectDataSource(JoinPoint joinPoint) {
        try {
            Field field = getFieldFromJoinPoint(joinPoint);
            DataSource dataSource = field.getAnnotation(DataSource.class);
            Datastore datastore = getDatastoreForDataSource(dataSource);
            setDatastoreOnField(joinPoint.getTarget(), field, datastore);
        } catch (NoSuchFieldException ne) {
            throw new DatastoreException("Error injecting datasource. Field not found. " + ne.getMessage());
        } catch (IllegalAccessException ie) {
            throw new DatastoreException("Error injecting datasource. Illegal Access. " + ie.getMessage());
        }
    }

    private Field getFieldFromJoinPoint(JoinPoint joinPoint) throws NoSuchFieldException {
        Signature signature = joinPoint.getSignature();
        Class<?> clazz = signature.getDeclaringType();
        return clazz.getDeclaredField(signature.getName());
    }

    private Datastore getDatastoreForDataSource(DataSource dataSource) {
        String datastoreName = dataSource.name();
        return DatastoreFactory.getInstance().getDatastoreByName(datastoreName);
    }

    private void setDatastoreOnField(Object target, Field field, Datastore datastore) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(target, datastore);
    }
}
