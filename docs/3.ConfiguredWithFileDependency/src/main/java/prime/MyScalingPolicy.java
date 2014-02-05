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
package prime;

import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;

/**
 * Simple scaling policy that always selects the first host.
 */
@XmlRootElement
public class MyScalingPolicy extends AbstractScalingPolicy {

	// The method is synchronized to avoid race conditions between different cloud objects 
	// being scheduled for execution at the same time.
	@Override
	public synchronized IHost selectHost(ClientCloudObject newCloudObject, IHostPool hostPool) {
		if(hostPool.getHostsCount() > 0)
		{
			IHost selectedHost = hostPool.getHosts().iterator().next();
			System.out.println("SCALING: Deploying new object "+ 
								newCloudObject.getCloudObjectClass().getName() +
								" on "+selectedHost.getId());
			
			return selectedHost;
		}
		else
		{
			System.out.println("SCALING: Deploying new object "+
									newCloudObject.getCloudObjectClass().getName() +
									" on new virtual machine.");
			// Here we return a host started asynchronously to minimize time inside synchronized section.
			return hostPool.startNewHostAsync();
		}
	}

	@Override
	public boolean scaleDown(IHost scaledHost, IHostPool hostPool) {
		// We will not scale down for this sample application as 
		// JCloudScale will shut down all hosts at the end, but you may need that.
		return false;
	}
}
