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
package at.ac.tuwien.infosys.jcloudscale.sample.sentiment.task;

import java.util.List;
import java.util.Random;

import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.DestructCloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.EventSink;
import at.ac.tuwien.infosys.jcloudscale.annotations.FileDependency;
import at.ac.tuwien.infosys.jcloudscale.annotations.FileDependency.FileAccess;
import at.ac.tuwien.infosys.jcloudscale.monitoring.IEventSink;
import at.ac.tuwien.infosys.jcloudscale.sample.sentiment.ResultsDatabase;
import at.ac.tuwien.infosys.jcloudscale.sample.sentiment.WEKAFileProvider;

@CloudObject
@FileDependency(dependencyProvider = WEKAFileProvider.class, accessType = FileAccess.ReadOnly)
public class Task {

	private String theTaskWord;
	
	// this is an event sink that we can use to trigger custom events
	// for event-based monitoring - it will be injected by JCloudScale
	@EventSink
	private IEventSink events;
	
	public static TaskThread randomTask(List<String> tasks, ResultsDatabase resultsCallback) {
		
		Random r = new Random();
		Task task = new Task(
				tasks.get(r.nextInt(tasks.size()))
			);
		return task.new TaskThread(resultsCallback);
	}
	
	public Task(String theTask) {
		theTaskWord = theTask;
	}
	
	@DestructCloudObject
	public void analyze(ResultsDatabase resultCallback) {
		
		System.out.println("Starting to analyse "+theTaskWord);
		
		// create our machine learning classifier
		Classifier classifier = ClassifierFactory.createClassifier();
		
		System.out.println("Created classifier for "+theTaskWord);
		
		// do the analysis
		SentimentResult result = 
				TwitterSentimentAnalyzer.analyzeTweets(theTaskWord, new TwitterQueryWrapper(), classifier, events);
		
		System.out.println("Received result for "+theTaskWord);
		
		resultCallback.storeResult(theTaskWord, result);
		
	}
	
	public class TaskThread extends Thread {
		
		private ResultsDatabase results;
		
		public TaskThread(ResultsDatabase results) {
			this.results = results;
		}
		
		@Override
		public void run() {
			// note that results here has the semantics of an 'out' parameter
			// - it is simply a callback to deliver the actual result to
			analyze(results);
		}
		
	}
	
	
}
