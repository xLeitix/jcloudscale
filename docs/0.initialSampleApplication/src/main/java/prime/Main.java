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
package prime;

import prime.scheduler.ThreadPoolScheduler;
import prime.scheduler.splitter.LinearSplitter;

public class Main {

	/**
	 * Entrance point of the application.
	 */
	public static void main(String[] args) 
	{
		System.out.println("Starting...");
		
		long start = System.nanoTime();
		
		// input parameters.
		final int threadCount = 4;
		final int from = 1;
		final int to = Integer.MAX_VALUE/100;
		
		System.out.println(String.format("Searching prime numbers between %s and %s in %s threads.", from, to, threadCount));
		
		ThreadPoolScheduler scheduler = new ThreadPoolScheduler(threadCount, new LinearSplitter());
		System.out.println(String.format("Execution finished. %s prime numbers found.", scheduler.search(from, to)));
		
		long elapsed = (System.nanoTime() - start)/1000000;
		
		long min = elapsed /(60*1000);
		long sec = (elapsed % (60*1000))/1000;
		long msec = elapsed % 1000;
		System.out.println(String.format("Elapsed: %02d:%02d.%03d", min, sec, msec));
	}

}
