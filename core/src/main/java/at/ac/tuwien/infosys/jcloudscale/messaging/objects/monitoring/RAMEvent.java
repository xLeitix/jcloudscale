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

public class RAMEvent extends StateEvent {

	private static final long serialVersionUID = 87112;
	private long maxMemory;
	private long usedMemory;
	private long freeMemory;
	
	private long commitedMemory;
	private long freeSwapMemory;
	private long maxSwapMemory;
	private long usedSwapMemory;
	
	public long getMaxMemory() {
		return maxMemory;
	}
	public void setMaxMemory(long maxMemory) {
		this.maxMemory = maxMemory;
	}
	public long getUsedMemory() {
		return usedMemory;
	}
	public void setUsedMemory(long usedMemory) {
		this.usedMemory = usedMemory;
	}
	public long getFreeMemory() {
		return freeMemory;
	}
	public void setFreeMemory(long freeMemory) {
		this.freeMemory = freeMemory;
	}
	
	public long getCommitedMemory() {
		return commitedMemory;
	}
	public void setCommitedMemory(long commitedMemory) {
		this.commitedMemory = commitedMemory;
	}
	public long getMaxSwapMemory() {
		return maxSwapMemory;
	}
	public void setMaxSwapMemory(long maxSwapMemory) {
		this.maxSwapMemory = maxSwapMemory;
	}
	public long getFreeSwapMemory() {
		return freeSwapMemory;
	}
	public void setFreeSwapMemory(long freeSwapMemory) {
		this.freeSwapMemory = freeSwapMemory;
	}
	public long getUsedSwapMemory() {
		return usedSwapMemory;
	}
	public void setUsedSwapMemory(long usedSwapMemory) {
		this.usedSwapMemory = usedSwapMemory;
	}
	
	@Override
	public String toString() {
		return String.format(super.toString()+" Free: %d Used: %d Max: %d",
			freeMemory, usedMemory, maxMemory);
	}
	
}
