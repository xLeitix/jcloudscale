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
package at.ac.tuwien.infosys.jcloudscale.vm;

import java.util.Date;
import java.util.UUID;

import at.ac.tuwien.infosys.jcloudscale.monitoring.CPUUsage;
import at.ac.tuwien.infosys.jcloudscale.monitoring.RAMUsage;

/**
 * Specifies the cloud host proxy and the set of user-available operations on the cloud host. 
 * @author RST
 */
public interface IHost 
{
	/**
	 * Gets the unique identifier of the host.
	 * @return The <b>UUID</b> that uniquely identifies this host.
	 */
	UUID getId();
	
	/**
	 * Gets the IP address of the host.
	 * @return The <b>String</b> that specifies the IP address of the host.
	 */
	String getIpAddress();
	
	/**
	 * Gets the startup time of the cloud host.
	 * @return The <b>Date</b> that identifies the startup time of the cloud host.
	 */
	Date getStartupTime();
	
	/**
	 * Allows to determine if the cloud host is online or not.
	 * @return<b>true</b> if cloud host is started up completely and is ready to receive requests. 
	 * If the host did not startup yet or was already shut down, -- <b>false</b>.
	 */
	boolean isOnline();
	
	/**
	 * Gets the last request time for this host.
	 * @return The <b>Date</b> that identifies the time when there was last interaction with this host.
	 */
	Date getLastRequestTime();
	
	/**
	 * Gets the collection of the cloud objects deployed on this machine.
	 * @return The collection of the cloud objects that are deployed on this cloud host.
	 */
	Iterable<ClientCloudObject> getCloudObjects();
	
	/**
	 * Gets the cloud objects with the specified id deployed to this host.
	 * @param objectId The id of the cloud object.
	 * @return The <b>ClientCloudObject</b> instance or <b>null</b> if object with the specified id was not found.
	 */
	ClientCloudObject getCloudObjectById(UUID objectId);
	
	/**
	 * Gets the amount of the cloud objects deployed on this cloud host.
	 * @return The positive <b>int</b> or 0 that determines the amount of cloud objects 
	 * currently deployed on this cloud host. 
	 */
	int getCloudObjectsCount();
	
	/**
	 * If monitoring is enabled, gets the last measurement of the CPU load on this machine. Otherwise returns null.   
	 * @return A complex value returning some basic load metrics of the CPU. Values are averages of the last 1-minute
	 * interval
	 */
	CPUUsage getCurrentCPULoad();
	
	/**
	 * If monitoring is enabled, gets the last measurement of the memory usage on this machine. Otherwise returns null.   
	 * @return A complex value containing the max, free and used memory of the host JVM process at measurement time.
	 */
	RAMUsage getCurrentRAMUsage();
	
//	double getCustomMetrics(String metricsName);
}
