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

import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase.HbaseCell;
import at.ac.tuwien.infosys.jcloudscale.datastore.driver.hbase.HbaseRow;
import at.ac.tuwien.infosys.jcloudscale.datastore.mapping.hbase.HbaseMapperImpl;
import at.ac.tuwien.infosys.jcloudscale.datastore.test.TestConstants;

import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.*;

public class HbaseMapperImplTest {

    private HbaseMapperImpl hbaseMapper;

    @Before
    public void init() {
        hbaseMapper = new HbaseMapperImpl();
    }

    @Test
    public void testSimpleObject_Serialize() {
        HbaseRow hbaseRow = hbaseMapper.serialize(TestConstants.PERSON);
        assertNotNull(hbaseRow);
        assertEquals("Person", hbaseRow.getTableName());
        assertEquals(2, hbaseRow.getCells().size());
        assertHbaseCell("firstName", "Person", Bytes.toBytes("John"), hbaseRow.getCells().get(0));
        assertHbaseCell("lastName", "Person", Bytes.toBytes("Doe"), hbaseRow.getCells().get(1));
    }

    @Test
    public void testSimpleObject_Deserialize() {
        HbaseRow hbaseRow = new HbaseRow("Person");
        List<HbaseCell> cells = Arrays.asList(new HbaseCell("Person", "firstName", Bytes.toBytes("John")),
                new HbaseCell("Person", "lastName", Bytes.toBytes("Doe")));
        hbaseRow.getCells().addAll(cells);

        TestConstants.Person person = hbaseMapper.deserialize(hbaseRow, TestConstants.Person.class);
        assertNotNull(person);
        assertEquals("John", person.firstName);
        assertEquals("Doe", person.lastName);
    }

    @Test
    public void testNestedObject_Serialize() {
        HbaseRow hbaseRow = hbaseMapper.serialize(TestConstants.ARTICLE);
        assertNotNull(hbaseRow);
        assertEquals("Article", hbaseRow.getTableName());
        assertEquals(3, hbaseRow.getCells().size());
        assertHbaseCell("title", "Article", Bytes.toBytes("Title"), hbaseRow.getCells().get(0));
        assertHbaseCell("firstName", "Person", Bytes.toBytes("John"), hbaseRow.getCells().get(1));
        assertHbaseCell("lastName", "Person", Bytes.toBytes("Doe"), hbaseRow.getCells().get(2));
    }

    @Test
    public void testNestedObject_Deserialize() {
        HbaseRow hbaseRow = new HbaseRow("Article");
        List<HbaseCell> cells = Arrays.asList(new HbaseCell("Article", "title", Bytes.toBytes("Title")),
                new HbaseCell("Person", "firstName", Bytes.toBytes("John")),
                new HbaseCell("Person", "lastName", Bytes.toBytes("Doe")));
        hbaseRow.getCells().addAll(cells);

        TestConstants.Article article = hbaseMapper.deserialize(hbaseRow, TestConstants.Article.class);
        assertNotNull(article);
        assertEquals("Title", article.title);
        assertEquals("John", article.author.firstName);
        assertEquals("Doe", article.author.lastName);
    }

    @Test
    public void testListObject_Serialize() {
        HbaseRow hbaseRow = hbaseMapper.serialize(TestConstants.CONTAINER);
        assertNotNull(hbaseRow);
        assertEquals("Container", hbaseRow.getTableName());
        assertEquals(2, hbaseRow.getCells().size());
        assertHbaseCell("name", "Container", Bytes.toBytes("Container"), hbaseRow.getCells().get(0));
        assertHbaseCell("items", "Container", Bytes.toBytes("[\"Item1\",\"Item2\"]"), hbaseRow.getCells().get(1));
    }

    @Test
    public void testListObject_Deserialize() {
        HbaseRow hbaseRow = new HbaseRow("Container");
        List<HbaseCell> cells = Arrays.asList(new HbaseCell("Container", "name", Bytes.toBytes("Container")),
                new HbaseCell("Container", "items", Bytes.toBytes("[\"Item1\",\"Item2\"]")));
        hbaseRow.getCells().addAll(cells);

        TestConstants.Container container = hbaseMapper.deserialize(hbaseRow, TestConstants.Container.class);
        assertNotNull(container);
        assertEquals("Container", container.name);
        assertEquals(2, container.items.size());
        assertEquals("Item1", container.items.get(0));
        assertEquals("Item2", container.items.get(1));
    }

    @Test
    public void testListObject_Complex_Serialize() {
        HbaseRow hbaseRow = hbaseMapper.serialize(TestConstants.GROUP);
        assertNotNull(hbaseRow);
        assertEquals("Group", hbaseRow.getTableName());
        assertEquals(2, hbaseRow.getCells().size());
        assertHbaseCell("name", "Group", Bytes.toBytes("Group1"), hbaseRow.getCells().get(0));
        assertHbaseCell("persons", "Group", Bytes.toBytes("[{\"firstName\":\"John\",\"lastName\":\"Doe\"},{\"firstName\":\"Max\",\"lastName\":\"Mustermann\"}]"), hbaseRow.getCells().get(1));
    }

