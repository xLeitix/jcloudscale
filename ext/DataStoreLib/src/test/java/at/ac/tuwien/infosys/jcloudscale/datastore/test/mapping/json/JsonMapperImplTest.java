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
package at.ac.tuwien.infosys.jcloudscale.datastore.test.mapping.json;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.json.JsonMapperImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.test.TestConstants;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

public class JsonMapperImplTest {

    private JsonMapperImpl jsonMapper;

    @Before
    public void init() {
        jsonMapper = new JsonMapperImpl();
    }

    @Test
    public void testSimpleObject_Serialize() {
        String jsonString = jsonMapper.serialize(TestConstants.PERSON);
        assertNotNull(jsonString);
        assertEquals(TestConstants.PERSON_JSON, jsonString);
    }

    @Test
    public void testSimpleObject_Serialize_Null() {
        TestConstants.Person person = new TestConstants.Person(null,"Doe");
        String jsonString = jsonMapper.serialize(person);
        assertNotNull(jsonString);
        assertEquals("{\"lastName\":\"Doe\"}", jsonString);
    }

    @Test
    public void testSimpleObject_Deserialize() {
        TestConstants.Person person = jsonMapper.deserialize(TestConstants.PERSON_JSON, TestConstants.Person.class);
        assertNotNull(person);
        assertEquals("John", person.firstName);
        assertEquals("Doe", person.lastName);
    }

    @Test
    public void testSimpleObject_Deserialize_Null() {
        TestConstants.Person person = jsonMapper.deserialize("{\"lastName\":\"Doe\"}", TestConstants.Person.class);
        assertNotNull(person);
        assertNull(person.firstName);
        assertEquals("Doe", person.lastName);
    }

    @Test
    public void testNestedObject_Serialize() {
        String jsonString = jsonMapper.serialize(TestConstants.ARTICLE);
        assertNotNull(jsonString);
        assertEquals(TestConstants.ARTICLE_JSON, jsonString);
    }

    @Test
    public void testNestedObject_Deserialize() {
        TestConstants.Article article = jsonMapper.deserialize(TestConstants.ARTICLE_JSON, TestConstants.Article.class);
        assertNotNull(article);
        assertEquals("Title", article.title);
        assertNotNull(article.author);
        assertEquals("John", article.author.firstName);
        assertEquals("Doe", article.author.lastName);
    }

    @Test
    public void testListObject_Serialize() {
        String jsonString = jsonMapper.serialize(TestConstants.CONTAINER);
        assertNotNull(jsonString);
        assertEquals(TestConstants.CONTAINER_JSON, jsonString);
    }

    @Test
    public void testListObject_Deserialize() {
        TestConstants.Container container = jsonMapper.deserialize(TestConstants.CONTAINER_JSON, TestConstants.Container.class);
        assertNotNull(container);
        assertEquals("Container", container.name);
        assertEquals(2, container.items.size());
        assertEquals("Item1", container.items.get(0));
        assertEquals("Item2", container.items.get(1));
    }

    @Test
    public void testListObject_Complex_Serialize() {
        String jsonString = jsonMapper.serialize(TestConstants.GROUP);
        assertNotNull(jsonString);
        assertEquals(TestConstants.GROUP_JSON, jsonString);
    }

    @Test
    public void testListObject_Complex_Deserialize() {
        TestConstants.Group group = jsonMapper.deserialize(TestConstants.GROUP_JSON, TestConstants.Group.class);
        assertNotNull(group);
        assertEquals("Group1", group.name);
        assertEquals(2, group.persons.size());
        TestConstants.Person person = group.persons.get(0);
        assertEquals("John", person.firstName);
        assertEquals("Doe", person.lastName);
        person = group.persons.get(1);
        assertEquals("Max", person.firstName);
        assertEquals("Mustermann", person.lastName);
    }

    @Test
    public void testStringMap_Serialize() {
        String jsonString = jsonMapper.serialize(TestConstants.PRODUCT);
        assertNotNull(jsonString);
        assertEquals(TestConstants.PRODUCT_JSON, jsonString);
    }

