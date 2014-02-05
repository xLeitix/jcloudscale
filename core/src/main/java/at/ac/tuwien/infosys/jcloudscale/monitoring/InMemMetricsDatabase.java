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
package at.ac.tuwien.infosys.jcloudscale.monitoring;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.policy.AbstractEventDrivenScalingPolicy;

public class InMemMetricsDatabase implements IMetricsDatabase {

	private HashMap<String, SortedMap<Long, Object>> values;
	private HashMap<String, HashMap<Object, Long>> valuesReverse;
	
	private HashMap<String, HashMap<UUID, IMetricsCallback>> callbacks;
	private HashMap<String, HashMap<UUID, AbstractEventDrivenScalingPolicy>> policies;
	
	public InMemMetricsDatabase() {
		purgeDatabase();
	} 
	
	@Override
	public void addValue(String metricName, Object value, long timestamp) {
		
		// add to values
		if(!values.containsKey(metricName))
			values.put(metricName, new TreeMap<>(new InverseLongComparator()));
		SortedMap<Long, Object> thisVals = values.get(metricName);
		thisVals.put(timestamp, value);
		
		// add to valuesReverse
		if(!valuesReverse.containsKey(metricName))
			valuesReverse.put(metricName, new HashMap<Object, Long>());
		HashMap<Object, Long> thisValsReverse = valuesReverse.get(metricName);
		thisValsReverse.put(value, timestamp);
		
		// notify callbacks
		if(callbacks.containsKey(metricName)) {
			for(IMetricsCallback callback : callbacks.get(metricName).values()) {
				callback.valueReceived(metricName, value, timestamp);
			}
		}
		
		// notify registered policies
		if(policies.containsKey(metricName)) {
			for(AbstractEventDrivenScalingPolicy policy : policies.get(metricName).values()) {
				policy.onEvent(CloudManager.getInstance().getHostPool(), value, timestamp);
			}
		}
		
	}

	@Override
	public void addValue(String metricName, Object value) {
		
		addValue(metricName, value, System.currentTimeMillis());
		
	}

	@Override
	public Collection<Object> getValues(String metricName) {
		
		if(!values.containsKey(metricName))
			return null;
		return values.get(metricName).values();
		
	}

	@Override
	public Collection<Object> getValuesSince(String metricName, long timestamp) {
		
		if(!values.containsKey(metricName))
			return null;
		SortedMap<Long, Object> map = values.get(metricName);
		map = map.headMap(timestamp);
		return map.values();
		
	}

	@Override
	public Object getLastValue(String metricName) {
		
		if(!values.containsKey(metricName))
			return null;
		SortedMap<Long, Object> map = values.get(metricName);
		return map.values().iterator().next();
		
	}

	@Override
	public long getTimestampToValue(String metricName, Object value) {
		
		if(!valuesReverse.containsKey(metricName))
			throw new IllegalArgumentException("Metric "+metricName+" is unknown");
		
		HashMap<Object, Long> vals = valuesReverse.get(metricName);
		if(!vals.containsKey(value))
			throw new IllegalArgumentException("Object "+value.toString()+" is not a known value of metric "+metricName);
		
		return vals.get(value);
		
	}

	@Override
	public UUID registerCallbackForMetric(String metricName,
			IMetricsCallback callback) {
		
		if(!callbacks.containsKey(metricName))
			callbacks.put(metricName, new HashMap<UUID, IMetricsCallback>());
		
		HashMap<UUID, IMetricsCallback> theCallbacks = callbacks.get(metricName);
		UUID regId = UUID.randomUUID();
		theCallbacks.put(regId, callback);
		
		return regId;
		
	}

	@Override
	public void unregisterCallbackForMetric(String metricName, UUID registrationId) {
		
		if(!callbacks.containsKey(metricName))
			throw new IllegalArgumentException("Metric "+metricName+" does not have registered callbacks");
		
		HashMap<UUID, IMetricsCallback> theCallbacks = callbacks.get(metricName);
		if(!theCallbacks.containsKey(registrationId))
			throw new IllegalArgumentException("Callback "+registrationId+" unknown for metric "+metricName);
		
		theCallbacks.remove(registrationId);
		
	}
	
	@Override
	public UUID registerEventDrivenScalingPolicy(String metricName,
			AbstractEventDrivenScalingPolicy policy) {

		if(!policies.containsKey(metricName))
			policies.put(metricName, new HashMap<UUID, AbstractEventDrivenScalingPolicy>());
		
		HashMap<UUID, AbstractEventDrivenScalingPolicy> theCallbacks = policies.get(metricName);
		UUID regId = UUID.randomUUID();
		theCallbacks.put(regId, policy);
		
		return regId;
		
	}

	@Override
	public void unregisterEventDrivenScalingPolicy(String metricName,
			UUID registrationId) {

		if(!policies.containsKey(metricName))
			throw new IllegalArgumentException("Metric "+metricName+" does not have registered policies");
		
		HashMap<UUID, AbstractEventDrivenScalingPolicy> thePolicies = policies.get(metricName);
		if(!thePolicies.containsKey(registrationId))
			throw new IllegalArgumentException("Policy "+registrationId+" unknown for metric "+metricName);
		
		thePolicies.remove(registrationId);
		
	}
	
	public void purgeDatabase() {
		values = new HashMap<>();
		valuesReverse = new HashMap<>();
		callbacks = new HashMap<>();
		policies = new HashMap<>();
	}
	
	static class InverseLongComparator implements Comparator<Long>, Serializable {

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(Long o1, Long o2) {
			
			return o1.compareTo(o2) * -1;
			
		}
	}

	@Override
	public void close() throws IOException {
		purgeDatabase();
	}
	
}