    @Test
    public void testListObject_Complex_Deserialize() {
        HbaseRow hbaseRow = new HbaseRow("Group");
        List<HbaseCell> cells = Arrays.asList(new HbaseCell("Group", "name", Bytes.toBytes("Group1")),
                new HbaseCell("Group", "persons", Bytes.toBytes("[{\"firstName\":\"John\",\"lastName\":\"Doe\"},{\"firstName\":\"Max\",\"lastName\":\"Mustermann\"}]")));
        hbaseRow.getCells().addAll(cells);

        TestConstants.Group group = hbaseMapper.deserialize(hbaseRow, TestConstants.Group.class);
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
        HbaseRow hbaseRow = hbaseMapper.serialize(TestConstants.PRODUCT);
        assertNotNull(hbaseRow);
        assertEquals("Product", hbaseRow.getTableName());
        assertEquals(2, hbaseRow.getCells().size());
        assertHbaseCell("name", "Product", Bytes.toBytes("Product1"), hbaseRow.getCells().get(0));
        assertHbaseCell("properties", "Product", Bytes.toBytes("{\"Property2\":\"Value2\",\"Property1\":\"Value1\"}"), hbaseRow.getCells().get(1));
    }

    @Test
    public void testStringMap_Deserialize() {
        HbaseRow hbaseRow = new HbaseRow("Product");
        List<HbaseCell> cells = Arrays.asList(new HbaseCell("Product", "name", Bytes.toBytes("Product1")),
                new HbaseCell("Product", "properties", Bytes.toBytes("{\"Property2\":\"Value2\",\"Property1\":\"Value1\"}")));
        hbaseRow.getCells().addAll(cells);

        TestConstants.Product product = hbaseMapper.deserialize(hbaseRow, TestConstants.Product.class);
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
        HbaseRow hbaseRow = hbaseMapper.serialize(TestConstants.TEAM);
        assertNotNull(hbaseRow);
        assertEquals("Team", hbaseRow.getTableName());
        assertEquals(2, hbaseRow.getCells().size());
        assertHbaseCell("name", "Team", Bytes.toBytes("Team1"), hbaseRow.getCells().get(0));
        assertHbaseCell("players", "Team", Bytes.toBytes("{\"Player2\":{\"firstName\":\"Max\",\"lastName\":\"Mustermann\"},\"Player1\":{\"firstName\":\"John\",\"lastName\":\"Doe\"}}"), hbaseRow.getCells().get(1));
    }

    @Test
    public void testObjectMap_Deserialize() {
        HbaseRow hbaseRow = new HbaseRow("Team");
        List<HbaseCell> cells = Arrays.asList(new HbaseCell("Team", "name", Bytes.toBytes("Team1")),
                new HbaseCell("Team", "players", Bytes.toBytes("{\"Player2\":{\"firstName\":\"Max\",\"lastName\":\"Mustermann\"},\"Player1\":{\"firstName\":\"John\",\"lastName\":\"Doe\"}}")));
        hbaseRow.getCells().addAll(cells);

        TestConstants.Team team = hbaseMapper.deserialize(hbaseRow, TestConstants.Team.class);
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
        HbaseRow hbaseRow = hbaseMapper.serialize(TestConstants.PRIMITIVE);
        assertNotNull(hbaseRow);
        assertEquals("Primitive", hbaseRow.getTableName());
        assertEquals(7, hbaseRow.getCells().size());
        assertHbaseCell("number", "Primitive", Bytes.toBytes(2), hbaseRow.getCells().get(0));
        assertHbaseCell("bool", "Primitive", Bytes.toBytes(true), hbaseRow.getCells().get(1));
        assertHbaseCell("ch", "Primitive", Bytes.toBytes('a'), hbaseRow.getCells().get(2));
        assertHbaseCell("doub", "Primitive", Bytes.toBytes(1.38), hbaseRow.getCells().get(3));
        assertHbaseCell("f", "Primitive", Bytes.toBytes(2.04f), hbaseRow.getCells().get(4));
        assertHbaseCell("l", "Primitive", Bytes.toBytes(42l), hbaseRow.getCells().get(5));
        assertHbaseCell("s", "Primitive", Bytes.toBytes((short)2), hbaseRow.getCells().get(6));
    }

