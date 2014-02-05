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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Provides all platform-defined information about the Cloud Object.
 */
public class ClientCloudObject {
	
	private final Object lock = new Object();
	
	private UUID id;
	private CloudObjectState state = CloudObjectState.IDLE;
	private WeakReference<Object> proxy;
	
	private List<String> executingMethods = new ArrayList<String>();
	private Class<?> cloudObjectClass;
	
//	private long creationTime; //do we need it? easy to add...
//	private long lastRequestTime;
	
	/**
	 * Creates the new instance of the ClientCloudObject class with default parameters.
	 */
	public ClientCloudObject() 
	{
	}
	
	/**
	 * Creates the new instance of the ClientCloudObject with specified parameters.
	 * @param id The unique id of the CloudObject
	 * @param objectClass The Class of the managed object.
	 * @param proxy The proxy object of the Cloud Object that allows to perform invocations on Cloud Object
	 */
	public ClientCloudObject(UUID id, Class<?> objectClass, Object proxy, ReferenceQueue<Object> refQueue)
	{
		this.id = id;
		this.cloudObjectClass = objectClass;
		this.proxy = new WeakReference<Object>(proxy, refQueue);
	}
	
	/**
	 * Gets the Unique Id of the Cloud Object
	 */
	public UUID getId() {
		return id;
	}
	
	/**
	 * Sets the Unique Id of the Cloud Object
	 */
	public void setId(UUID id) {
		this.id = id;
	}
	
	/**
	 * Gets the current execution state of the Cloud Object.
	 */
	public CloudObjectState getState() {
		return state;
	}
	
	/**
	 * Gets the proxy object for the Cloud Object that allows to perform invocations on the remote cloud object.
	 * This proxy object can be cast to the type of the Cloud Object (see <b>getCloudObjectClass</b> method).
	 */
	public Object getProxy() {
		return proxy.get();
	}
	
	/**
	 * Sets the proxy object for this Cloud Object
	 */
	void setProxy(Object proxy, ReferenceQueue<Object> refQueue) {
		this.proxy = new WeakReference<Object>(proxy, refQueue);
	}
	
	/**
	 * Adds the specified method name to the collection.
	 * @param method
	 */
	public void addExecutingMethod(String method)
	{
		synchronized (lock) 
		{
			this.executingMethods.add(method);
			this.state = CloudObjectState.EXECUTING;
		}
	}
	
	/**
	 * Removes the specified method from the set of executing methods.
	 * @param method The method to remove from the set of executing methods
	 * @return <b>true</b> if the method was removed successfully. Otherwise, <b>false</b>. 
	 */
	public boolean removeExecutingMethod(String method)
	{
		synchronized (lock) 
		{
			if(this.executingMethods.remove(method))
			{
				this.state = this.executingMethods.size() > 0 ? 
							CloudObjectState.EXECUTING : 
								CloudObjectState.IDLE;
				return true;
			}
			else
				return false;
		}
	}
	
	/**
	 * Gets the collection of the methods that are currently executed on this Cloud Object.
	 * @return
	 */
	public List<String> getExecutingMethods()
	{
		return Collections.unmodifiableList(executingMethods);
	}

	/**
	 * Gets the Class that specifies the type of the Cloud Object.
	 * @return
	 */
	public Class<?> getCloudObjectClass() {
		return cloudObjectClass;
	}

	/**
	 * Sets the Class that specifies the type of the Cloud Object.
	 * @param cloudObjectClass The class that specifies the type of this Cloud Object
	 */
	void setCloudObjectClass(Class<?> cloudObjectClass) {
		this.cloudObjectClass = cloudObjectClass;
	}
	
	/**
	 * Marks this client cloud object as destructed
	 */
	public void setDestructed() {
		this.state = CloudObjectState.DESTRUCTED;
	}
	
	/**
	 * Marks this client cloud object as migrating
	 */
	public void beginMigration() {
		state = CloudObjectState.MIGRATING; 
	}
	
	/**
	 * Marks that migration is done
	 */
	public void endMigration() {
		state = CloudObjectState.IDLE; 
	}
}
