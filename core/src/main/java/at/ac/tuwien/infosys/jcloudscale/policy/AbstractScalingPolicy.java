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

import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;


/**
 * The class that determines the scaling rules of the application.
 * By implementing this abstract class and providing it to the configuration you can control 
 * where the cloud objects will be deployed and how they will be treated.  
 */
public abstract class AbstractScalingPolicy
{
	/**
	 * Selects the virtual host for the new cloud object.
	 * @param newCloudObject The descriptor of the new cloud object.
	 * @param hostPool The host pool that contains the set of available hosts and allows 
	 * to perform additional scaling operations on these hosts.
	 * @return The host that should be used to deploy new cloud object.
	 */
	public abstract IHost selectHost(ClientCloudObject newCloudObject, IHostPool hostPool);
	
	/**
	 * This method is called periodically 
	 * (with the period specified by <b>scaleDownInterval</b> from common configuration) 
	 * to perform scaling down and cloud usage optimization tasks. 
	 * @param hostPool The host pool that contains the set of available hosts and allows 
	 * to perform additional scaling operations on these hosts. 
	 * @param scaledHost Indicates the host that reached the next <b>scaleDownInterval</b>.
	 * @return <b>true</b> if the specified host should be scaled down. Otherwise, 
	 * if the specified host has to stay online for another scaling interval, <b>false</b>. 
	 */
	public abstract boolean scaleDown(IHost scaledHost, IHostPool hostPool);
}
