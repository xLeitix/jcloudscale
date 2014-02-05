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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IdManager;
import at.ac.tuwien.infosys.jcloudscale.vm.VirtualHostPool;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class EC2CloudPlatformConfiguration extends CloudPlatformConfiguration 
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * The EC2 user script template that creates a file with MQ server access credentials
	 */
	private static final String mqAddressDistributionUserScript = 
			"#!/bin/sh"+"\n"+	//have to use Linux separators here.
			"mkdir -p %s"+"\n"+	//creating directory, if it is missing.
    		"echo \"%s\" > %s";	//writing file.
	
	private String imageName = "JCloudScale"+"_v"+JCloudScaleConfiguration.CS_VERSION;// "JCloudScale_v0.1"
	private String mqImageName ="ActiveMQ";
	
	private String newInstanceType = "m1.tiny";
	private String newMqInstanceType = "m1.small";
	
	private String SshKey = "";//if empty -- don't attach anything.
	
	private String awsEndpoint;
	private String awsConfigFile;
	
	private transient EC2Wrapper ec2Wrapper = null;
	//----------------------------------------------------------------------
	
	public String getImageName()
	{
		return imageName;
	}
	
	public void setImageName(String name)
	{
		this.imageName = name;
	}
	
	public String getInstanceType()
	{
		return newInstanceType;
	}
	
	public void setInstanceType(String type)
	{
		this.newInstanceType = type;
	}
	
	public String getMqImageName() 
	{
		return mqImageName;
	}

	public void setMqImageName(String imageName) 
	{
		this.mqImageName = imageName;
	}
	
	public String getSshKey()
	{
		return this.SshKey;
	}
	
	public String getMqInstanceTypeName() {
		return newMqInstanceType;
	}	
	
	public void setSshKey(String sshKey) {
		SshKey = sshKey;
	}

	public synchronized void setEC2Wrapper(EC2Wrapper ec2Wrapper) {
		this.ec2Wrapper = ec2Wrapper;
	}

	public void setNewMqInstanceType(String newMqInstanceType) {
		this.newMqInstanceType = newMqInstanceType;
	}

	public String getAwsEndpoint() {
		return awsEndpoint;
	}

	public void setAwsEndpoint(String awsEndpoint) {
		this.awsEndpoint = awsEndpoint;
	}

	public String getAwsConfigFile() {
		return awsConfigFile;
	}

	public void setAwsConfigFile(String awsConfigFile) {
		this.awsConfigFile = awsConfigFile;
	}
	
	//----------------------------------------------------------------------
	
	/**
	 * Specifies the path to the AWS EC2 config file.
	 * @param filePath The path to the file that contains Amazon EC2 credentials
	 * @return The current instance of <b>EC2CloudPlatformConfiguration</b> to continue configuration.
	 */
	public EC2CloudPlatformConfiguration withAwsConfigFile(String filePath) 
	{
		setAwsConfigFile(filePath);
		return this;
	}
	
	/**
	 * Specifies the address of the Amazon EC2 endpoint.
	 * @param awsEndpoint The address of the Amazon EC2 endpoint.
	 * @return The current instance of <b>EC2CloudPlatformConfiguration</b> to continue configuration.
	 */
	public EC2CloudPlatformConfiguration withAwsEndpoint(String awsEndpoint) 
	{
		setAwsEndpoint(awsEndpoint);
		return this;
	}
	
	/**
	 * Specifies the name of the image that will be used to start new instances in the EC2 Cloud.
	 * @param imageName The existing name of the image that new virtual hosts will be spawned from.
	 * @return The current instance of <b>EC2CloudPlatformConfiguration</b> to continue configuration.
	 */
	public EC2CloudPlatformConfiguration withInstanceImage(String imageName)
	{
		setImageName(imageName);
		return this;
	}
	
	/**
	 * Specifies the type name of the new virtual hosts that should be started.
	 * @param instanceTypeName The type name (e.g., "m1.micro") of the new virtual hosts that should be used.
	 * @return The current instance of <b>EC2CloudPlatformConfiguration</b> to continue configuration.
	 */
	public EC2CloudPlatformConfiguration withInstanceType(String instanceTypeName)
	{
		setInstanceType(instanceTypeName);
		return this;
	}
	
	/**
	 * Specifies the SSH key name that may be used to access the instance through SSH connection.
	 * @param sshKeyName The name of existing SSH key that will be attached to the new virtual host to allow remote access. 
	 * @return The current instance of <b>EC2CloudPlatformConfiguration</b> to continue configuration.
	 */
	public EC2CloudPlatformConfiguration withSshKey(String sshKeyName)
	{
		setSshKey(sshKeyName);
		return this;
	}
	
	/**
	 * Specifies the image name that should be used to start new Message Queue host.
	 * @param mqImageName The existing name of the image that can be used in case Message Queue host has to be started.
	 * @return The current instance of <b>EC2CloudPlatformConfiguration</b> to continue configuration.
	 */
	public EC2CloudPlatformConfiguration withMQImage(String mqImageName)
	{
		setMqImageName(mqImageName);
		return this;
	}
	
	/**
	 * Specifies the instance type of the Message Queue host.
	 * @param mqInstanceTypeName The type name (e.g., "m1.small") of the message queue host that should be used.
	 * @return The current instance of <b>EC2CloudPlatformConfiguration</b> to continue configuration.
	 */
	public EC2CloudPlatformConfiguration withMQInstanceType(String mqInstanceTypeName)
	{
		setNewMqInstanceType(mqInstanceTypeName);
		return this;
	}
	
	//----------------------------------------------------------------------

	@Override
	public IVirtualHost getVirtualHost(IdManager idManager) 
	{
		boolean startPerformanceMonitoring = JCloudScaleConfiguration.getConfiguration()
				.common().monitoring().isEnabled();
		
		return new EC2Instance(this, idManager, startPerformanceMonitoring);
	}

	//it's public only for tests. change test's package?
	public synchronized EC2Wrapper getEC2Wrapper()
	{
		if(this.ec2Wrapper == null)
			try {
				this.ec2Wrapper = new EC2Wrapper(this);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		return this.ec2Wrapper;
	}

	@Override
	protected Closeable startMessageQueueServer(MessageQueueConfiguration communicationConfiguration) throws Exception 
	{
		final String ec2Id = getEC2Wrapper().startNewMessageQeueHost();
		
		if(ec2Id == null)
			throw new Exception("Failed to start MQ Server in EC2.");
		
		return new Closeable() 
		{
			@Override
			public void close() throws IOException 
			{
				getEC2Wrapper().shutdownHost(ec2Id);
			}
		};
	}

	@Override
	public VirtualHostPool getVirtualHostPool() 
	{
		return new VirtualHostPool(this, getMessageQueueConfiguration());
	}
	
	public String generateHostUserData() 
	{
	    MessageQueueConfiguration mqConfig = getMessageQueueConfiguration();
	    
	    String mqServerLocationData = mqConfig.getServerAddress()+":"+mqConfig.getServerPort();
	    
	    String mqFileOnServer = mqConfig.getMessageQueueConnectionFilePath();
	    String mqFolderOnServer = new File(mqFileOnServer).getParent().replace('\\', '/');//have to replace here to not have windows-slashes.
	    
	    return String.format(mqAddressDistributionUserScript, 
							mqFolderOnServer,
							mqServerLocationData,
							mqFileOnServer);
	}
}
