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
package at.ac.tuwien.infosys.jcloudscale.policy.sample;

import at.ac.tuwien.infosys.jcloudscale.logging.Logged;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;

/**
 * This policy uses same host for all cloud objects and never scales down.
 */
@Logged
public class SingleHostScalingPolicy extends HostPerObjectScalingPolicy {
	
	@Override
	public IHost selectHost(ClientCloudObject cloudObject, IHostPool pool) {
		
		if(pool.getHostsCount() == 0)
			return pool.startNewHost();
		
		return pool.getHosts().iterator().next();
	}
	
}
