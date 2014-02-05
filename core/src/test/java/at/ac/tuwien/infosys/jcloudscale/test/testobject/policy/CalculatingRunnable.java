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

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import static com.google.common.collect.Maps.*;

/**
 * @author Gregor Schauer
 */
public class CalculatingRunnable implements Runnable, Closeable {
	Map<Long, Double> results = newTreeMap();
	int counter;
	
	@Override
	public void run() {
		for (counter = 1; counter > 0; counter++) {
			Long now = System.currentTimeMillis();
			results.put(now, Math.pow(counter, counter));
		}
	}

	@Override
	public void close() throws IOException {
		counter = Integer.MIN_VALUE;
	}
}
