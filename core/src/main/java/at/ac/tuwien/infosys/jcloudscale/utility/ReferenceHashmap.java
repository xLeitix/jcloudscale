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
package at.ac.tuwien.infosys.jcloudscale.utility;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

public class ReferenceHashmap<K, V> implements Map<K, V> {

	private int size = 0;
	private HashMap<Integer, List<WeakReference<K>>> keys = 
		new HashMap<>();
	private HashMap<Integer, List<Entry<WeakReference<K>, V>>> values = 
		new HashMap<>();
		
	
	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return values.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsKey(Object key) {
		List<WeakReference<K>> _keys = keys.get(System.identityHashCode(key));
		if(_keys == null)
			return false;
		return containsKey(_keys, (K) key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsValue(Object value) {
		for(List<Entry<WeakReference<K>, V>> _values : values.values())
			if(containsValueInPair(_values, (V) value))
				return true;
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		List<Entry<WeakReference<K>, V>> _pairs = values.get(System.identityHashCode(key));
		if(_pairs == null)
			return null;
		return getKeyInPair(_pairs, (K) key);
	}

	@Override
	public V put(K key, V value) {
		
		int hash = System.identityHashCode(key);
		
		// add to key map
		List<WeakReference<K>> _keys = keys.get(hash);
		if(_keys == null) {
			_keys = new ArrayList<WeakReference<K>>();
			keys.put(hash, _keys);
		}
		_keys.add(new WeakReference<K>(key));
		
		// add to value map
		List<Entry<WeakReference<K>, V>> _entries = values.get(hash);
		if(_entries == null) {
			_entries = new ArrayList<>();
			values.put(hash, _entries);
		}
		_entries.add(new MapEntry<>(new WeakReference<K>(key), value));
		
		size++;
		
		return null;
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		
		V value = get(key);
		if(value == null)
			return null;
		
		int hash = System.identityHashCode(key); 
		
		// remove from keys
		removeFromKeys(keys.get(hash), (K) key);
		
		// remove from values
		removeFromValues(values.get(hash), (K) key);
		
		size--;
		return value;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new NotImplementedException("putAll not implemented");
	}

	@Override
	public void clear() {
		
		size = 0;
		keys.clear();
		values.clear();
		
	}

	@Override
	public Set<K> keySet() {
		
		throw new NotImplementedException("keySet not implemented");
		
	}

	@Override
	public Collection<V> values() {

		throw new NotImplementedException("values not implemented");
		
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		
		throw new NotImplementedException("entrySet not implemented");
		
	}
	
	private <O> boolean containsKey(List<WeakReference<O>> list, O element) {
		
		for(WeakReference<O> o : list) {
			if(o.get() == element)
				return true;
		}
		return false;
		
	}
	
	private boolean containsValueInPair(List<Entry<WeakReference<K>, V>> list, V element) {
		
		for(Entry<WeakReference<K>, V> entry : list) {
			if(entry.getValue() == element)
				return true;
		}
		return false;
		
	}
	
	private V getKeyInPair(List<Entry<WeakReference<K>, V>> list, K element) {
		
		for(Entry<WeakReference<K>, V> entry : list) {
			if(entry.getKey().get() == element)
				return entry.getValue();
		}
		return null;
		
	}
	
	private void removeFromKeys(List<WeakReference<K>> list, K key) {
		for(int i = 0; i < list.size(); i++) {
			if(list.get(i).get() == key) {
				list.remove(i);
				return;
			}
		}
	}
	
	private void removeFromValues(List<Entry<WeakReference<K>, V>> list, K key) {
		for(int i = 0; i < list.size(); i++) {
			if(list.get(i).getKey().get() == key) {
				list.remove(i);
				return;
			}
		}
	}
	
	static class MapEntry<K,V> implements Entry<K,V> {

		private K key;
		private V value;
		
		public MapEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			this.value = value;
			return value;
		}
		
	}
	
}
