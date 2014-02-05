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
package at.ac.tuwien.infosys.jcloudscale.sample.sentiment;

import at.ac.tuwien.infosys.jcloudscale.messaging.objects.monitoring.Event;

public class ClassificationDurationEvent extends Event {
	
	private static final long serialVersionUID = 1L;
	
	private String tweet;
	private long duration;
	private String hostId;
	
	public ClassificationDurationEvent() {} 
	
	public ClassificationDurationEvent(String tweet, long duration, String hostId) {
		this.tweet = tweet;
		this.duration = duration;
		this.hostId = hostId;
	}
	
	public String getTweet() {
		return tweet;
	}
	public void setTweet(String tweet) {
		this.tweet = tweet;
	}
	public long getDuration() {
		return duration;
	}
	public void setDuration(long duration) {
		this.duration = duration;
	}
	public String getHostId() {
		return hostId;
	}
	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

}
