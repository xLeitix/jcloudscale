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
package at.ac.tuwien.infosys.jcloudscale.test.datastore.mapping.json;

import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.*;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.json.*;
import com.google.gson.JsonElement;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

import static junit.framework.Assert.*;

public class JsonTypeAdapterFactoryTest {

    private TypeAdapterFactory<JsonElement> typeAdapterFactory;
    private enum VALUES {ONE, TWO, THREE};


    @Before
    public void init() {
        typeAdapterFactory = new JsonTypeAdapterFactory();
    }

    @Test
    public void testStringType() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(String.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonStringTypeAdapter);
    }

    @Test
    public void testIntegerType() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(Integer.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonIntegerTypeAdapter);
    }

    @Test
    public void testDoubleType() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(Double.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonDoubleTypeAdapter);
    }

    @Test
    public void testListType_ArrayList() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(ArrayList.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonListTypeAdapter);
    }

    @Test
    public void testListType_LinkedList() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(LinkedList.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonListTypeAdapter);
    }

    @Test
    public void testMapType_HashMap() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(HashMap.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonMapTypeAdapter);
    }

    @Test
    public void testMapType_TreeMap() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(TreeMap.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonMapTypeAdapter);
    }

    @Test
    public void testUnsupportedType() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(Object.class);
        assertNull(typeAdapter);
    }

    @Test
    public void testPrimitiveType_Int() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(int.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonIntegerTypeAdapter);
    }

    @Test
    public void testEnumType() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(VALUES.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonEnumTypeAdapter);
    }

    @Test
    public void testByteType() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(Byte.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonByteTypeAdapter);
    }

    @Test
    public void testPrimitiveType_Byte() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(byte.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonByteTypeAdapter);
    }

    @Test
    public void testCharacterType() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(Character.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonCharacterTypeAdapter);
    }

    @Test
    public void testPrimitiveType_Character() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(char.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonCharacterTypeAdapter);
    }

    @Test
    public void testFloatType() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(Float.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonFloatTypeAdapter);
    }

    @Test
    public void testPrimitiveType_Float() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(float.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonFloatTypeAdapter);
    }

    @Test
    public void testLongType() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(Long.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonLongTypeAdapter);
    }

    @Test
    public void testPrimitiveType_Long() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(long.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonLongTypeAdapter);
    }

    @Test
    public void testShortType() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(Short.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonShortTypeAdapter);
    }

    @Test
    public void testPrimitiveType_Short() {
        TypeAdapter<?, JsonElement> typeAdapter = typeAdapterFactory.get(short.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof JsonShortTypeAdapter);
    }
}
