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
package at.ac.tuwien.infosys.jcloudscale.server;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class ServerLogFormatter extends SimpleFormatter {
	
	private String server;
	
	public ServerLogFormatter() {
		try {
			this.server = PlatformSpecificUtil.findBestIP();
		} catch (Exception e) {
			e.printStackTrace();
			this.server = "Unknown";
		}
	}
	
	@Override
	public String format(LogRecord record) 
	{
		String orig = super.format(record);
		return orig.replaceFirst(": ", ": ("+server+"/"+
				// XXX AbstractJCloudScaleServerRunner
				// JCloudScaleServerRunner.getInstance().getId()
				AbstractJCloudScaleServerRunner.getInstance().getId()
				+") ");
	}
}
