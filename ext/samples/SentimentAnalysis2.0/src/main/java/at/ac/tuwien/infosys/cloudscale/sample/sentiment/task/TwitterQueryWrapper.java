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

import java.util.ArrayList;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

public class TwitterQueryWrapper 
{
	private static final TwitterFactory twitterFactory = new TwitterFactory();
	private static final int retryAttempts = 5;
	private static final int sleepInterval = 1000;
	
	private Twitter twitter;
	//----------------------------------------------
	
	public TwitterQueryWrapper()
	{
		twitter = twitterFactory.getInstance();
	}
	
	//----------------------------------------------
	
	public List<Status> query(String queryString) throws Exception 
	{
		List<Status> result = new ArrayList<Status>();
		
		twitter = twitterFactory.getInstance();
	    Query query = new Query(queryString);
	    
	    QueryResult queryResult = null;
	    
	    queryResult = searchWithRetry(query, retryAttempts);
	    	
	    List<Status> tweets = queryResult.getTweets();
		for(Status tweet : tweets)
			result.add(tweet);
	    
	    return result;
	}

	private QueryResult searchWithRetry(Query query, int retryCount) throws Exception 
	{
    	while(true)
		    try
		    {
		    	return twitter.search(query);
		    }
		    catch(Exception ex)
		    {
		    	if(retryCount-- == 0)
		    		throw ex;

		    	Thread.sleep(sleepInterval);
		    }
	}
}
