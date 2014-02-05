/*
   Copyright 2014 Philipp Leitner 

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
package at.ac.tuwien.infosys.jcloudscale.monitoring;

import java.io.Serializable;

public class RAMUsage implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private double maxMemory;
	private double usedMemory;
	private double freeMemory;
	
	public RAMUsage(double max, double used, double free) {
		this.maxMemory = max;
		this.usedMemory = used;
		this.freeMemory = free;
	}
	
	public double getMaxMemory() {
		return maxMemory;
	}
	public double getUsedMemory() {
		return usedMemory;
	}
	public double getFreeMemory() {
		return freeMemory;
	}
	
	@Override
	public String toString() {
		
		return String.format("Used:%f Free:%f Max:%f", usedMemory, freeMemory, maxMemory);
		
	}
	
}
