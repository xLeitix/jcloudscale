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
package at.ac.tuwien.infosys.jcloudscale.datastore.test;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TestConstants {

    //Avoid instantiation
    private TestConstants() {};

    public static String PERSON_JSON = "{\"firstName\":\"John\",\"lastName\":\"Doe\"}";
    public static Person PERSON = new Person("John", "Doe");
    public static class Person {
        public Person() {}
        public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
        public String firstName;
        public String lastName;
    }

    public static String ARTICLE_JSON = "{\"title\":\"Title\",\"author\":{\"firstName\":\"John\",\"lastName\":\"Doe\"}}";
    public static Article ARTICLE = new Article("Title", new Person("John", "Doe"));
    public static class Article {
        public Article() {}
        public Article(String title, Person author) {
            this.title = title;
            this.author = author;
        }
        public String title;
        public Person author;
    }

    public static String CONTAINER_JSON = "{\"name\":\"Container\",\"items\":[\"Item1\",\"Item2\"]}";
    public static Container CONTAINER = new Container("Container", Arrays.asList("Item1", "Item2"));
    public static class Container {
        public Container() {}
        public Container(String name, List<String> items) {
            this.name = name;
            this.items = items;
        }
        public String name;
        public List<String> items;
    }

    public static String GROUP_JSON = "{\"name\":\"Group1\",\"persons\":[{\"firstName\":\"John\",\"lastName\":\"Doe\"},{\"firstName\":\"Max\",\"lastName\":\"Mustermann\"}]}";
    public static Group GROUP = new Group("Group1", Arrays.asList(new Person("John", "Doe"), new Person("Max", "Mustermann")));
    public static class Group {
        public Group() {}
        public Group(String name, List<Person> persons) {
            this.name = name;
            this.persons = persons;
        }
        public String name;
        public List<Person> persons;
    }

    public static String PRODUCT_JSON = "{\"name\":\"Product1\",\"properties\":{\"Property2\":\"Value2\",\"Property1\":\"Value1\"}}";
    public static Product PRODUCT = new Product("Product1", createStringMap());
    public static class Product {
        public Product() {}
        public Product(String name, Map<String, String> properties) {
            this.name = name;
            this.properties = properties;
        }
        public String name;
        public Map<String, String> properties;
    }

    public static String TEAM_JSON = "{\"name\":\"Team1\",\"players\":{\"Player2\":{\"firstName\":\"Max\",\"lastName\":\"Mustermann\"},\"Player1\":{\"firstName\":\"John\",\"lastName\":\"Doe\"}}}";
    public static Team TEAM = new Team("Team1",createPersonMap());
    public static class Team {
        public Team() {}
        public Team(String name, Map<String, Person> players) {
            this.name = name;
            this.players = players;
        }
        public String name;
        public Map<String,Person> players;
    }

    private static Map<String, String> createStringMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("Property1", "Value1");
        map.put("Property2", "Value2");
        return map;
    }

    private static Map<String,Person> createPersonMap() {
        Map<String, Person> map = new HashMap<String, Person>();
        map.put("Player1", new Person("John", "Doe"));
        map.put("Player2", new Person("Max", "Mustermann"));
        return map;
    }

    public static String PRIMITIVE_JSON = "{\"number\":2,\"bool\":true,\"ch\":\"a\",\"doub\":1.38,\"f\":2.04,\"l\":42,\"s\":2}";
    public static Primitive PRIMITIVE = new Primitive(2, true, 'a', 1.38d, 2.04f, 42l, (short) 2);
    public static class Primitive {
        public Primitive() {}
        public Primitive(int number, boolean bool, char ch, double doub, float f, long l, short s) {
            this.number = number;
            this.bool = bool;
            this.ch = ch;
            this.doub = doub;
            this.f = f;
            this.l = l;
            this.s = s;
        }
        public int number;
        public boolean bool;
        public char ch;
        public double doub;
        public float f;
        public long l;
        public short s;
    }

    public static String ENUMERATION_JSON = "{\"gender\":\"MALE\"}";
    public static Enumeration ENUMERATION = new Enumeration(Enumeration.Gender.MALE);
    public static class Enumeration {
        public Enumeration() {}
        public Enumeration(Gender gender) {
            this.gender = gender;
        }
        public enum Gender {
            MALE, FEMALE
        }
        public Gender gender;
    }

    public static String STATICFINAL_JSON = "{\"string\":\"test\"}";
    public static Staticfinal STATICFINAL = new Staticfinal("value1", "test");
    public static class Staticfinal {
        public Staticfinal() {}
        public Staticfinal(String value1, String string) {
            this.value1 = value1;
            this.string = string;
        }
        public static String value1;
        public final String value2 = "Foo";
        public static final String CONSTANT = "Constant";
        public String string;

    }

    public static Unsupported UNSUPPORTED = new Unsupported("value1", new StringBuffer("Buffer"));
    public static class Unsupported {
        public Unsupported() {}
        public Unsupported(String value1, StringBuffer buffer) {
            this.value1 = value1;
            this.buffer = buffer;
        }
        public String value1;
        public StringBuffer buffer;

    }

    public static String TEAM_INTEGER_JSON = "{\"name\":\"Team1\",\"players\":{\"1\":{\"firstName\":\"John\",\"lastName\":\"Doe\"},\"2\":{\"firstName\":\"Max\",\"lastName\":\"Mustermann\"}}}";
    public static TeamInteger TEAM_INTEGER = new TeamInteger("Team1",createPersonMapInteger());
    public static class TeamInteger {
        public TeamInteger() {}
        public TeamInteger(String name, Map<Integer, Person> players) {
            this.name = name;
            this.players = players;
        }
        public String name;
        public Map<Integer,Person> players;
    }

    private static Map<Integer,Person> createPersonMapInteger() {
        Map<Integer, Person> map = new HashMap<Integer, Person>();
        map.put(new Integer(1), new Person("John", "Doe"));
        map.put(new Integer(2), new Person("Max", "Mustermann"));
        return map;
    }
}
