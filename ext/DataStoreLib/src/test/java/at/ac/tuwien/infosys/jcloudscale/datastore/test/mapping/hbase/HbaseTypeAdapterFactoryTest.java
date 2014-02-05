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
package at.ac.tuwien.infosys.jcloudscale.datastore.test.mapping.hbase;

import at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase.HbaseCell;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.TypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.hbase.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class HbaseTypeAdapterFactoryTest {

    private TypeAdapterFactory<HbaseCell> typeAdapterFactory;
    private enum VALUES {ONE, TWO, THREE};

    @Before
    public void init() {
        typeAdapterFactory = new HbaseTypeAdapterFactory();
    }

    @Test
    public void testStringType() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(String.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseStringTypeAdapter);
    }

    @Test
    public void testIntegerType() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(Integer.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseIntegerTypeAdapter);
    }

    @Test
    public void testDoubleType() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(Double.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseDoubleTypeAdapter);
    }

    @Test
    public void testListType_ArrayList() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(ArrayList.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseListTypeAdapter);
    }

    @Test
    public void testListType_LinkedList() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(LinkedList.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseListTypeAdapter);
    }

    @Test
    public void testMapType_HashMap() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(HashMap.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseMapTypeAdapter);
    }

    @Test
    public void testMapType_TreeMap() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(TreeMap.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseMapTypeAdapter);
    }

    @Test
    public void testUnsupportedType() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(Object.class);
        assertNull(typeAdapter);
    }

    @Test
    public void testPrimitiveType() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(int.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseIntegerTypeAdapter);
    }

    @Test
    public void testEnumType() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(VALUES.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseEnumTypeAdapter);
    }

    @Test
    public void testByteType() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(Byte.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseByteTypeAdapter);
    }

    @Test
    public void testPrimitiveType_Byte() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(byte.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseByteTypeAdapter);
    }

    @Test
    public void testCharacterType() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(Character.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseCharacterTypeAdapter);
    }

    @Test
    public void testPrimitiveType_Character() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(char.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseCharacterTypeAdapter);
    }

    @Test
    public void testFloatType() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(Float.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseFloatTypeAdapter);
    }

    @Test
    public void testPrimitiveType_Float() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(float.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseFloatTypeAdapter);
    }

    @Test
    public void testLongType() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(Long.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseLongTypeAdapter);
    }

    @Test
    public void testPrimitiveType_Long() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(long.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseLongTypeAdapter);
    }

    @Test
    public void testShortType() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(Short.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseShortTypeAdapter);
    }

    @Test
    public void testPrimitiveType_Short() {
        TypeAdapter<?, HbaseCell> typeAdapter = typeAdapterFactory.get(short.class);
        assertNotNull(typeAdapter);
        assertTrue(typeAdapter instanceof HbaseShortTypeAdapter);
    }
}
