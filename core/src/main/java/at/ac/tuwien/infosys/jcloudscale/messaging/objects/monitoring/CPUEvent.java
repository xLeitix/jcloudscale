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
package at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring;

public class CPUEvent extends StateEvent {

	private static final long serialVersionUID = 87108;

	private String arch;
	private String vendor;
	private String model;
	private String osVersion;
	private int nrCPUs;
	private int maxMHz;

	private double systemLoadAverage;
	// private double processCpuLoad;
	// private long processCpuTime;
	// private double systemCpuLoad;

	private double combinedCpuLoad;
	private double idleCpuLoad;
	private double niceCpuLoad;
	private double systemCpuLoad;
	private double userCpuLoad;

	public String getArch() {
		return arch;
	}

	public void setArch(String arch) {
		this.arch = arch;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	public int getNrCPUs() {
		return nrCPUs;
	}

	public void setNrCPUs(int nrCPUs) {
		this.nrCPUs = nrCPUs;
	}

	public int getMaxMHz() {
		return maxMHz;
	}

	public void setMaxMHz(int maxMHz) {
		this.maxMHz = maxMHz;
	}

	public double getSystemLoadAverage() {
		return systemLoadAverage;
	}

	public void setSystemLoadAverage(double systemLoadAverage) {
		this.systemLoadAverage = systemLoadAverage;
	}

	public double getCombinedCpuLoad() {
		return combinedCpuLoad;
	}

	public void setCombinedCpuLoad(double combinedCpuLoad) {
		this.combinedCpuLoad = combinedCpuLoad;
	}

	public double getIdleCpuLoad() {
		return idleCpuLoad;
	}

	public void setIdleCpuLoad(double idleCpuLoad) {
		this.idleCpuLoad = idleCpuLoad;
	}

	public double getNiceCpuLoad() {
		return niceCpuLoad;
	}

	public void setNiceCpuLoad(double niceCpuLoad) {
		this.niceCpuLoad = niceCpuLoad;
	}

	public double getSystemCpuLoad() {
		return systemCpuLoad;
	}

	public void setSystemCpuLoad(double systemCpuLoad) {
		this.systemCpuLoad = systemCpuLoad;
	}

	public double getUserCpuLoad() {
		return userCpuLoad;
	}

	public void setUserCpuLoad(double userCpuLoad) {
		this.userCpuLoad = userCpuLoad;
	}

	@Override
	public String toString() {
		return String.format(super.toString() + " %f (%d, %s)", systemLoadAverage, nrCPUs, arch);
	}

}
