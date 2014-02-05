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

public class CPUUsage implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private double cpuLoad;
	private int processors;
	private String arch;
	
	public CPUUsage(double load, int processors, String arch) {
		this.cpuLoad = load;
		this.processors = processors;
		this.arch = arch;
	}
	
	public double getCpuLoad() {
		return cpuLoad;
	}
	public int getProcessors() {
		return processors;
	}
	public String getArch() {
		return arch;
	}
	
	@Override
	public String toString() {
		
		return String.format("%f on %d processors (%s)", cpuLoad, processors, arch);
		
	}
	
}
