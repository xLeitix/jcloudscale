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
package at.ac.tuwien.infosys.jcloudscale.vm.ec2;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import sun.misc.BASE64Encoder;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
 
@SuppressWarnings("restriction")
public class EC2Wrapper {
	
	private AmazonEC2 ec2Client = null;
	private AWSCredentials creds = null;
	private EC2CloudPlatformConfiguration config = null;
	private Logger log = null;
	
	EC2Wrapper(EC2CloudPlatformConfiguration config) throws IOException 
	{
		this.config = config;
		this.log = JCloudScaleConfiguration.getLogger(this);
		
		// connect to EC2
		InputStream credentialsAsStream = new FileInputStream(config.getAwsConfigFile()); 
		creds = new PropertiesCredentials(credentialsAsStream);
		ec2Client = new AmazonEC2Client(creds);
		ec2Client.setEndpoint(config.getAwsEndpoint());
	}
	
	public void startNewHost(String size) {
		
		// TODO: this is more efficient the way we do it in the OS wrapper
		String imageId = lookupImageId(config.getImageName());
		
		if(imageId == null) 
		{
			String message = "Cannot scale up as requested since no valid " +
							"EC2 image could be found for name " + config.getImageName(); 
            log.severe(message);
            throw new JCloudScaleException(message);
		}

	    String flavId = null;
	    if(size == null)
	    	flavId = config.getInstanceType();
	    else
	    	flavId = size;
	    
	    String userData = config.generateHostUserData();
	    
        BASE64Encoder enc = new BASE64Encoder();
	    RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
	    	// instances in EC2 don't have a name
	    	.withInstanceType(flavId)
	    	.withImageId(imageId)
	    	.withMinCount(1)
	    	.withMaxCount(1)
	    	.withKeyName(config.getSshKey())
	    	.withUserData(enc.encode(userData.getBytes()));
	    	// .withUserData(userData);

	    try {
	    	ec2Client.runInstances(runInstancesRequest);
	    } catch(Exception e) {
	    	throw new JCloudScaleException("Unable to scale up using Amazon EC2: "+e.getMessage());
	    }
	}
	
	/**
	 * Starts MQ server with specified name.
	 * @param serverName The name of the server to start. Consider it to be unique.
	 * @return OpenStack internal id if server was start up successfully. Otherwise, <b>null</b>.
	 */
	public String startNewMessageQeueHost()
	{
		
		String mqImageId = lookupImageId(config.getMqImageName());
		
		if(mqImageId == null) 
		{
			String message = "Cannot scale up as requested since no valid " +
					"EC2 image could be found for name " + config.getMqImageName(); 
		    log.severe(message);
		    throw new JCloudScaleException(message);
		}

	    String flavId = config.getMqInstanceTypeName();

	    RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
	    	// instances in EC2 don't have a name
	    	.withInstanceType(flavId)
	    	.withImageId(mqImageId)
	    	.withMinCount(1)
	    	.withMaxCount(1)
	    	.withKeyName(config.getSshKey());

	    RunInstancesResult result = null;
	    try {
	    	result = ec2Client.runInstances(runInstancesRequest);
	    } catch(Exception e) {
	    	throw new JCloudScaleException("Unable to instantiate MQ image using Amazon EC2: "+e.getMessage());
	    }
	    
	    String instanceId = result.getReservation().getInstances().get(0).getInstanceId();
	    
	    // TODO how do we associate the correct IP here?
	    
	    return instanceId;
	}
		
	String lookupImageId(String imgName) 
	{
		
		DescribeImagesRequest req = new DescribeImagesRequest();
		
		if(imgName == null || imgName.length() == 0)
			return null;
		
		req.setRequestCredentials(creds);
		DescribeImagesResult result = ec2Client.describeImages();
		for(Image image : result.getImages()) {
			if(image.getName() != null && image.getName().equals(imgName)) 
				return image.getImageId();
		}
		return null;
	}
	
	/**
	 * Finds the first instance with the specified IP address.
	 * @param ip
	 * @return
	 */
	public String findEC2Id(String ip) 
	{
		DescribeInstancesResult result = ec2Client.describeInstances();
		for(Reservation r : result.getReservations()) {
			for(Instance i : r.getInstances()) {
				if((i.getPublicIpAddress() != null && i.getPublicIpAddress().equals(ip)) || 
					(i.getPrivateIpAddress() != null && i.getPrivateIpAddress().equals(ip)))
					return i.getInstanceId();
			}
		}
		return null;
	}
	
	public boolean isHostRunningViaIP(String ip)
	{
		String ec2Id = findEC2Id(ip);
		
		if(ec2Id == null)
			return false;
		
		return isHostRunningViaId(ec2Id);
	}

	private boolean isHostRunningViaId(String ec2Id)
	{
		DescribeInstancesRequest req = new DescribeInstancesRequest()
			.withInstanceIds(ec2Id);
		DescribeInstancesResult result = ec2Client.describeInstances(req);
		for(Reservation r : result.getReservations()) {
			for(Instance i : r.getInstances()) {
				if(i.getInstanceId().equals(ec2Id) && i.getState().getName().equalsIgnoreCase("active"))
					return true;
			}
		}
		return false;
	}
	
	public void shutdownHost(String ip) {
		
		// find EC2 id to this IP address
		String ec2Id = findEC2Id(ip);
		
		if(ec2Id == null) {
			log.warning("Cannot shutdown host with IP "+ip+" - no such server found.");
			return;
		}

		shutdownHostViaEC2Id(ec2Id);
	}
	
	void shutdownHostViaEC2Id(String ec2Id)
	{
		// now destroy the server with this EC2 id
		TerminateInstancesRequest req = new TerminateInstancesRequest()
			.withInstanceIds(ec2Id);
		ec2Client.terminateInstances(req);
		
		// waiting for server to actually start shutting down.
		try
		{
			final int SLEEP_TIME = 100; 
			int timeout = 100;//10 sec should be enough.
			do
			{
				Thread.sleep(SLEEP_TIME);
			}
			while(timeout--> 0 && isHostRunningViaId(ec2Id));
			
			if(timeout <= 0)
				log.warning("While Shutting down host with EC2 Id="+ec2Id+" waited for timeout and server is still active.");
		}
		catch(InterruptedException e)
		{
		}
	}

	public String getInstanceSize(String id) {
		
		DescribeInstancesRequest req = new DescribeInstancesRequest()
			.withInstanceIds(id);
		DescribeInstancesResult result = ec2Client.describeInstances(req);
		for(Reservation r : result.getReservations()) {
			for(Instance i : r.getInstances()) {
				if(i.getInstanceId().equals(id))
					return i.getInstanceType();
			}
		}
		return null;
		
	}
	
}
