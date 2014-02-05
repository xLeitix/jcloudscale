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

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

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
public class OpenstackCloudPlatformConfiguration extends CloudPlatformConfiguration 
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * The Openstack user script template that creates a file with MQ server access credentials
	 */
	private static final String mqAddressDistributionUserScript = 
			"#!/bin/sh"+"\n"+	//have to use Linux separators here.
			"mkdir -p %s"+"\n"+	//creating directory, if it is missing.
    		"echo \"%s\" > %s";	//writing file.

	private static final String IDENTITY_PROPERTY = "OS_AUTH_URL";
	private static final String TENANT_NAME_PROPERTY = "OS_TENANT_NAME";
	private static final String LOGIN_PROPERTY = "OS_USERNAME";
	private static final String PASSWORD_PROPERTY = "OS_PASSWORD";
	
	private String imageName = "JCloudScale"+"_v"+JCloudScaleConfiguration.CS_VERSION;// "JCloudScale_v0.1"
	private String imageId = "";//if empty, detect by name. One of them should not be null
	
	private String mqImageName ="ActiveMQ";
	private String mqImageId = "";//if empty, detect by name.
	
	private String newInstanceType = "m1.tiny";
	private String newInstanceTypeId = "";
	
	private String newMqInstanceType = "m1.small";
	private String newMqInstanceTypeId = "";
	
	private String SshKey = "";//if empty -- don't attach anything.
	private String identityPublicURL;
	private String tenantName;
	private String login;
	private String password;
	
	private transient OpenStackWrapper osWrapper = null;
	//----------------------------------------------------------------------
	
	/**
	 * Creates the new instance of <b>OpenstackCloudPlatformConfiguration</b> with the properties specified.
	 * @param identityPublicURL The URL that specifies the authentication server (<b>OS_AUTH_URL</b> openstack property value).
	 * @param tenantName The name of the tenant that can be used by this application (<b>OS_TENANT_NAME</b> openstack property value).
	 * @param login The login of the user that can be used by this application (<b>OS_USERNAME</b> openstack property value).
	 * @param password The password of the user that correspond for the specified username (<b>OS_PASSWORD</b> openstack property value).
	 */
	public OpenstackCloudPlatformConfiguration(String identityPublicURL, String tenantName, String login, String password)
	{
		this.identityPublicURL = identityPublicURL;
		this.tenantName = tenantName;
		this.login = login;
		this.password = password;
	}
	
	/**
	 * Creates the new instance of <b>OpenstackCloudPlatformConfiguration</b> with the properties specified.
	 * @param identityPublicURL The URL that specifies the authentication server (<b>OS_AUTH_URL</b> openstack property value).
	 * @param tenantName The name of the tenant that can be used by this application (<b>OS_TENANT_NAME</b> openstack property value).
	 * @param login The login of the user that can be used by this application (<b>OS_USERNAME</b> openstack property value).
	 * @param password The password of the user that correspond for the specified username (<b>OS_PASSWORD</b> openstack property value).
	 * @param imageName The name of the image that should be used to start new virtual hosts.
	 */
	public OpenstackCloudPlatformConfiguration(String identityPublicURL, String tenantName, String imageName, String login, String password)
	{
		this(identityPublicURL, tenantName, login, password);
		this.imageName = imageName;
	}
	
	/**
	 * Creates a new instance of <b>OpenstackCloudPlatformConfiguration</b> with the configuration values stored in the properties.
	 * @param properties The collection of properties that can be used to initialize this instance of <b>OpenstackCloudPlatformConfiguration</b>.
	 */
	public OpenstackCloudPlatformConfiguration(Properties properties)
	{
		this(properties.getProperty(IDENTITY_PROPERTY), 
			 properties.getProperty(TENANT_NAME_PROPERTY), 
			 properties.getProperty(LOGIN_PROPERTY), 
			 properties.getProperty(PASSWORD_PROPERTY));
	}
	
	/**
	 * Creates a new instance of <b>OpenstackCloudPlatformConfiguration</b> with the configuration values stored in the properties file.
	 * @param osPropertiesFileName The file name of the file that contains properties that can be used to initialize this instance of <b>OpenstackCloudPlatformConfiguration</b>. 
	 * @throws IOException In case the file is missing or cannot be read.
	 */
	public OpenstackCloudPlatformConfiguration(String osPropertiesFileName) throws IOException
	{
		this(loadProperties(osPropertiesFileName));
	}
	
	private static Properties loadProperties(String osPropertiesFileName) throws IOException 
	{
		Properties properties = new Properties();
		try(FileReader reader = new FileReader(osPropertiesFileName))
		{
			properties.load(reader);
		}
		return properties;
	}
	
	//---------------------------------------------------------------------------------------------

	String getIdentityPublicURL() {
		return identityPublicURL;
	}

	String getLogin() {
		return login;
	}

	String getPassword() {
		return password;
	}
	
	String getTenantName()
	{
		return this.tenantName;
	}
	
	synchronized String getImageId()
	{
		if(this.imageId == null || this.imageId.length() == 0)
			this.imageId = getOpenstackWrapper().lookupImageId(imageName);
		
		return imageId;
	}
	
	synchronized String getInstanceTypeId()
	{
		if(this.newInstanceTypeId == null || this.newInstanceTypeId.length() == 0)
			this.newInstanceTypeId = getOpenstackWrapper().lookupFlavorId(this.newInstanceType);
		
		return this.newInstanceTypeId; 
	}
	
	synchronized String getMqImageId() 
	{
		if(this.mqImageId == null || this.mqImageId.length() == 0)
			this.mqImageId = getOpenstackWrapper().lookupImageId(mqImageName);
		
		return mqImageId;
	}

	synchronized String getMqInstanceTypeId() 
	{
		if(this.newMqInstanceTypeId == null || this.newMqInstanceTypeId.length() == 0)
			this.newMqInstanceTypeId = getOpenstackWrapper().lookupFlavorId(this.newMqInstanceType);
		
		return this.newMqInstanceTypeId; 
	}
	
	String getNewInstanceTypeName()
	{
		return this.newInstanceType;
	}
	
	String getSshKey()
	{
		return this.SshKey;
	}
	
	public String getImageName() {
		return imageName;
	}

	public String getNewInstanceType() {
		return newInstanceType;
	}
	public String getMqInstanceTypeName() {
		return newMqInstanceType;
	}	
	
	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public synchronized void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public void setNewInstanceType(String newInstanceType) {
		this.newInstanceType = newInstanceType;
	}

	public synchronized void setNewInstanceTypeId(String newInstanceTypeId) {
		this.newInstanceTypeId = newInstanceTypeId;
	}

	public void setSshKey(String sshKey) {
		SshKey = sshKey;
	}

	public void setIdentityPublicURL(String identityPublicURL) {
		this.identityPublicURL = identityPublicURL;
	}

	public void setTenantName(String tenantName) {
		this.tenantName = tenantName;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public synchronized void setOsWrapper(OpenStackWrapper osWrapper) {
		this.osWrapper = osWrapper;
	}

	public synchronized void setMqImageName(String mqImageName) {
		this.mqImageName = mqImageName;
	}

	public synchronized void setMqImageId(String mqImageId) {
		this.mqImageId = mqImageId;
	}

	public void setNewMqInstanceType(String newMqInstanceType) {
		this.newMqInstanceType = newMqInstanceType;
	}

	public synchronized void setNewMqInstanceTypeId(String newMqInstanceTypeId) {
		this.newMqInstanceTypeId = newMqInstanceTypeId;
	}
	
	//----------------------------------------------------------------------
	
	/**
	 * Specifies the name of the image that will be used to start new instances in the Openstack Cloud.
	 * @param imageName The existing name of the image that new virtual hosts will be spawned from.
	 * @return The current instance of <b>OpenstackCloudPlatformConfiguration</b> to continue configuration.
	 */
	public OpenstackCloudPlatformConfiguration withInstanceImage(String imageName)
	{
		setImageName(imageName);
		return this;
	}
	
	/**
	 * Specifies the type name of the new virtual hosts that should be started.
	 * @param instanceTypeName The type name (e.g., "m1.tiny") of the new virtual hosts that should be used.
	 * @return The current instance of <b>OpenstackCloudPlatformConfiguration</b> to continue configuration.
	 */
	public OpenstackCloudPlatformConfiguration withInstanceType(String instanceTypeName)
	{
		setNewInstanceType(instanceTypeName);
		return this;
	}
	
	/**
	 * Specifies the SSH key name that may be used to access the instance through SSH connection.
	 * @param sshKeyName The name of existing SSH key that will be attached to the new virtual host to allow remote access. 
	 * @return The current instance of <b>OpenstackCloudPlatformConfiguration</b> to continue configuration.
	 */
	public OpenstackCloudPlatformConfiguration withSshKey(String sshKeyName)
	{
		setSshKey(sshKeyName);
		return this;
	}
	
	/**
	 * Specifies the image name that should be used to start new Message Queue host.
	 * @param mqImageName The existing name of the image that can be used in case Message Queue host has to be started.
	 * @return The current instance of <b>OpenstackCloudPlatformConfiguration</b> to continue configuration.
	 */
	public OpenstackCloudPlatformConfiguration withMQImage(String mqImageName)
	{
		setMqImageName(mqImageName);
		return this;
	}
	
	/**
	 * Specifies the instance type of the Message Queue host.
	 * @param mqInstanceTypeName The type name (e.g., "m1.tiny") of the message queue host that should be used.
	 * @return The current instance of <b>OpenstackCloudPlatformConfiguration</b> to continue configuration.
	 */
	public OpenstackCloudPlatformConfiguration withMQInstanceType(String mqInstanceTypeName)
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
		
		return new OpenStackInstance(this, idManager, startPerformanceMonitoring);
	}

	
	//it's public only for tests. change test's package?
	public synchronized OpenStackWrapper getOpenstackWrapper()
	{
		if(this.osWrapper == null)
			this.osWrapper = new OpenStackWrapper(this);
		
		return this.osWrapper;
	}

	@Override
	protected Closeable startMessageQueueServer(MessageQueueConfiguration communicationConfiguration) throws Exception 
	{
		String serverName = "MQ_"+UUID.randomUUID().toString();
		final String openStackId = getOpenstackWrapper().startNewMessageQeueHost(serverName, communicationConfiguration.getServerAddress());
		
		return new Closeable() 
		{
			@Override
			public void close() throws IOException 
			{
				getOpenstackWrapper().shutdownHostViaInternalId(openStackId);
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
