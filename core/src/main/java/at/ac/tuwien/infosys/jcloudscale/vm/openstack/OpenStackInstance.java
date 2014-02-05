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
package at.ac.tuwien.infosys.jcloudscale.vm.openstack;

import at.ac.tuwien.infosys.jcloudscale.logging.Logged;
import at.ac.tuwien.infosys.jcloudscale.vm.IdManager;
import at.ac.tuwien.infosys.jcloudscale.vm.localVm.LocalVM;

@Logged
public class OpenStackInstance extends LocalVM {
	
	public OpenStackInstance(OpenstackCloudPlatformConfiguration config, IdManager idManager, boolean startPerformanceMonitoring) 
	{
		super(idManager, startPerformanceMonitoring);
		this.config = config;
	}
	
	@Override
	protected void launchHost(String size) { 
		this.instanceSize = size;
		((OpenstackCloudPlatformConfiguration)config).getOpenstackWrapper().startNewHost(size);
	}
	
	@Override 
	public void close() { 
		super.close();
		if(!isStaticHost())
			((OpenstackCloudPlatformConfiguration)config).getOpenstackWrapper().shutdownHost(serverIp);
	}
	
}
