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
package at.ac.tuwien.infosys.jcloudscale.cli;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;

import asg.cliche.Command;
import at.ac.tuwien.infosys.jcloudscale.cli.demoapp.TestRemoteObject;
import at.ac.tuwien.infosys.jcloudscale.management.CloudConfigException;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.messaging.ActiveMQHelper;

public class CLIDemoClientBackend {
	
	private int cntr = 0;
	private Hashtable<Integer,TestRemoteObject> objects = new Hashtable<Integer,TestRemoteObject>();
	
	private TestRemoteObject current;
	private long runParam;
	
	private ActiveMQHelper activemq;
	
	
	@Command(name="initialize-jcloudscale", abbrev="init-cs")
	public void initializeJCloudScale() throws InstantiationException,
		IllegalAccessException, CloudConfigException 
	{
		
		System.out.println("-- Initializing JCloudScale --");
		TestRemoteObject.class.newInstance();
		CloudManager.getInstance();
		
		System.out.println("-- Done --");
	}
	
	@Command(name="list-test-objects", abbrev="ls")
	public void listCurrentObjects() {
		
		System.out.println("-- Current Objects --");
		for(Object obj : objects.keySet()) {
			System.out.println(obj.toString()+": "+objects.get(obj));
		}
		System.out.println("-- Done --");
		
	}
	
	@Command(name="create-objects", abbrev="new")
	public void newObjects(int nrOfNewObjects) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		
		for(int i=0; i<nrOfNewObjects; i++) {
			int id = cntr++;
			objects.put(id,new TestRemoteObject());
		}
		
		System.out.println("-- Done --");
		
	}
	
	@Command(name="destroy-object", abbrev="del")
	public void destroyObject(int id) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		if(!objects.containsKey(id)) {
			System.out.println("-- Unknown object --");
			return;
		}
		
		objects.get(id).remove();
		objects.remove(id);
		System.out.println("-- Done --");
		
	}
	
	@Command(name="invoke-object", abbrev="run")
	public void invokeObject(int id, long param) throws NoSuchMethodException, SecurityException,
		IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		if(!objects.containsKey(id)) {
			System.out.println("-- Unknown object --");
			return;
		}
		
		current = objects.get(id);
		runParam = param;
		
		new Thread() {
			
			@Override
			public void run() {
				current.run(runParam);
			}
			
		}.start();
		
		System.out.println("-- Done --");
		
	}
	
	@Command(name="invoke-object", abbrev="run")
	public void invokeObject(int id) throws NoSuchMethodException, SecurityException, IllegalAccessException,
		IllegalArgumentException, InvocationTargetException {
		
		invokeObject(id);
		
	}
	
	@Command(name="check-object-state", abbrev="check")
	public void checkState(int id) {
		
		if(!objects.containsKey(id)) {
			System.out.println("-- Unknown object --");
			return;
		}
		
		System.out.println(id+": "+objects.get(id).toString());
		System.out.println("-- Done --");
		
	}
	
	@Command(name="start-activemq", abbrev="start-mq")
	public void launchActiveMQ() throws Exception {
		
		activemq = new ActiveMQHelper();
		
		if(activemq.isRunning()) {
			System.out.println("-- ActiveMQ seems to be already running --");
		} else {
			activemq.start();
			System.out.println("-- Started ActiveMQ --");
		}
		
	}
	
	@Command(name="stop-activemq", abbrev="stop-mq")
	public void stopActiveMQ() throws Exception {
		
		if(!activemq.isRunning()) {
			System.out.println("-- ActiveMQ does not seem to be running --");
		} else {
			activemq.close();
			System.out.println("-- Stopped ActiveMQ --");
		}
		
	}
	
}