    @Test
    public void testStringMap_Deserialize() {
        TestConstants.Product product = jsonMapper.deserialize(TestConstants.PRODUCT_JSON, TestConstants.Product.class);
        assertNotNull(product);
        assertEquals("Product1", product.name);
        assertEquals(2, product.properties.size());
        assertTrue(product.properties.containsKey("Property1"));
        assertEquals("Value1", product.properties.get("Property1"));
        assertTrue(product.properties.containsKey("Property2"));
        assertEquals("Value2", product.properties.get("Property2"));
    }

    @Test
    public void testObjectMap_Serialize() {
        String jsonString = jsonMapper.serialize(TestConstants.TEAM);
        assertNotNull(jsonString);
        assertEquals(TestConstants.TEAM_JSON, jsonString);
    }

    @Test
    public void testObjectMap_Deserialize() {
        TestConstants.Team team = jsonMapper.deserialize(TestConstants.TEAM_JSON, TestConstants.Team.class);
        assertNotNull(team);
        assertEquals("Team1", team.name);
        assertEquals(2, team.players.size());
        assertTrue(team.players.containsKey("Player1"));
        TestConstants.Person person = team.players.get("Player1");
        assertEquals("John", person.firstName);
        assertEquals("Doe", person.lastName);
        person = team.players.get("Player2");
        assertEquals("Max", person.firstName);
        assertEquals("Mustermann", person.lastName);
    }

    @Test
    public void testPrimitive_Serialize() {
        String jsonString = jsonMapper.serialize(TestConstants.PRIMITIVE);
        assertNotNull(jsonString);
        assertEquals(TestConstants.PRIMITIVE_JSON, jsonString);
    }

    @Test
    public void testPrimitive_Deserialize() {
        TestConstants.Primitive primitive = jsonMapper.deserialize(TestConstants.PRIMITIVE_JSON, TestConstants.Primitive.class);
        assertNotNull(primitive);
        assertEquals(2, primitive.number);
        assertTrue(primitive.bool);
        assertEquals('a', primitive.ch);
        assertEquals(1.38, primitive.doub);
        assertEquals(2.04f, primitive.f);
        assertEquals(42, primitive.l);
        assertEquals(2, primitive.s);
    }

    @Test
    public void testEnumeration_Serialize() {
        String jsonString = jsonMapper.serialize(TestConstants.ENUMERATION);
        assertNotNull(jsonString);
        assertEquals(TestConstants.ENUMERATION_JSON, jsonString);
    }

    @Test
    public void testEnumeration_Deserialize() {
        TestConstants.Enumeration enumeration = jsonMapper.deserialize(TestConstants.ENUMERATION_JSON, TestConstants.Enumeration.class);
        assertNotNull(enumeration);
        assertEquals(TestConstants.Enumeration.Gender.MALE, enumeration.gender);
    }

    @Test
    public void testStaticFinal_Serialize() {
        String jsonString = jsonMapper.serialize(TestConstants.STATICFINAL);
        assertNotNull(jsonString);
        assertEquals(TestConstants.STATICFINAL_JSON, jsonString);
    }

    @Test
    public void testStaticFinal_Deserialize() {
        TestConstants.Staticfinal staticfinal = jsonMapper.deserialize(TestConstants.STATICFINAL_JSON, TestConstants.Staticfinal.class);
        assertNotNull(staticfinal);
        assertEquals("test", staticfinal.string);
        assertEquals("Foo", staticfinal.value2);
        assertEquals("Constant", TestConstants.STATICFINAL.CONSTANT);
    }

    @Test
    public void testUnsupportedType() {
        try {
            jsonMapper.serialize(TestConstants.UNSUPPORTED);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testObjectMapInteger_Serialize() {
        String jsonString = jsonMapper.serialize(TestConstants.TEAM_INTEGER);
        assertNotNull(jsonString);
        assertEquals(TestConstants.TEAM_INTEGER_JSON, jsonString);
    }

    @Test
    public void testObjectMapInteger_Deserialize() {
        TestConstants.TeamInteger team = jsonMapper.deserialize(TestConstants.TEAM_INTEGER_JSON, TestConstants.TeamInteger.class);
        assertNotNull(team);
        assertEquals("Team1", team.name);
        assertEquals(2, team.players.size());
        TestConstants.Person person = team.players.get(new Integer(1));
        assertEquals("John", person.firstName);
        assertEquals("Doe", person.lastName);
        person = team.players.get(2);
        assertEquals("Max", person.firstName);
        assertEquals("Mustermann", person.lastName);
    }
}
