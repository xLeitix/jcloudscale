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
package at.ac.tuwien.infosys.jcloudscale.cli.demoapp;

import java.util.UUID;

import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObjectId;
import at.ac.tuwien.infosys.jcloudscale.annotations.DestructCloudObject;

@CloudObject//(cloudConfig = "cloudconfig.properties", policy = DemoScalingPolicy.class)
public class TestRemoteObject {
	
	private String state;
	
	@CloudObjectId
	private UUID coId;
	
	public TestRemoteObject() {
		state = "Idle";
	}
	
	public void run(long ms) {
		
		final long theMs = ms;
		
		state = "Running";
		try {
			Thread.sleep(theMs);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		state = "Idle";
		
	}
	
	public String getCurrentState() {
		return state;
	}
	
	@DestructCloudObject
	public void remove(){}
	
	@Override
	public String toString() {
		return super.toString()+" ("+coId.toString()+") -- "+getCurrentState();
	}
	
}
