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

import java.util.UUID;

/**
 * Declares the interface of the Host Pool that allows to manage running virutal host instances
 * and objects deployed on them.
 */
public interface IHostPool
{
    /**
     * Gets the collection of the hosts known to the system (running or starting up).
     * @return The iterable collection of the hosts known to the system.
     */
    Iterable<IHost> getHosts();

    /**
     * Gets the amount of the hosts known to the system (running or starting up).
     * @return The positive integer value that represents the amount of hosts known to the system.
     */
    int getHostsCount();

    /**
     * Gets the cloud object with the specified id.
     * @param objectId The unique id of the cloud object to find.
     * @return The <b>ClientCloudObject</b> with the specified id or <b>null</b>
     * if such object does not exist.
     */
    ClientCloudObject getCloudObjectById(UUID objectId);

    /**
     * Gets the cloud host with the specified id.
     * @param hostId The unique id of the cloud host to find.
     * @return The <b>IHost</b> instance with specified id or <b>null</b>
     * if such host does not exist.
     */
    IHost getHostById(UUID hostId);

    /**
     * Starts a new host and waits for it to start working.
     * 
     * @return The newly created host.
     */
    IHost startNewHost();
    
    /**
     * Starts a new host of a given size and waits for it to start working.
     * 
     * @param size The textual representation of the size of the host to start (e.g., "m1.tiny").
     * This is going to be backend-specific.
     * @return The newly created host.
     */
    IHost startNewHost(String size);    

    /**
     * Starts a new host and returns immediately.
     * 
     * @return The newly created host.
     */
    IHost startNewHostAsync();
    
    /**
     * Starts a new host and returns immediately.
     * 
     * @param size The textual representation of the size of the host to start (e.g., "m1.tiny").
     * This is going to be backend-specific. 
     * @return The newly created host.
     */
    IHost startNewHostAsync(String size);

    /**
     * Starts a new host and returns immediately.
     * When the host will be started, callback will be executed.
     * @param afterHostStarted The callback that allows to specify post-startup action on the new host.
     * 
     * @return The newly created host.
     */
    IHost startNewHostAsync(IHostStartedCallback afterHostStarted);

    /**
     * Starts a new host and returns immediately.
     * When the host will be started, callback will be executed.
     * @param afterHostStarted The callback that allows to specify post-startup action on the new host.
     * @param size The textual representation of the size of the host to start (e.g., "m1.tiny").
     * This is going to be backend-specific.  
     * @return The newly created host.
     */
    IHost startNewHostAsync(IHostStartedCallback afterHostStarted, String size);

    /**
     * Shuts down the specified host asynchronously.
     * @param host
     */
    void shutdownHostAsync(IHost host);

    /**
     *  The callback interface that allows to declare any custom action
     *  to be executed after the cloud host is completely started.
     */
    public interface IHostStartedCallback
    {
        /**
         * Executes post-build actions specified by user.
         * @param host The host that this callback refers to.
         */
        void startupFinished(IHost host);
    }
}
