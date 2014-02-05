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

import java.util.Date;
import java.util.logging.Logger;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.openstack.v2_0.domain.Resource;
import org.jclouds.rest.RestContext;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;

public class OpenStackWrapper {
	
	private static final String NOVA_PROVIDER = "openstack-nova";
	
	private ComputeServiceContext context = null;
	private RestContext<NovaApi, NovaAsyncApi> nova;
	private ServerApi openstack;
	private FloatingIPApi floatingIp;
	private FlavorApi flavors;
	private ImageApi images;
	private OpenstackCloudPlatformConfiguration config = null;
	private Logger log = null;
	
	@SuppressWarnings("deprecation")
	OpenStackWrapper(OpenstackCloudPlatformConfiguration config) 
	{
		this.config = config;
		this.log = JCloudScaleConfiguration.getLogger(this);
		
		String identity = config.getTenantName()+":"+config.getLogin();
		
		ContextBuilder builder = ContextBuilder.newBuilder(NOVA_PROVIDER)
                 .credentials(identity, config.getPassword())
                 .endpoint(config.getIdentityPublicURL());
		context = builder.buildView(ComputeServiceContext.class);
		nova = context.unwrap();
		openstack = nova.getApi().getServerApiForZone(nova.getApi().getConfiguredZones().toArray(new String[0])[0]);
		floatingIp = nova.getApi().getFloatingIPExtensionForZone(nova.getApi().getConfiguredZones().toArray(new String[0])[0]).get();
		flavors = nova.getApi().getFlavorApiForZone(nova.getApi().getConfiguredZones().toArray(new String[0])[0]);
		images = nova.getApi().getImageApiForZone(nova.getApi().getConfiguredZones().toArray(new String[0])[0]);
		
	}
	
	@SuppressWarnings("deprecation")
	public void startNewHost(String size) {
		
		if(config.getImageId() == null) 
		{
			String message = "Cannot scale up as requested since no valid " +
							"Openstack image could be found for name " + config.getImageName(); 
            log.severe(message);
            throw new JCloudScaleException(message);
		}

	    String flavId = null;
	    if(size == null)
	    	flavId = config.getInstanceTypeId();
	    else
	    	// TODO: for performance reasons we should be caching that
	    	flavId = lookupFlavorId(size);
	
	    if(flavId == null)
	            throw new JCloudScaleException("Unknown instance type: "+config.getNewInstanceTypeName());

//	    Date date = new Date();
//	    ServerForCreate sfc = new ServerForCreate();
//	    sfc.setName("JCloudScale Server "+date.toGMTString());
//	    sfc.setFlavorRef(flavId);
//	    sfc.setImageRef(config.getImageId());
//	    sfc.setKeyName(config.getSshKey());
	    
	    
	    String userData = config.generateHostUserData();
	    CreateServerOptions options = new CreateServerOptions();
	    if(config.getSshKey() != null) {
	    	options.keyPairName(config.getSshKey());
	    }
	    options.userData(userData.getBytes());
	    Date date = new Date();
	    openstack.create(
	    	"JCloudScale Server "+date.toGMTString(),
	    	config.getImageId(),
	    	flavId,
	    	options);
	}
	
	/**
	 * Starts MQ server with specified name.
	 * @param serverName The name of the server to start. Consider it to be unique.
	 * @return OpenStack internal id if server was start up successfully. Otherwise, <b>null</b>.
	 */
	public String startNewMessageQeueHost(String serverName, String mqServerIpAddress)
	{
		if(config.getMqImageId() == null) 
		{
			String message = "Cannot scale up as requested since no valid " +
					"Openstack image could be found for name " + config.getImageName(); 
		    log.severe(message);
		    throw new JCloudScaleException(message);
		}

	    String flavId = config.getMqInstanceTypeId();
	
	    if(flavId == null)
	            throw new JCloudScaleException("Unknown instance type: "+config.getMqInstanceTypeName());

	    CreateServerOptions options = new CreateServerOptions();
	    options.keyPairName(config.getSshKey());
	    String osId = openstack.create(
	    	serverName,
	    	config.getMqImageId(),
	    	flavId,
	    	options).getId();
	    
//	    String osId = findOpenStackIdByName(serverName);
	    
	    if(osId == null)
	    	throw new JCloudScaleException("Failed to start Message Queue host: The started host was not found in the running hosts list.");
	    
	    //
	    // Associating floating Ip.
	    //
	    //TODO: should we check if mqServerIpAddress is IP and if we have it in our IP pool?
	    try 
	    {
	    	
	    	// find floating IP
//	    	FluentIterable<? extends FloatingIP> ips = floatingIp.list();
//	    	String ipId = null;
//	    	for(FloatingIP ip : ips.toList()) {
//	    		if(ip.getIp().equalsIgnoreCase(mqServerIpAddress))
//	    			ipId = ip.getId();
//	    	}
	    	
	    	// wait for host to become online
	    	do {
	    		Thread.sleep(1000);
	    	} while(!isHostRunningViaId(osId));
	    	
		    do
		    {
				Thread.sleep(1000);
				floatingIp.addToServer(mqServerIpAddress, osId);
				
				// novaclient.execute(new AssociateFloatingIp(osId, new ServerAction.AssociateFloatingIp(mqServerIpAddress)));
		    }
		    while(!hostHasAddressAssociated(osId, mqServerIpAddress));//checking if we succeeded.
	    } catch (InterruptedException e) 
	    {
		}
	    	
	    return osId;
	}
	
//	private void executeStartServerCommand(ServerForCreate sfc)
//	{
//		try 
//		{
//	    	novaclient.execute(ServersCore.createServer(sfc));
//	    } catch(ClientErrorException e) 
//	    {
//	    	if(e.getResponse().getStatus() == 413) // in this case we ran out of quota
//	    		throw new JCloudScaleException("Unable to scale up. Server reported that we are out of quota");
//	    	else
//	    		throw new JCloudScaleException(e);
//	    }
//	}
		
