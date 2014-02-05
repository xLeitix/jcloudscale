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
package at.ac.tuwien.infosys.jcloudscale.sample.service.jcloudscale;

import at.ac.tuwien.infosys.jcloudscale.policy.IScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;

/**
 * Defines the scaling rules of the jcloudscale application
 */
public class ScalingPolicy implements IScalingPolicy {

	@Override
	public boolean scaleDown(IHost host, IHostPool hostPool) {
		return false;
	}

	@Override
	public synchronized IHost selectHost(ClientCloudObject co, IHostPool hostPool) 
	{
		//
		// If we have used less hosts than we can, let's use new one.
		//
		if(hostPool.getHostsCount() < JCloudScaleCodeFactory.staticHostsCount || 
			(JCloudScaleCodeFactory.staticHostsCount == 0 && hostPool.getHostsCount() == 0))
				return hostPool.startNewHostAsync();
		
		//
		// selecting least loaded host
		//
		IHost leastLoadedHost = null;
		for(IHost host : hostPool.getHosts())
			if(leastLoadedHost == null || host.getCloudObjectsCount() < leastLoadedHost.getCloudObjectsCount())
				leastLoadedHost = host;
		
		return leastLoadedHost;
	}

}
