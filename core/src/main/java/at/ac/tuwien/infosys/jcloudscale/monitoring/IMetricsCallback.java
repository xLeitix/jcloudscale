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

/**
 * Callback interface for getting notified about new monitoring values.
 * 
 * @author philipp
 *
 */
public interface IMetricsCallback {
	
	/**
	 * Users that want to register for callbacks from the metrics
	 * database should implement this method. It will be called whenever
	 * a new value is received.
	 * 
	 * @param metricName The name of the metric.
	 * @param value The value that has been received.
	 * @param timestamp The timestamp associated with this value.
	 */
	void valueReceived(String metricName, Object value, long timestamp);
	
}
