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
package prime.scheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import prime.scheduler.splitter.ISplitter;
import prime.searcher.ISearcher;
import prime.searcher.Range;
import prime.searcher.SearcherFactory;

/**
 * @author RST
 * Schedules prime search on the specified amount of threads and run them in the ThreadPool.
 */
public class ThreadPoolScheduler 
{
	/**
	 * The thread pool to run tasks in.
	 */
	private ExecutorService threadPool = null;
	
	/**
	 * Amount of threads to run 
	 */
	private int threadCounter;
	
	/**
	 * The variable to store results from each of the threads.
	 */
	private AtomicLong result = new AtomicLong(0);
	
	/**
	 * The synchronization primitive to wait all threads before returning result.
	 */
	private CountDownLatch countDown;
	
	/**
	 * The splitter that will split initial  search region on the smaller regions for separate threads. 
	 */
	private ISplitter splitter;
	
	/**
	 * Creates the new instance of Thread Pool Scheduler with the specified number of threads to run and the splitter to split the provided task
	 * @param threadCounter The amount of threads to schedule execution on.
	 * @param splitter The splitter that should be used to split task on the smaller parts. 
	 */
	public ThreadPoolScheduler(int threadCounter, ISplitter splitter)
	{
		this.splitter = splitter;
		this.threadCounter = threadCounter;
	}
	
	/**
	 * Runs blocking search on the specified interval with configured amount of threads and splitter.
	 * @param from The beginning of the search interval
	 * @param to The end of the search interval
	 * @return The amount of prime numbers found on the interval
	 */
	public long search(int from, int to)
	{
		// initialization
		result.set(0);
		countDown = new CountDownLatch(threadCounter);
		
		threadPool = Executors.newCachedThreadPool();
		
		// starting workers
			for(Range range : splitter.split(new Range(from, to), threadCounter))
				threadPool.execute(new Search(range));
			
			//waiting for result
			try 
			{
				countDown.await();
			} 
			catch (InterruptedException e){}
		
		threadPool.shutdown();
		
		return result.get();
	}
	
	/**
	 * Adds found amount to the result counter and in case this is the last thread running, informs main thread that result is ready.
	 * @param count The amount of found prime numbers.
	 */
	private void reportResult(long count)
	{
		result.addAndGet(count);
		countDown.countDown();
	}
	
	/**
	 * @author RST
	 * Separate runnable search task with the specified range.
	 */
	private class Search implements Runnable
	{
		Range range;
		
		public Search(Range range)
		{
			this.range = range;
		}

		@Override
		public void run() 
		{
			ISearcher searcher = SearcherFactory.createSearcher(range);
			
			searcher.run();
			reportResult(searcher.getResult());
		}
	}
}
