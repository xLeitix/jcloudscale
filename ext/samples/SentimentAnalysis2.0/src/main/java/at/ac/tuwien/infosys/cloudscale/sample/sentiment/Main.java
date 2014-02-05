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
package at.ac.tuwien.infosys.jcloudscale.sample.sentiment;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown;
import at.ac.tuwien.infosys.jcloudscale.sample.sentiment.task.Task;

 
public class Main {
	
	public static final String TASK_FILE = "task.txt";
	public static final String SERIALIZED_MODELS = "files";
	public static final String LOAD_DEFINITION = "load.txt";
	
	// we need some structure to collect our results
	private static final ResultsDatabase results = new ResultsDatabase();
	
	@JCloudScaleShutdown
	public static void main(String[] args) throws InterruptedException,
		FileNotFoundException, IOException {
		
		// load tasks from file
		List<String> tasks = readTasks(TASK_FILE);
		List<Integer> delays = readDelays(LOAD_DEFINITION);
		
		System.out.println("Starting JCloudScale sentiment analysis sample");
		
		for(int delay : delays) {
			
			delay *= 1000;
			
			System.out.println("Scheduling new task");
			
			// start a new request
			// (note that we do that in a separate thread, because requests
			//  should come in despite us e.g., waiting for a host to start)
			new RequestRunner(tasks).start();
			
			System.out.println("Waiting for "+delay+" milliseconds");
			
			// wait for a random amount of time
			Thread.sleep(delay);
			
		}
		
		System.out.println("Shutting down Sentiment Analysis example");
		System.out.println("We are done. Printing results now");
		results.print();
		
	}
	
	private static List<String> readTasks(String taskfile) 
	{
		List<String> res = new ArrayList<String>();
		try
		{
			Scanner scanner = new Scanner(new FileReader(taskfile));
			try
			{
				while(scanner.hasNext())
					res.add(scanner.nextLine());
			}
			finally
			{
				scanner.close();
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return res;
	}
	
	private static List<Integer> readDelays(String delaysfile) 
	{
		List<Integer> ints = new ArrayList<Integer>();
		try
		{
			Scanner scanner = new Scanner(new FileReader(delaysfile));
			try
			{
				while(scanner.hasNext())
					ints.add(Integer.parseInt(scanner.nextLine().trim()));
			}
			finally
			{
				scanner.close();
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return ints;
	}
	
	private static class RequestRunner extends Thread {
		
		private List<String> tasks;
		
		public RequestRunner(List<String> tasks) {
			this.tasks = tasks;
		}
		
		@Override
		public void run() {
			Thread thread = Task.randomTask(tasks, results);
			thread.setDaemon(true);
			thread.start();
		}
		
	}
}
