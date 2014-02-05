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

import java.util.UUID;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.monitoring.IEventSink;
import at.ac.tuwien.infosys.jcloudscale.sample.sentiment.ClassificationDurationEvent;
import at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServerRunner;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;
import twitter4j.Status;

public class TwitterSentimentAnalyzer
{
	
	public static SentimentResult analyzeTweets(String query, TwitterQueryWrapper twitterWrapper,
			Classifier classifier, IEventSink events)
	{
		SentimentResult result = new SentimentResult(query);
		
		try
		{
			for(Status tweet : twitterWrapper.query(query)) {
				long begin = System.currentTimeMillis();
				result.acceptResult(classifier.classify(tweet.getText()));
				long end = System.currentTimeMillis();
				// trigger an event that contains the processing time for this tweet
				// TODO: there should be a nicer way to get this
				UUID serverId = JCloudScaleServerRunner.getInstance().getId(); 
				events.trigger(
					new ClassificationDurationEvent(tweet.getText(), (end - begin), serverId.toString())
				);
			}
			
			result.setSuccess(true);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			
			result.setSuccess(false);
			result.setException(ex.toString());
		}
		
		return result;
	}
	
}
