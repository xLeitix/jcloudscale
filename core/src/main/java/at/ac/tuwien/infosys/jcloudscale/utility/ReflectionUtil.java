/*
   Copyright 2013 Philipp Leitner

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
package at.ac.tuwien.infosys.jcloudscale.utility;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

import at.ac.tuwien.infosys.jcloudscale.CloudInvocationInfos;
import at.ac.tuwien.infosys.jcloudscale.InvocationInfo;
import at.ac.tuwien.infosys.jcloudscale.annotations.ByValueParameter;
import at.ac.tuwien.infosys.jcloudscale.annotations.ClientObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObjectId;
import at.ac.tuwien.infosys.jcloudscale.annotations.EventSink;
import at.ac.tuwien.infosys.jcloudscale.exception.IllegalDefinitionException;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.monitoring.DefaultEventSink;
import at.ac.tuwien.infosys.jcloudscale.monitoring.IEventSink;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;

import com.google.common.primitives.Primitives;

public class ReflectionUtil {

    /**
     * Returns a {@link Constructor} object that reflects the specified constructor of the given type.
     * <p/>
     * If no parameter types are specified i.e., {@code paramTypes} is {@code null} or empty, the default constructor
     * is returned.<br/>
     * If a parameter type is not known i.e., it is {@code null}, all declared constructors are checked whether their
     * parameter types conform to the known parameter types i.e., if every known type is assignable to the parameter
     * type of the constructor and if exactly one was found, it is returned.<br/>
     * Otherwise a {@link NoSuchMethodException} is thrown indicating that no or several constructors were found.
     *
     * @param type the class
     * @param paramTypes the full-qualified class names of the parameters (can be {@code null})
     * @param classLoader the class loader to use
     * @return the accessible constructor resolved
     * @throws NoSuchMethodException if there are zero or more than one constructor candidates
     * @throws ClassNotFoundException if a class cannot be located by the specified class loader
     */
    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> findConstructor(Class<T> type, Class<?>... clazzes) throws NoSuchMethodException, ClassNotFoundException {
        Constructor<T> constructor = null;

        // If all parameter types are known, find the constructor that exactly matches the signature
        if (!ArrayUtils.contains(clazzes, null)) {
            try {
                constructor = type.getDeclaredConstructor(clazzes);
            } catch (NoSuchMethodException e) {
                // Ignore
            }
        }

        // If no constructor was found, find all possible candidates
        if (constructor == null) {
            List<Constructor<T>> candidates = new ArrayList<>(1);
            for (Constructor<T> declaredConstructor : (Constructor<T>[]) type.getDeclaredConstructors()) {
                if (ClassUtils.isAssignable(clazzes, declaredConstructor.getParameterTypes())) {

                    // Check if there is already a constructor method with the same signature
                    for (int i = 0; i < candidates.size(); i++) {
                        Constructor<T> candidate = candidates.get(i);
                        /**
                         * If all parameter types of constructor A are assignable to the types of constructor B
                         * (at least one type is a subtype of the corresponding parameter), keep the one whose types
                         * are more concrete and drop the other one.
                         */
                        if (ClassUtils.isAssignable(declaredConstructor.getParameterTypes(), candidate.getParameterTypes())) {
                            candidates.remove(candidate);
                            i--;
                        } else if (ClassUtils.isAssignable(candidate.getParameterTypes(), declaredConstructor.getParameterTypes())) {
                            declaredConstructor = null;
                            break;
                        }
                    }

                    if (declaredConstructor != null) {
                        candidates.add(declaredConstructor);
                    }
                }
            }
            if (candidates.size() != 1) {
                throw new NoSuchMethodException(String.format("Cannot find distinct constructor for type '%s' with parameter types %s",
                        type, Arrays.toString(clazzes)));
            }
            constructor = candidates.get(0);
        }

        //do we really need this dependency?
        //ReflectionUtils.makeAccessible(constructor);
        if(constructor != null && !constructor.isAccessible())
            constructor.setAccessible(true);

        return constructor;

    }

    /**
     * Returns a {@link Method} with a certain name and parameter types declared in the given class or any subclass
     * (except {@link Object}).
     * <p/>
     * If no parameter types are specified i.e., {@code paramTypes} is {@code null} the parameter types are ignored
     * for signature comparison.<br/>
     * If a parameter type is not known i.e., it is {@code null}, all declared methods are checked whether their
     * parameter types conform to the known parameter types i.e., if every known type is assignable to the parameter
     * type of the method and if exactly one was found, it is returned.<br/>
     * Otherwise a {@link NoSuchMethodException} is thrown indicating that no or several methods were found.
     * 
     * @param type the class
     * @param methodName the name of the method to find
     * @param clazzes the full-qualified class names of the parameters
     * @return the accessible method resolved
     * @throws ClassNotFoundException if a class cannot be located by the specified class loader
     */
    public static Method findMethod(final Class<?> type, String methodName, Class<?>... clazzes) throws ClassNotFoundException {
        Method method = null;

        // If all parameter types are known, find the method that exactly matches the signature
        if (clazzes != null && !ArrayUtils.contains(clazzes, null)) {
            for (Class<?> clazz = type; clazz != Object.class; clazz = clazz.getSuperclass()) {
                try {
                    method = type.getDeclaredMethod(methodName, clazzes);
                    break;
                } catch (NoSuchMethodException e) {
                    // Ignore
                }
            }
        }

        // If no method was found, find all possible candidates
        if (method == null) {
            List<Method> candidates = new ArrayList<>();
            for (Class<?> clazz = type; clazz != null; clazz = clazz.getSuperclass()) {
                for (Method declaredMethod : clazz.getDeclaredMethods()) {
                    if (declaredMethod.getName().equals(methodName)
                            && (clazzes == null || ClassUtils.isAssignable(clazzes, declaredMethod.getParameterTypes()))) {

                        // Check if there is already a overridden method with the same signature
                        for (Method candidate : candidates) {
                            if (candidate.getName().equals(declaredMethod.getName())) {
                                /**
                                 * If there is at least one parameters in the method of the super type, which is a
                                 * sub type of the corresponding parameter of the sub type, remove the method declared
                                 * in the sub type.
                                 */
                                if (!Arrays.equals(declaredMethod.getParameterTypes(), candidate.getParameterTypes())
                                        && ClassUtils.isAssignable(declaredMethod.getParameterTypes(), candidate.getParameterTypes())) {
                                    candidates.remove(candidate);
                                } else {
                                    declaredMethod = null;
                                }
                                break;
                            }
                        }

                        // If the method has a different signature matching the given types, add it to the candidates
                        if (declaredMethod != null) {
                            candidates.add(declaredMethod);
                        }
                    }
                }
            }

            if (candidates.size() != 1) {
                throw new JCloudScaleException(String.format("Cannot find distinct method '%s.%s()' with parameter types %s",
                        type, methodName, Arrays.toString(clazzes)));
            }
            method = candidates.get(0);
        }

        //do we really need this dependency?
        //ReflectionUtils.makeAccessible(method);
        if(method != null && !method.isAccessible())
            method.setAccessible(true);

        return method;

    }

    public static void checkLegalCloudIdDef(Object obj) {

        for(Field f : obj.getClass().getDeclaredFields()) {
            if(f.getAnnotation(CloudObjectId.class) != null) {

                if(!f.getType().equals(UUID.class) && !f.getType().equals(String.class) && !f.getType().equals(Object.class))
                    throw new IllegalDefinitionException("Illegal field type "+f.getType().getName()+" annotated with " +
                            "@CloudObjectId. You may only annotate java.lang.Object, java.lang.String and java.util.UUID");

                if(Modifier.isStatic(f.getModifiers()))
                    throw new IllegalDefinitionException("Illegal field "+f.getName()+" annotated with @CloudObjectId. " +
                            "Field has to be non-static.");

            }
        }

    }

    public static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException, SecurityException {

        if(type.equals(Object.class)) {
            return type.getDeclaredField(fieldName);
        }

        // now that's an ugly construct :)
        Field field = null;
        try {
            field = type.getDeclaredField(fieldName);
        } catch(NoSuchFieldException e) {
            field = findField(type.getSuperclass(), fieldName);
        }

        return field;

    }

    public static void injectCloudId(Object obj, UUID id) {

        for(Field f : obj.getClass().getDeclaredFields()) {
            if(f.getAnnotation(CloudObjectId.class) != null) {

                f.setAccessible(true);
                try {
                    if(f.getType().equals(String.class))
                        f.set(obj, id.toString());
                    else
                        f.set(obj, id);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new JCloudScaleException(e, "Unexpected error when injecting @CloudObjectId");
                }

            }
        }

    }
    
    public static void injectClientCloudObject(Object obj, ClientCloudObject cco) {

        for(Field f : obj.getClass().getFields()) {
            if(f.getAnnotation(ClientObject.class) != null) {

                f.setAccessible(true);
                try {
                	f.set(obj, cco);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new JCloudScaleException(e, "Unexpected error when injecting @ClientCloudObject");
                }

            }
        }

    }

    public static void checkLegalCloudInvocationInfoDef(Object obj) {

        for(Field f : obj.getClass().getDeclaredFields()) {
            if(f.getAnnotation(CloudInvocationInfos.class) != null) {

                Type[] genericTypes = ((ParameterizedType)f.getGenericType()).getActualTypeArguments();
                Class<?> typeParameter = (Class<?>) genericTypes[0];


                if(!f.getType().equals(List.class) || genericTypes.length!=1 || !typeParameter.equals(InvocationInfo.class))
                    throw new IllegalDefinitionException("Illegal field type "+f.getType().getName()+" annotated with " +
                            "@CloudInvocationInfos. You may only annotate java.util.List<InvocationInfo> fields.");

                if(Modifier.isStatic(f.getModifiers()))
                    throw new IllegalDefinitionException("Illegal field "+f.getName()+" annotated with @CloudInvocationInfos. " +
                            "Field has to be non-static.");

            }
        }

    }

    @SuppressWarnings("unchecked")
    public static void addInvocationInfo(Object obj, InvocationInfo info) {

        if(obj == null)
            return;

        for(Field f : obj.getClass().getDeclaredFields()) {
            if(f.getAnnotation(CloudInvocationInfos.class) != null) {

                try {
                    f.setAccessible(true);
                    if(f.get(obj) == null)
                        f.set(obj, new LinkedList<InvocationInfo>());
                    ((List<InvocationInfo>)f.get(obj)).add(info);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new JCloudScaleException(e, "Unexpected error when injecting into @CloudInvocationInfos");
                }

            }
        }

    }

    @SuppressWarnings("unchecked")
    public static void removeInvocationInfo(Object obj, String id) {

        if(obj == null)
            return;

        for(Field f : obj.getClass().getDeclaredFields()) {
            if(f.getAnnotation(CloudInvocationInfos.class) != null) {

                try {
                    f.setAccessible(true);
                    if(((List<InvocationInfo>)f.get(obj)).contains(new InvocationInfo(null, id, null, null)))
                        ((List<InvocationInfo>)f.get(obj)).remove(new InvocationInfo(null, id, null, null));
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new JCloudScaleException(e, "Unexpected error when injecting into @CloudInvocationInfos");
                }

            }
        }

    }

    public static void injectEventSink(Object obj) {

        for(Field f : obj.getClass().getDeclaredFields()) {
            if(f.getAnnotation(EventSink.class) != null) {

                f.setAccessible(true);
                if(IEventSink.class.isAssignableFrom(f.getType()))
                    try {
                        f.set(obj, new DefaultEventSink());
                    } catch (Exception e) {
                        throw new JCloudScaleException(e, "Unexpected error when injecting into @EventSink");
                    }
                else
                    throw new JCloudScaleException(
                            "Cannot inject event sink into field "+f.getName()+". "+
                                    "Type needs to be IEventSink, but is "+f.getType().getCanonicalName());

            }
        }

    }

    /**
     * Returns the full-qualified class names for the given classes.<br/>
     * This method is the inverse of {@link #getClassesFromNames(String[], ClassLoader)}.
     *
     * @param classes the classes
     * @return a non-null array of the full-qualified class names
     */
    public static String[] getNamesFromClasses(Class<?>... classes) {
        if (ArrayUtils.isEmpty(classes)) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        String[] names = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            if (classes[i] != null) {
                names[i] = classes[i].getName();
            }
        }
        return names;
    }

    /**
     * Loads the classes with the given full-qualified names using the provided {@link ClassLoader}.<br/>
     * This method is the inverse of {@link #getNamesFromClasses(Class[])}.
     * <p/>
     * Note that unknown parameter types i.e., {@code "null"} are handled gracefully. Instead of throwing an exception,
     * they are resolved to {@code null} instead. This allows the caller to predict the type e.g., by comparing them
     * with method or constructor signatures.
     * 
     * @param names the names of the classes to load
     * @param classLoader the class loader to use
     * @return a non-null array of the classes loaded
     * @throws ClassNotFoundException if a class cannot be located by the specified class loader
     */
    public static Class<?>[] getClassesFromNames(String[] names, ClassLoader classLoader) throws ClassNotFoundException {
        if (ArrayUtils.isEmpty(names)) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        Class<?>[] classes = new Class<?>[names.length];
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) {
                // If there is no name, skip the determination and assume that the caller is able to predict it
                classes[i] = null;
            } else if (isPrimitive(names[i])) {
                classes[i] = getPrimitiveType(names[i]);
            } else {
                classes[i] = Class.forName(names[i], true, classLoader);
            }
        }
        return classes;
    }

    public static Class<?>[] getClassesFromObjects(Object... args) {
        if (ArrayUtils.isEmpty(args)) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        // At least one parameter was null
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i] != null ? args[i].getClass() : null;
        }
        return paramTypes;
    }

    /**
     * Checks whether the given named type represents a primitive type.
     * 
     * @param name the name of the type
     * @return {@code true} if the type is a primitive type, {@code false} otherwise
     * @see #getPrimitiveType(String)
     */
    private static boolean isPrimitive(String name) {
        return getPrimitiveType(name) != null;
    }

    /**
     * Returns the virtual machine's Class object for the named primitive type.<br/>
     * If the type is not a primitive type, {@code null} is returned.
     *
     * @param name the name of the type
     * @return the Class instance representing the primitive type
     */
    private static Class<?> getPrimitiveType(String name) {
        name = StringUtils.defaultIfEmpty(name, "");
        if(name.equals("int"))
            return Integer.TYPE;
        if(name.equals("short"))
            return Short.TYPE;
        if(name.equals("long"))
            return Long.TYPE;
        if(name.equals("float"))
            return Float.TYPE;
        if(name.equals("double"))
            return Double.TYPE;
        if(name.equals("byte"))
            return Byte.TYPE;
        if(name.equals("char"))
            return Character.TYPE;
        if(name.equals("boolean"))
            return Boolean.TYPE;
        if(name.equals("void"))
            return Void.TYPE;

        return null;
    }

    public static <T> T newInstance(Class<T> clazz)
    {
        if(clazz == null)
            return null;
        try
        {
            return findConstructor(clazz).newInstance();
        }
        catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | ClassNotFoundException e)
        {
            throw new JCloudScaleException(e, "Failed to construct instance of class "+clazz.getName());
        }
    }

    public static boolean[] findByRefParams(Method method) {

        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        Class<?>[] paramTypes = method.getParameterTypes();
        return findByRefParams(paramAnnotations, paramTypes);

    }

    public static boolean[] findByRefParams(Constructor<?> method) {

        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        Class<?>[] paramTypes = method.getParameterTypes();
        return findByRefParams(paramAnnotations, paramTypes);

    }

    private static boolean[] findByRefParams(Annotation[][] paramAnnotations, Class<?>[] paramTypes) {

        boolean[] byRefReturn = new boolean[paramTypes.length];

        int i = 0;
        for(Class<?> param : paramTypes) {
            byRefReturn[i] = isByRef(param, paramAnnotations[i]);
            i++;
        }

        return byRefReturn;

    }

    public static boolean isByRefReturn(Method method) {

        Annotation[] methodAnnotations = method.getAnnotations();
        Class<?> returnType = method.getReturnType();
        return isByRef(returnType, methodAnnotations);

    }

    public static boolean isByRef(Field field) {

        Annotation[] fieldAnnotations = field.getAnnotations();
        Class<?> type = field.getType();
        return isByRef(type, fieldAnnotations);
    }

    private static boolean isByRef(Class<?> param, Annotation[] annotations) {
        // if parameter has ByValue annotation, it is ByValueParameter
        if (containsAnnotation(annotations, ByValueParameter.class))
            return false;

        // if this is primitive or wrapper type, it is ByValueParameter
        if(param.isPrimitive() || param.equals(String.class) || Primitives.allWrapperTypes().contains(param))
            return false;

        // if this is enum, it is ByValueParameter
        if(param.isEnum())
            return false;

        // if parameter class has ByValueParameter annotation, it is ByValueParameter
        if(param.isAnnotationPresent(ByValueParameter.class))
            return false;

        // otherwise it is ByRefParameter
        return true;
    }

    private static boolean containsAnnotation(Annotation[] annotations, Class<?> annotation) {
        for(Annotation a : annotations) {
            if(annotation.equals(a.annotationType()))
                return true;
        }
        return false;
    }
}
