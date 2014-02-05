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

import java.io.Closeable;
import java.util.Collection;
import java.util.UUID;

import at.ac.tuwien.infosys.jcloudscale.policy.AbstractEventDrivenScalingPolicy;

/**
 * This is the general interface for databases of monitoring metric values
 * in JCloudScale. The database supports both, a pull (retrieve values per
 * metric name) and a push (register callbakc listeners) model of data
 * retrieval.
 * 
 * <p>
 * 
 * Conceptually, this database is read-only. No functionality is provided to
 * remove values once they are in the database. However, different
 * implementations will employ different strategies to timeout and remove old
 * values. 
 * 
 * <p>
 * 
 * Users should only use the database to query data. Although methods are provided
 * to also write to the database, most applications should only query and/or
 * register for values. The main source of values should be the JCloudScale
 * CEP engine ({@link at.ac.tuwien.infosys.jcloudscale.monitoring.EventCorrelationEngine}).
 * 
 * @author philipp
 *
 */
public interface IMetricsDatabase extends Closeable {

	/**
	 * Adds a new value to the metrics database. Every value needs to
	 * have an associated timestamp. If the overload without timestamp
	 * is used, the current system time is used as timestamp.
	 * 
	 * <p/>
	 * 
	 * Note that, in principle, only the JCloudScale CEP engine is
	 * supposed to add values to the database. For other consumers,
	 * the database should be considered read-only.
	 * 
	 * @param metricName The name of the metric to add a value to. 
	 * @param value The concrete value to add. For most use cases,
	 * all values of a given metric should have the same runtime type,
	 * but the metrics database should not be expected to enforce this.
	 * @param timestamp The timestamp to associate with this value
	 */
	void addValue(String metricName, Object value, long timestamp);
	
	/**
	 * Adds a new value to the metrics database. Every value needs to
	 * have an associated timestamp. If the overload without timestamp
	 * is used, the current system time is used as timestamp.
	 * 
	 * <p/>
	 * 
	 * Note that, in principle, only the JCloudScale CEP engine is
	 * supposed to add values to the database. For other consumers,
	 * the database should be considered read-only.
	 * 
	 * @param metricName The name of the metric to add a value to. 
	 * @param value The concrete value to add. For most use cases,
	 * all values of a given metric should have the same runtime type,
	 * but the metrics database should not be expected to enforce this.
	 */
	void addValue(String metricName, Object value);
	
	/**
	 * Get all values for a given metric. The returned values will be sorted
	 * according to their associated timestamp (descending).
	 * 
	 * @param metricName The metric to get the values for.
	 * @return A sorted list of values (sorted descending after their timestamp).
	 */
	Collection<Object> getValues(String metricName);
	
	/**
	 * Get all values for a given metric that are younger than a given timestamp.
	 * The returned values will be sorted according to their associated timestamp (descending).
	 * 
	 * @param metricName The metric to get the values for.
	 * @param timestamp Get all values that are younger than this timestamp.
	 * @return A sorted list of values (sorted descending after their timestamp).
	 */
	Collection<Object> getValuesSince(String metricName, long timestamp);
	
	/**
	 * Get the very last value that has been received for a given metric. This is a
	 * useful helper, as very often users will be interested not in the historic trend
	 * of a metric, but only in what the value is right now (or the closest approximation
	 * of 'right now', which is the last value we know of).
	 * 
	 * @param metricName The metric to get the last value for.
	 * @return The last known value for this metric.
	 */
	Object getLastValue(String metricName);
	
	/**
	 * As all the getters of MetricsDatabase only return values (but not their
	 * timestamp), this method allows to get the timestamp associated with
	 * a concrete value for a metric.
	 * 
	 * @param metricName The name of the metric that this value is associated with.
	 * @param value The concrete value to get the timestamp for.
	 * @return The timestamp of this value.
	 */
	long getTimestampToValue(String metricName, Object value);
	
	/**
	 * Register a new callback listener for a metric. Note that you need to use
	 * exactly the same string name as the name of the metric is going to be.
	 * Currently, there is no subscription or pattern language for mass-subscibing
	 * more than one metric at the same time.
	 * 
	 * @param metricName The name of the metric to register for.
	 * @param callback An implementation of {@link at.ac.tuwien.infosys.jcloudscale.monitoring.IMetricsCallback}
	 * passed by the user. This callback will be notified whenever a new value comes in.
	 * @return An subscription ID. This is mainly useful if users want to unregister at a
	 * later point.
	 */
	UUID registerCallbackForMetric(String metricName, IMetricsCallback callback);
	
	/**
	 * Unregister a callback. Callbacks are identified via their callback ID, which is
	 * generated and returned as part of the registration process.
	 * @param metricName The name of the metric that this callback was registered to.
	 * @param registrationId The registration ID that has been returned when this listener
	 * was originally registered.
	 */
	void unregisterCallbackForMetric(String metricName, UUID registrationId);
	
	/**
	 * Register a new event-driven scaling policy, which should be triggered whenever a given value for
	 * a monitoring metric is received.
	 * 
	 * @param metricName The name of the metric to register for.
	 * @param callback An implementation of {@link at.ac.tuwien.infosys.jcloudscale.policy.IEventDrivenScalingPolicy}
	 * passed by the user. This callback will be notified whenever a new value comes in.
	 * @return An subscription ID. This is mainly useful if users want to unregister at a
	 * later point.
	 */
	UUID registerEventDrivenScalingPolicy(String metricName, AbstractEventDrivenScalingPolicy policy);	
	
	/**
	 * Unregister an event-driven scaling policy. Scaling policiesare identified via their ID,
	 * which is
	 * generated and returned as part of the registration process.
	 * @param metricName The name of the metric that this policy was registered to.
	 * @param registrationId The registration ID that has been returned when this listener
	 * was originally registered.
	 */
	void unregisterEventDrivenScalingPolicy(String metricName, UUID registrationId);
}
