package at.ac.tuwien.infosys.jcloudscale.server.riak;
///*
//   Copyright 2013 Philipp Leitner
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//*/
//package at.ac.tuwien.infosys.jcloudscale.server.riak;
//
//import java.util.*;
//import java.lang.reflect.*;
//
//public class SerializationHandler {
//    
//	private static Map<Class<?>,Class<?>> primitiveMap = new HashMap<Class<?>,Class<?>>();
//    
//	static {
//		primitiveMap.put(boolean.class, Boolean.class);
//		primitiveMap.put(byte.class, Byte.class);
//		primitiveMap.put(char.class, Character.class);
//		primitiveMap.put(short.class, Short.class);
//		primitiveMap.put(int.class, Integer.class);
//		primitiveMap.put(long.class, Long.class);
//		primitiveMap.put(float.class, Float.class);
//		primitiveMap.put(double.class, Double.class);
//    }
//
//    /**
//     * Best try to convert string to destination class. If destClass is one of
//     * the supported primitive classes, an object of that type is returned. 
//     * Otherwise, the original string is returned.
//     */
//    public static Object convert(String value, Class<?> destClass) {
//		if ((value == null) || "".equals(value)) {
//		    return value;
//		}
//	
//		if (destClass.isPrimitive()) {
//		    destClass = primitiveMap.get(destClass);
//		}
//	
//		try {
//		    Method m = destClass.getMethod("valueOf", String.class);
//		    int mods = m.getModifiers();
//		    if (Modifier.isStatic(mods) && Modifier.isPublic(mods)) {
//			return m.invoke(null, value);
//		    }
//		}
//		catch (NoSuchMethodException e) {
//		    if (destClass == Character.class) {
//			return Character.valueOf(value.charAt(0));
//		    }
//		}
//		catch (IllegalAccessException e) {
//		    // this won't happen
//		}
//		catch (InvocationTargetException e) {
//		    // when this happens, the string cannot be converted to the intended type
//	            // we are ignoring it here - the original string will be returned.
//	            // But it can be re-thrown if desired!
//		}
//	
//		return value;
//    }
//
//}