	String lookupImageId(String imgName) 
	{
		if(imgName == null || imgName.length() == 0)
			return null;
		
		FluentIterable<? extends Resource> allImages = images.list().concat();
		
		for(Resource img : allImages.toList()) 
			if(img.getName().equals(imgName)) 
				return img.getId();

		return null;
	}
	
	String lookupFlavorId(String flavorName)
	{
		if(flavorName == null || flavorName.length() == 0)
			return null;
		
		FluentIterable<? extends Resource> flavs = flavors.list().concat();
		
	    for(Resource flavor : flavs.toList())
            if(flavor.getName().equalsIgnoreCase(flavorName)) 
                    return flavor.getId();
	    
	    return null;
	}
	
	/**
	 * Finds if the host, specified by OpenStack host id has the specified address associated with it.
	 * @param hostId The OpenStack id of the host.
	 * @param hostAddress The address that has to be found.
	 * @return <b>true</b> of the address is found. Otherwise -- false.
	 */
	private boolean hostHasAddressAssociated(String hostId, String hostAddress) 
	{
		try {
			
			Server theServer = openstack.get(hostId);
			if(theServer == null) {
				log.severe("Unable to check IP address for host "+hostId+" - host not found");
				return false;
			}
			
			Multimap<String, Address> addresses = theServer.getAddresses();
	    	for(Address address : addresses.values())
	    			if(hostAddress.equals(address.getAddr()))
	    					return true;
		} catch(Exception e) {
			log.info("Trying to get address for openstack instance "+hostId+" failed. Ignoring.");
		}
    	
		return false;
	}
	
	/**
	 * Finds the first instance with the specified IP address.
	 * @param ip
	 * @return
	 */
	public String findOpenStackIdByIP(String ip) 
	{
		
		FluentIterable<? extends Resource> servers = openstack.list().concat();
		
		for(Resource server : servers.toList()) {
			Server theServer = openstack.get(server.getId());
			if(hostHasAddressAssociated(theServer.getId(), ip))
				return theServer.getId();
		}
		
		log.severe("No Openstack host for IP "+ip+" found");
		return null;
	}
	
//	private String findOpenStackIdByName(String name) 
//	{
//		Servers servers = novaclient.execute(ServersCore.listServers(true));
//		for(Server server : servers.getList()) {
//			if(server.getName().equals(name))
//				return server.getId();
//		}
//		
//		return null;
//	}
	
	public boolean isHostRunningViaIP(String ip)
	{
		String internalId = findOpenStackIdByIP(ip);
		String osId = findOpenStackIdByIP(internalId);
		
		if(osId == null)
			return false;
		
		return isHostRunningViaId(osId);
	}

	private boolean isHostRunningViaId(String openstackId)
	{
		Server serverInfo = openstack.get(openstackId);
		
		if(serverInfo == null)
			return false;
		
		return serverInfo.getStatus().name().equalsIgnoreCase("active");
	}
	
	public void shutdownHost(String ip) {
		
		// find OS id to our internal id
		String osId = findOpenStackIdByIP(ip);
		
		if(osId == null) {
			log.warning("Cannot shutdown host with IP "+ip+" - no such server found.");
			return;
		}

		shutdownHostViaInternalId(osId);
	}
	
	void shutdownHostViaInternalId(String openstackId)
	{
		// now destroy the server with this OS id
		openstack.delete(openstackId);
		
		// waiting for server to actually start shutting down.
		try
		{
			final int SLEEP_TIME = 100; 
			int timeout = 100;//10 sec should be enough.
			do
			{
				Thread.sleep(SLEEP_TIME);
			}
			while(timeout--> 0 && isHostRunningViaId(openstackId));
			
			if(timeout <= 0)
				log.warning("While Shutting down host with OpenStack Id="+openstackId+" waited for timeout and server is still active.");
		}
		catch(InterruptedException e)
		{
		}
	}

	public String getInstanceSize(String id) {

		Server theServer = openstack.get(id);
		Resource flavor = theServer.getFlavor();
		return flavors.get(flavor.getId()).getName();
		
	}
	
}
