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
package at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type;

import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.Adapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.Adapters;
import at.ac.tuwien.infosys.jcloudscale.datastore.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TypeMetadataImpl<T> implements TypeMetadata<T> {

    private Class<T> targetClass;

    /**
     * The parent object (owner) of the field
     */
    private Object parent;

    /**
     * The type adapter factory to use
     */
    private TypeAdapterFactory<T> typeAdapterFactory;

    /**
     * The name of the field
     */
    private String fieldName;

    /**
     * The type of the field
     */
    private Class<?> fieldType;

    /**
     * The classes of the type parameters if generic
     */
    private List<Class<?>> typeParameterTypes;

    /**
     * The type adapter for the field
     */
    private TypeAdapter<?, T> typeAdapter;

    /**
     * The type adapters for the type parameters if generic
     */
    private List<TypeAdapter<?, T>> typeParameterTypeAdapters;

    /**
     * Determines if the type adapter is a custom one
     */
    private boolean isCustomTypeAdapterPresent;

    /**
     * Determines if the class of the field is java internal
     */
    private boolean isJavaInternalClass;

    //Hide default constructor
    private TypeMetadataImpl() {
    }

    public TypeMetadataImpl(Object parent, Field field, TypeAdapterFactory<T> typeAdapterFactory, Class<T> targetClass) {
        this.targetClass = targetClass;
        this.parent = parent;
        this.typeAdapterFactory = typeAdapterFactory;
        this.fieldName = field.getName();
        this.fieldType = field.getType();
        this.typeAdapter = getTypeAdapterForField(field);
        this.typeParameterTypeAdapters = getTypeParameterTypeAdaptersForField(field);
        this.isJavaInternalClass = ReflectionUtil.isJavaInternalType(field);
    }

    public <K> TypeMetadataImpl(TypeMetadata<K> typeMetadata, Field field, TypeAdapterFactory<T> typeAdapterFactory, Class<T> targetClass) {
        this.targetClass = targetClass;
        this.parent = typeMetadata.getParent();
        this.typeAdapterFactory = typeAdapterFactory;
        this.fieldName = typeMetadata.getFieldName();
        this.fieldType = typeMetadata.getFieldType();
        this.typeAdapter = getTypeAdapterForField(field);
        this.typeParameterTypeAdapters = getTypeParameterTypeAdaptersForField(field);
        this.isJavaInternalClass = typeMetadata.isJavaInternalClass();
    }

    @Override
    public Object getParent() {
        return this.parent;
    }

    @Override
    public String getFieldName() {
        return this.fieldName;
    }

    @Override
    public Class<?> getFieldType() {
        return fieldType;
    }

    @Override
    public List<Class<?>> getTypeParameterTypes() {
        return typeParameterTypes;
    }

    @Override
    public boolean isCustomTypeAdapterPresent() {
        return this.isCustomTypeAdapterPresent;
    }

    @Override
    public TypeAdapter<?, T> getTypeAdapter() {
        return this.typeAdapter;
    }

    @Override
    public List<TypeAdapter<?, T>> getTypeParameterTypeAdapters() {
        return typeParameterTypeAdapters;
    }

    @Override
    public boolean isJavaInternalClass() {
        return this.isJavaInternalClass;
    }

    private TypeAdapter<?, T> getTypeAdapterForField(Field field) {
        TypeAdapter<?, T> typeAdapter = getCustomTypeAdapterIfPresent(field);
        if(typeAdapter != null) {
            isCustomTypeAdapterPresent = true;
            return typeAdapter;
        } else {
            isCustomTypeAdapterPresent = false;
            return typeAdapterFactory.get(field.getType());
        }
    }

    private TypeAdapter<?, T> getCustomTypeAdapterIfPresent(Field field) {
        Adapters adapters = field.getAnnotation(Adapters.class);
        if(adapters != null) {
            Class<? extends TypeAdapter<?, ?>> customAdapterClass = getCorrespondingTypeAdapter(adapters.value());
            if(customAdapterClass != null) {
                TypeAdapter typeAdapter = ReflectionUtil.createInstance(customAdapterClass);
                return typeAdapter;
            }
        }
        return null;
    }

    private List<TypeAdapter<?, T>> getTypeParameterTypeAdaptersForField(Field field) {
        if(List.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType())) {
            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            typeParameterTypes = convertTypeArguments(parameterizedType.getActualTypeArguments());
            return createTypeParameterAdapters();
        }
        return null;
    }

    private List<Class<?>> convertTypeArguments(Type[] typeArguments) {
        List<Class<?>> parameterTypes = new ArrayList<Class<?>>();
        for(Type type : typeArguments) {
            parameterTypes.add((Class<?>) type);
        }
        return parameterTypes;
    }

    private List<TypeAdapter<?, T>> createTypeParameterAdapters() {
        List<TypeAdapter<?, T>> typeAdapters = new ArrayList<TypeAdapter<?, T>>();
        int typeParameterIndex = 0;
        for(Class<?> typeParameterType : typeParameterTypes) {
            if(ReflectionUtil.isJavaInternalType(typeParameterType)) {
                typeAdapters.add(typeAdapterFactory.get(typeParameterType));
            } else {
                typeAdapters.add(typeAdapterFactory.getTypeParameterTypeAdapter(typeParameterIndex));
            }
            typeParameterIndex++;
        }
        return typeAdapters;
    }

    private Class<? extends TypeAdapter<?, ?>> getCorrespondingTypeAdapter(Adapter[] adapters) {
        for(Adapter adapter : adapters) {
            if(targetClass.equals(adapter.targetClass())) {
                return adapter.adapterClass();
            }
        }
        return null;
    }
}
