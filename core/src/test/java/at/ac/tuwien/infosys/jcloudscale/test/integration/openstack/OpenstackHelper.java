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
package at.ac.tuwien.infosys.jcloudscale.test.integration.openstack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import scala.actors.threadpool.Arrays;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IdManager;
import at.ac.tuwien.infosys.jcloudscale.vm.openstack.OpenStackWrapper;
import at.ac.tuwien.infosys.jcloudscale.vm.openstack.OpenstackCloudPlatformConfiguration;

public class OpenstackHelper 
{
	public static final int STATIC_HOSTS_COUNT = 3;
	
	private List<String> staticHosts = new ArrayList<>();
	private OpenStackWrapper osWrapper;
	private MessageQueueConfiguration mqConfig;
	private CloudPlatformConfiguration cloudPlatformConfiguration;
	private AutoCloseable communicationServer;

	public OpenstackHelper(JCloudScaleConfiguration config)
	{
		this.cloudPlatformConfiguration = config.server().cloudPlatform();
		this.mqConfig = config.common().communication();
		osWrapper = ((OpenstackCloudPlatformConfiguration)config.server().cloudPlatform()).getOpenstackWrapper();
	}
	
	public void startStaticInstances() throws InterruptedException
	{
		System.out.println("Ensuring Communication Server is running...");
		try 
		{
			communicationServer = this.cloudPlatformConfiguration.ensureCommunicationServerRunning();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			throw new RuntimeException("Failed to ensure communication server is running.", e);
		}
		
		//
		// starting static hosts.
		//
		try(IdManager idManager = new IdManager(mqConfig))
		{
		        int existingHosts = idManager.getRegisteredInstances().size();
		        if(existingHosts > 0)
		            System.out.println("Detected "+existingHosts+" cloud hosts running already!");
		        
	                if(existingHosts < STATIC_HOSTS_COUNT)
	                {
	                    int newHostsCount = STATIC_HOSTS_COUNT - existingHosts;
	                    System.out.println("Starting "+newHostsCount+" new static hosts...");
	                    for(int i=0; i<newHostsCount; ++i)
                                osWrapper.startNewHost(null);
	                }
	                else
	                    System.out.println("Using already running hosts.");
	                
		        
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
			try {
				osWrapper.shutdownHost(hostIp);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Shutting down communication server...");
		if(communicationServer != null)
			try 
			{
				communicationServer.close();
			} catch (Exception e) 
			{
				e.printStackTrace();
			}
	}
	
	public boolean verifyWithOpenstack(IVirtualHost host, String instanceSize) {
		String ip = host.getIpAddress();
		String id = osWrapper.findOpenStackIdByIP(ip);
		String size = osWrapper.getInstanceSize(id);
		return instanceSize.equals(size);
	}
}
