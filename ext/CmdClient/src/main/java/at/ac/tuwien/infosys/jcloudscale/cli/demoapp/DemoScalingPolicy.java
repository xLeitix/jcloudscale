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
package at.ac.tuwien.infosys.jcloudscale.cli.demoapp;

import at.ac.tuwien.infosys.jcloudscale.policy.AbstractScalingPolicy;
import at.ac.tuwien.infosys.jcloudscale.vm.ClientCloudObject;
import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;

public class DemoScalingPolicy extends AbstractScalingPolicy {

	@Override
	public IHost selectHost(ClientCloudObject newCloudObject,
			IHostPool hostPool) 
	{
		// we reuse a host that has 0 running objects on it at the moment
		for(IHost host : hostPool.getHosts())
			if(host.getCloudObjectsCount() == 0)
				return host;
		
		return null;	
	}

	@Override
	public boolean scaleDown(IHost scaledHost, IHostPool hostPool) 
	{
		return false;
	}

}
