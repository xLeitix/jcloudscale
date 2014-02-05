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
package at.ac.tuwien.infosys.jcloudscale.datastore.test.mapping.type;

import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.Adapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.Adapters;
import at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase.HbaseCell;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.*;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.json.JsonListTypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.json.JsonMapTypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.json.JsonStringTypeAdapter;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type.json.JsonTypeAdapterFactory;
import at.ac.tuwien.infosys.jcloudscale.datastore.test.TestConstants;

import com.google.gson.JsonElement;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;

public class TypeMetadataImplTest {

    @Test
    public void testNonGenericField() throws NoSuchFieldException {
        Field field = TestConstants.Person.class.getDeclaredField("firstName");
        TypeMetadata typeMetadata = new TypeMetadataImpl(TestConstants.PERSON, field, new JsonTypeAdapterFactory(), JsonElement.class);
        assertEquals("firstName", typeMetadata.getFieldName());
        assertEquals(String.class, typeMetadata.getFieldType());
        assertEquals(TestConstants.PERSON, typeMetadata.getParent());
        assertTrue(typeMetadata.getTypeAdapter() instanceof JsonStringTypeAdapter);
        assertTrue(typeMetadata.isJavaInternalClass());
        assertFalse(typeMetadata.isCustomTypeAdapterPresent());
    }

    @Test
    public void testListField() throws NoSuchFieldException {
        Field field = TestConstants.Container.class.getDeclaredField("items");
        TypeMetadata typeMetadata = new TypeMetadataImpl(TestConstants.CONTAINER, field, new JsonTypeAdapterFactory(), JsonElement.class);
        assertEquals("items", typeMetadata.getFieldName());
        assertEquals(List.class, typeMetadata.getFieldType());
        assertEquals(TestConstants.CONTAINER, typeMetadata.getParent());
        assertTrue(typeMetadata.getTypeAdapter() instanceof JsonListTypeAdapter);
        assertTrue(typeMetadata.isJavaInternalClass());
        assertFalse(typeMetadata.isCustomTypeAdapterPresent());
        assertEquals(1, typeMetadata.getTypeParameterTypeAdapters().size());
        assertTrue(typeMetadata.getTypeParameterTypeAdapters().get(0) instanceof JsonStringTypeAdapter);
        assertEquals(1, typeMetadata.getTypeParameterTypes().size());
        assertEquals(String.class, typeMetadata.getTypeParameterTypes().get(0));
    }

    @Test
    public void testMapField() throws NoSuchFieldException {
        Field field = TestConstants.Product.class.getDeclaredField("properties");
        TypeMetadata typeMetadata = new TypeMetadataImpl(TestConstants.PRODUCT, field, new JsonTypeAdapterFactory(), JsonElement.class);
        assertEquals("properties", typeMetadata.getFieldName());
        assertEquals(Map.class, typeMetadata.getFieldType());
        assertEquals(TestConstants.PRODUCT, typeMetadata.getParent());
        assertTrue(typeMetadata.getTypeAdapter() instanceof JsonMapTypeAdapter);
        assertTrue(typeMetadata.isJavaInternalClass());
        assertFalse(typeMetadata.isCustomTypeAdapterPresent());
        assertEquals(2, typeMetadata.getTypeParameterTypeAdapters().size());
        assertTrue(typeMetadata.getTypeParameterTypeAdapters().get(0) instanceof JsonStringTypeAdapter);
        assertTrue(typeMetadata.getTypeParameterTypeAdapters().get(1) instanceof JsonStringTypeAdapter);
        assertEquals(2, typeMetadata.getTypeParameterTypes().size());
        assertEquals(String.class, typeMetadata.getTypeParameterTypes().get(0));
        assertEquals(String.class, typeMetadata.getTypeParameterTypes().get(1));
    }

    @Test
    public void testCustomField() throws NoSuchFieldException {
        Field field = TestConstants.Article.class.getDeclaredField("author");
        TypeMetadata typeMetadata = new TypeMetadataImpl(TestConstants.ARTICLE, field, new JsonTypeAdapterFactory(), JsonElement.class);
        assertEquals("author", typeMetadata.getFieldName());
        assertEquals(TestConstants.Person.class, typeMetadata.getFieldType());
        assertEquals(TestConstants.ARTICLE, typeMetadata.getParent());
        assertNull(typeMetadata.getTypeAdapter());
        assertFalse(typeMetadata.isJavaInternalClass());
        assertFalse(typeMetadata.isCustomTypeAdapterPresent());
    }

    @Test
    public void testCustomTypeAdapter() throws NoSuchFieldException {
        Field field = Article.class.getField("author");
        Article article = new Article();
        TypeMetadata typeMetadata = new TypeMetadataImpl(article, field, new JsonTypeAdapterFactory(), JsonElement.class);
        assertEquals("author", typeMetadata.getFieldName());
        assertEquals(TestConstants.Person.class, typeMetadata.getFieldType());
        assertEquals(article, typeMetadata.getParent());
        assertTrue(typeMetadata.getTypeAdapter() instanceof CustomJsonTypeAdapter);
        assertFalse(typeMetadata.isJavaInternalClass());
        assertTrue(typeMetadata.isCustomTypeAdapterPresent());
    }

    public static class Article {
        @Adapters({
                @Adapter(targetClass = HbaseCell.class, adapterClass = CustomHbaseTypeAdapter.class),
                @Adapter(targetClass = JsonElement.class, adapterClass = CustomJsonTypeAdapter.class)
        })
        public TestConstants.Person author;
    }

    public static class CustomJsonTypeAdapter implements TypeAdapter<TestConstants.Person, JsonElement> {

        @Override
        public JsonElement serialize(TestConstants.Person object, TypeMetadata<JsonElement> typeMetadata) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public TestConstants.Person deserialize(JsonElement element, TypeMetadata<JsonElement> typeMetadata) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    public static class CustomHbaseTypeAdapter implements TypeAdapter<TestConstants.Person, HbaseCell> {

        @Override
        public HbaseCell serialize(TestConstants.Person object, TypeMetadata<HbaseCell> typeMetadata) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public TestConstants.Person deserialize(HbaseCell element, TypeMetadata<HbaseCell> typeMetadata) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
