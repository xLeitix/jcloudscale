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
package at.ac.tuwien.infosys.jcloudscale.test.testobject.policy;

import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.DestructCloudObject;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Gregor Schauer
 */
@CloudObject
public class WaitingRunnable implements Runnable, Closeable {
	boolean closed;

	public void start() {
		new Thread() {
			@Override
			public void run() {
				WaitingRunnable.this.run();
			}
		}.start();
	}
	
	@Override
	public void run() {
		try {
			while (!closed) {
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			// Who cares?
		}
	}

	@DestructCloudObject
	@Override
	public void close() throws IOException {
		closed = true;
		Thread.currentThread().interrupt();
	}
}
