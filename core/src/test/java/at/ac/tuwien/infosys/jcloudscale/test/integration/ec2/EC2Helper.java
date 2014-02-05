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
package at.ac.tuwien.infosys.jcloudscale.test.integration.ec2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import scala.actors.threadpool.Arrays;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IdManager;
import at.ac.tuwien.infosys.jcloudscale.vm.ec2.EC2CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.ec2.EC2Wrapper;

public class EC2Helper 
{
	public static final int STATIC_HOSTS_COUNT = 1;
	
	private List<String> staticHosts = new ArrayList<>();
	private EC2Wrapper ec2Wrapper;
	private MessageQueueConfiguration mqConfig;

	public EC2Helper(JCloudScaleConfiguration config)
	{
		this.mqConfig = config.common().communication();
		ec2Wrapper = ((EC2CloudPlatformConfiguration)config.server().cloudPlatform()).getEC2Wrapper();
	}
	
	public void startStaticInstances() throws InterruptedException
	{
		System.out.println("Starting "+STATIC_HOSTS_COUNT+" static hosts...");
		
		for(int i=0;i<STATIC_HOSTS_COUNT; ++i)
			ec2Wrapper.startNewHost(null);
		
//		//
//		// waiting for servers to actually start.
//		//
		try(IdManager idManager = new IdManager(mqConfig))
		{
			while(idManager.getRegisteredInstances().size() < STATIC_HOSTS_COUNT)
			{
				System.out.println("Waiting for hosts to start...");
				Thread.sleep(5000);
			}
			
			//
			// Saving static hosts ips
			//
			for(UUID hostId : idManager.getRegisteredInstances())
				staticHosts.add(idManager.getIpToId(hostId));
			
			System.out.printf("Started successfully %s static hosts: %s.%n", staticHosts.size(), Arrays.toString(staticHosts.toArray()));
		}
	}
	
	public void shudownStaticInstances()
	{
		System.out.printf("Shutting down %s static hosts: %s.%n", staticHosts.size(), Arrays.toString(staticHosts.toArray()));
		for(String hostIp : staticHosts)
		{
			System.out.println("Shutting down host "+hostIp+"...");
			ec2Wrapper.shutdownHost(hostIp);
		}
	}
	
	public boolean verifyWithAWS(IVirtualHost host, String instanceSize) {
		String ip = host.getIpAddress();
		String id = ec2Wrapper.findEC2Id(ip);
		String size = ec2Wrapper.getInstanceSize(id);
		return instanceSize.equals(size);
	}
	
	public void shutdownHost(IVirtualHost host) {
		System.out.println("Shutting down host "+host.getIpAddress()+"...");
		ec2Wrapper.shutdownHost(host.getIpAddress());
	}
}
