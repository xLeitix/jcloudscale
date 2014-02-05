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
package at.ac.tuwien.infosys.jcloudscale.policy;

import at.ac.tuwien.infosys.jcloudscale.monitoring.EventCorrelationEngine;
import at.ac.tuwien.infosys.jcloudscale.monitoring.IMetricsDatabase;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;

/**
 * A special type of a {@link at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy}, which gets triggered not only on
 * cloud object creation and during host teardown checks (like a regular scaling policy), but which can also be
 * triggered by arbitrary events of the {@link at.ac.tuwien.infosys.jcloudscale.monitoring.EventCorrelationEngine}. Such
 * policies allow finely controlled scaling policies, which get triggered directly by the application.
 */
public abstract class AbstractEventDrivenScalingPolicy extends AbstractScalingPolicy
{
	
	/**
	 * This method gets triggered whenever the scaling policy receives an event that it is registered for. For this to
	 * work, the scaling policy needs to be registered at the {@link IMetricsDatabase} implementation managed by the
	 * {@link EventCorrelationEngine}. 
	 * 
	 * @param hostPool The current state of the virtual host pool. Scaling policies are allowed to change this, i.e.,
	 * start new hosts, terminate hosts, or initiate cloud object migration
	 * @param triggeringValue The monitoring value that triggered that event-driven scaling policy
	 * @param timestamp The timestamp of the received monitoring value
	 */
	public abstract void onEvent(IHostPool hostPool, Object triggeringValue, long timestamp);
	
}
