/*
   Copyright 2014 Philipp Leitner 

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
package at.ac.tuwien.infosys.jcloudscale.test.unit;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import at.ac.tuwien.infosys.jcloudscale.utility.ReferenceHashmap;

public class TestReferenceHashmap {
	
	private ReferenceHashmap<BrokenKey, BrokenValue> map;
	
	@Before
	public void setup() {
		map = new ReferenceHashmap<>();
	}
	
	@Test
	public void testSize() {
		
		BrokenKey key = new BrokenKey(); 
		
		assertEquals(0, map.size());
		map.put(key, new BrokenValue());
		assertEquals(1, map.size());
		map.put(new BrokenKey(), new BrokenValue());
		assertEquals(2, map.size());
		map.remove(key);
		assertEquals(1, map.size());
		map.clear();
		assertEquals(0, map.size());
		
		
	}
	
	@Test
	public void testIsEmpty() {
		
		assertTrue(map.isEmpty());
		map.put(new BrokenKey(), new BrokenValue());
		assertFalse(map.isEmpty());
		map.clear();
		assertTrue(map.isEmpty());
		
	}
	
	@Test
	public void testContainsKey() {
		
		BrokenKey key = new BrokenKey(); 
		
		assertEquals(0, map.size());
		map.put(key, new BrokenValue());
		assertEquals(1, map.size());
		map.put(new BrokenKey(), new BrokenValue());
		assertEquals(2, map.size());
		assertTrue(map.containsKey(key));
		
	}
	
	@Test
	public void testContainsValue() {
		
		BrokenKey key = new BrokenKey();
		BrokenValue value = new BrokenValue();
		
		assertEquals(0, map.size());
		map.put(key, value);
		assertEquals(1, map.size());
		map.put(new BrokenKey(), new BrokenValue());
		assertEquals(2, map.size());
		assertTrue(map.containsValue(value));
		
	}
	
	@Test
	public void testGetAndPut() {
		
		BrokenKey key = new BrokenKey();
		BrokenValue value = new BrokenValue();
		
		assertEquals(0, map.size());
		map.put(key, value);
		assertEquals(1, map.size());
		map.put(new BrokenKey(), new BrokenValue());
		assertEquals(2, map.size());
		assertTrue(map.get(key) == value);
		
	}
	
	@Test(expected = RuntimeException.class)
	public void testFailing() {
		
		BrokenKey key = new BrokenKey();
		BrokenValue value = new BrokenValue();
		
		HashMap<BrokenKey, BrokenValue> oldMap =
				new HashMap<>();
				
		oldMap.put(key , value);
		
	}
	
	
	private static class BrokenKey {
		
		@Override
		public int hashCode() {
			throw new RuntimeException("hashCode called");
		}
		
		@Override
		public boolean equals(Object other) {
			throw new RuntimeException("equals called");
		}
		
	}
	
	private static class BrokenValue {
		
		@Override
		public int hashCode() {
			throw new RuntimeException("hashCode called");
		}
		
		@Override
		public boolean equals(Object other) {
			throw new RuntimeException("equals called");
		}
		
	}
	
}