    @Test
    public void testPrimitive_Deserialize() {
        HbaseRow hbaseRow = new HbaseRow("Primitive");
        List<HbaseCell> cells = Arrays.asList(new HbaseCell("Primitive", "number", Bytes.toBytes(2)),
                new HbaseCell("Primitive", "bool", Bytes.toBytes(true)),
                new HbaseCell("Primitive", "ch", Bytes.toBytes('a')),
                new HbaseCell("Primitive", "doub", Bytes.toBytes(1.38)),
                new HbaseCell("Primitive", "f", Bytes.toBytes(2.04f)),
                new HbaseCell("Primitive", "l", Bytes.toBytes(42l)),
                new HbaseCell("Primitive", "s", Bytes.toBytes((short)2)));
        hbaseRow.getCells().addAll(cells);

        TestConstants.Primitive primitive = hbaseMapper.deserialize(hbaseRow, TestConstants.Primitive.class);
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
        HbaseRow hbaseRow = hbaseMapper.serialize(TestConstants.ENUMERATION);
        assertNotNull(hbaseRow);
        assertEquals("Enumeration", hbaseRow.getTableName());
        assertEquals(1, hbaseRow.getCells().size());
        assertHbaseCell("gender", "Enumeration", Bytes.toBytes(TestConstants.Enumeration.Gender.MALE.toString()), hbaseRow.getCells().get(0));
    }

    @Test
    public void testEnumeration_Deserialize() {
        HbaseRow hbaseRow = new HbaseRow("Enumeration");
        List<HbaseCell> cells = Arrays.asList(new HbaseCell("Enumeration", "gender", Bytes.toBytes(TestConstants.Enumeration.Gender.MALE.toString())));
        hbaseRow.getCells().addAll(cells);

        TestConstants.Enumeration enumeration = hbaseMapper.deserialize(hbaseRow, TestConstants.Enumeration.class);
        assertNotNull(enumeration);
        assertEquals(TestConstants.Enumeration.Gender.MALE, enumeration.gender);
    }

    @Test
    public void testStaticFinal_Serialize() {
        HbaseRow hbaseRow = hbaseMapper.serialize(TestConstants.STATICFINAL);
        assertNotNull(hbaseRow);
        assertEquals("Staticfinal", hbaseRow.getTableName());
        assertEquals(1, hbaseRow.getCells().size());
        assertHbaseCell("string", "Staticfinal", Bytes.toBytes("test"), hbaseRow.getCells().get(0));
    }

    @Test
    public void testStaticFinal_Deserialize() {
        HbaseRow hbaseRow = new HbaseRow("Staticfinal");
        List<HbaseCell> cells = Arrays.asList(new HbaseCell("Staticfinal", "string", Bytes.toBytes("test")));
        hbaseRow.getCells().addAll(cells);

        TestConstants.Staticfinal staticfinal = hbaseMapper.deserialize(hbaseRow, TestConstants.Staticfinal.class);
        assertNotNull(staticfinal);
        assertEquals("test", staticfinal.string);
        assertEquals("Foo", staticfinal.value2);
        assertEquals("Constant", TestConstants.STATICFINAL.CONSTANT);
    }

    @Test
    public void testUnsupportedType() {
        try {
            hbaseMapper.serialize(TestConstants.UNSUPPORTED);
            fail();
        } catch (DatastoreException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testObjectMapInteger_Serialize() {
        HbaseRow hbaseRow = hbaseMapper.serialize(TestConstants.TEAM_INTEGER);
        assertNotNull(hbaseRow);
        assertEquals("TeamInteger", hbaseRow.getTableName());
        assertEquals(2, hbaseRow.getCells().size());
        assertHbaseCell("name", "TeamInteger", Bytes.toBytes("Team1"), hbaseRow.getCells().get(0));
        assertHbaseCell("players", "TeamInteger", Bytes.toBytes("{\"1\":{\"firstName\":\"John\",\"lastName\":\"Doe\"},\"2\":{\"firstName\":\"Max\",\"lastName\":\"Mustermann\"}}"), hbaseRow.getCells().get(1));
    }

    @Test
    public void testObjectMapInteger_Deserialize() {
        HbaseRow hbaseRow = new HbaseRow("TeamInteger");
        List<HbaseCell> cells = Arrays.asList(new HbaseCell("TeamInteger", "name", Bytes.toBytes("Team1")),
                new HbaseCell("TeamInteger", "players", Bytes.toBytes("{\"1\":{\"firstName\":\"John\",\"lastName\":\"Doe\"},\"2\":{\"firstName\":\"Max\",\"lastName\":\"Mustermann\"}}")));
        hbaseRow.getCells().addAll(cells);

        TestConstants.TeamInteger team = hbaseMapper.deserialize(hbaseRow, TestConstants.TeamInteger.class);
        assertNotNull(team);
        assertEquals("Team1", team.name);
        assertEquals(2, team.players.size());
        assertTrue(team.players.containsKey(1));
        TestConstants.Person person = team.players.get(1);
        assertEquals("John", person.firstName);
        assertEquals("Doe", person.lastName);
        person = team.players.get(2);
        assertEquals("Max", person.firstName);
        assertEquals("Mustermann", person.lastName);
    }

    public void assertHbaseCell(String columnName, String familyName, byte[] value, HbaseCell hbaseCell) {
        assertEquals(columnName, hbaseCell.getColumnName());
        assertEquals(familyName, hbaseCell.getFamilyName());
        assertTrue(Arrays.equals(value, hbaseCell.getValue()));
    }
}
