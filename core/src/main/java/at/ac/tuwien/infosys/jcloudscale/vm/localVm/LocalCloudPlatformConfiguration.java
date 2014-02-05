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
package at.ac.tuwien.infosys.jcloudscale.vm.localVm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.ActiveMQHelper;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.CloudPlatformConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IdManager;
import at.ac.tuwien.infosys.jcloudscale.vm.VirtualHostPool;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LocalCloudPlatformConfiguration extends CloudPlatformConfiguration 
{
	private static final long serialVersionUID = 1L;
	
	private String serverStartupClass = "at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServerRunner";
	private String javaPath = "";//empty means use same as current app.
	private String classpath = "";//empty means use same as current app.
	private String startupDirectory = "";//emplty means use as current app.
	private boolean isServerMode = false;
	private long javaHeapSizeMB = 0;//zero is "use default"
	private List<String> customJVMArgs = new ArrayList<>();
	
	public List<String> getCustomJVMArgs() {
		return customJVMArgs;
	}

	public void addCustomJVMArgs(String arg) {
		customJVMArgs.add(arg);
	}

	public void setJavaPath(String javaPath) {
		this.javaPath = javaPath;
	}

	public void setClasspath(String classpath) {
		this.classpath = classpath;
	}

	public void setStartupDirectory(String startupDirectory) {
		this.startupDirectory = startupDirectory;
	}

	public void setServerMode(boolean isServerMode) {
		this.isServerMode = isServerMode;
	}

	public void setJavaHeapSizeMB(long javaHeapSizeMB) {
		this.javaHeapSizeMB = javaHeapSizeMB;
	}
	
	public void setServerStartupClass(String serverStartupClass) {
		this.serverStartupClass = serverStartupClass;
	}

	String getJavaPath() 
	{
		if(javaPath == null || javaPath.length() == 0)
			javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		
		return javaPath;
	}

	String getClasspath() 
	{
		if(classpath == null || classpath.length() == 0)
			classpath = System.getProperty("java.class.path");
		return classpath.replace(';', File.pathSeparatorChar);//we expect users to specify it ";"-separated, as it allows us to keep it platform-independent.
	}

	String getStartupDirectory() 
	{
		if(startupDirectory == null || startupDirectory.length() == 0)
		        startupDirectory = System.getProperty("java.io.tmpdir");
		return startupDirectory;
	}

	boolean isServerMode() {
		return isServerMode;
	}

	long getJavaHeapSizeMB() {
		return javaHeapSizeMB;
	}
	
	String getServerStartupClass() {
		return serverStartupClass;
	}
	
	//---------------------------------------------------------------------------
	
	/**
	 * Specifies the startup and working directory of the new JVMs that are representing cloud hosts.
	 * @param path The valid path to the existing directory in the local file system.
	 * @return The current instance of <b>LocalCloudPlatformConfiguration</b> to continue configuration.
	 */
	public LocalCloudPlatformConfiguration withStartupDirectory(String path)
	{
		setStartupDirectory(path);
		return this;
	}
	
	/**
	 * Specifies the class path of the new JVM to start with. java-classpath specification rules are used.
	 * Dependencies can be separated by ";" on any platform. Class path specified has to contain all necessary 
	 * dependencies of the application that has to be started. 
	 * @param classpath The ";" separated set of paths that will be provided to java on JVM startup.
	 * @return The current instance of <b>LocalCloudPlatformConfiguration</b> to continue configuration.
	 */
	public LocalCloudPlatformConfiguration withClasspath(String classpath)
	{
		setClasspath(classpath);
		return this;
	}
	
	/**
	 * Specifies the collection of custom JVM arguments that need to be provided on JVM startup.
	 * @param parameters The collection of separate parameters passed to JVM on startup. 
	 * @return The current instance of <b>LocalCloudPlatformConfiguration</b> to continue configuration.
	 */
	public LocalCloudPlatformConfiguration withCustomJVMArgs(String... parameters)
	{
		if(parameters != null)
			for(String param : parameters)
				addCustomJVMArgs(param);
		
		return this;
	}
	
	/**
	 * Specifies the path to the java executable that can be used to start JVM.
	 * @param javaPath The absolute path to the java executable to start new JVM.
	 * @return The current instance of <b>LocalCloudPlatformConfiguration</b> to continue configuration.
	 */
	public LocalCloudPlatformConfiguration withJavaPath(String javaPath)
	{
		setJavaPath(javaPath);
		return this;
	}
	
	/**
	 * Specifies whether the JVM should start in server mode.
	 * @param isServerMode <b>true</b> if server mode should be enabled. Otherwise, <b>false</b>.
	 * @return The current instance of <b>LocalCloudPlatformConfiguration</b> to continue configuration.
	 */
	public LocalCloudPlatformConfiguration withServerMode(boolean isServerMode)
	{
		setServerMode(isServerMode);
		return this;
	}
	
	/**
	 * Specifies the amount of maximal java heap size to allocate for new JVM.
	 * @param javaHeapSizeMB The positive <b>long</b> that specifies the amount of megabytes to limit java heap
	 * size of new JVM. 
	 * @return The current instance of <b>LocalCloudPlatformConfiguration</b> to continue configuration.
	 */
	public LocalCloudPlatformConfiguration withJavaHeapSizeInMB(long javaHeapSizeMB)
	{
		setJavaHeapSizeMB(javaHeapSizeMB);
		return this;
	}
	
	/**
	 * Specifies the entry point of the new JVM.
	 * @param serverStartupClass The full name of the class that contains main method and will be used to start application.
	 * @return The current instance of <b>LocalCloudPlatformConfiguration</b> to continue configuration.
	 */
	public LocalCloudPlatformConfiguration withServerStartupClass(String serverStartupClass)
	{
		setServerStartupClass(serverStartupClass);
		return this;
	}
	
	//---------------------------------------------------------------------------
	
	@Override
	public IVirtualHost getVirtualHost(IdManager idManager) 
	{
		boolean startPerformanceMonitoring = JCloudScaleConfiguration.getConfiguration()
										.common().monitoring().isEnabled();
		
		return new LocalVM(this, idManager, startPerformanceMonitoring);
	}

	@Override
	public VirtualHostPool getVirtualHostPool() {
		return new VirtualHostPool(this, getMessageQueueConfiguration());
	}
	
	@Override
	protected AutoCloseable startMessageQueueServer(MessageQueueConfiguration communicationConfiguration) throws Exception 
	{
		ActiveMQHelper mqServer = new ActiveMQHelper(communicationConfiguration);
		mqServer.start();
		return mqServer;
	}
}
