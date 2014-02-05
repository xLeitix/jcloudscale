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


import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.DatastoreLib;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.LibWrapper;
import at.ac.tuwien.infosys.jcloudscale.datastore.core.DatastoreFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.lang.reflect.Field;

/**
 * Aspect to inject external datastore libs into fields annotated with @DatastoreLib
 */
@Aspect
public class DatastoreLibAspect {

    @Before("get(@at.ac.tuwien.infosys.jcloudscale.datastore.annotations.DatastoreLib * *.*)")
    public void injectDataSource(JoinPoint joinPoint) {
        try {
            Field field = getFieldFromJoinPoint(joinPoint);
            DatastoreLib datastoreLib = field.getAnnotation(DatastoreLib.class);
            Datastore datastore = getDatastoreForDatastoreLib(datastoreLib);
            LibWrapper libWrapper = getWrapperForDatastoreLib(datastoreLib, datastore);
            setLibWrapperField(joinPoint.getTarget(), field, libWrapper, datastore);
        } catch (NoSuchFieldException ne) {
            throw new DatastoreException("Error injecting datastore lib. Field not found. " + ne.getMessage());
        } catch (IllegalAccessException ie) {
            throw new DatastoreException("Error injecting datastore lib. Illegal Access. " + ie.getMessage());
        }
    }

    private Field getFieldFromJoinPoint(JoinPoint joinPoint) throws NoSuchFieldException {
        Signature signature = joinPoint.getSignature();
        Class<?> clazz = signature.getDeclaringType();
        return clazz.getDeclaredField(signature.getName());
    }

    private Datastore getDatastoreForDatastoreLib(DatastoreLib datastoreLib) {
        String datastoreName = datastoreLib.datastore();
        return DatastoreFactory.getInstance().getDatastoreByName(datastoreName);
    }

    private LibWrapper getWrapperForDatastoreLib(DatastoreLib datastoreLib, Datastore datastore) {
        String libName = datastoreLib.name();
        return datastore.getExternalLibrary(libName);
    }

    private void setLibWrapperField(Object target, Field field, LibWrapper libWrapper, Datastore datastore) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(target, libWrapper.getLib(datastore));
    }
}
