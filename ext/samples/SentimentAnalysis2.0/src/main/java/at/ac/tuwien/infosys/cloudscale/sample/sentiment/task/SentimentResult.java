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

import java.io.Serializable;

public class SentimentResult implements Serializable
{
	private static final long serialVersionUID = -4986303925105532742L;

	private String query;
	private boolean isSuccess = false;
	private String exception = "";
	
	private int positive = 0;
	private int negative = 0;
	private int neutral = 0;

	//-------------------------------------------
	
	public SentimentResult()
	{
	}
	
	public SentimentResult(String query)
	{
		this.query = query;
	}
	
	//------------------ PROPERTIES -------------------------
	
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}
	
	public boolean isSuccess() {
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}
	
	public int getPositive() {
		return positive;
	}

	public int getNegative() {
		return negative;
	}

	public int getNeutral() {
		return neutral;
	}

	public TextSentimentType getOverallResult()
	{
		if(positive > negative && positive > neutral)
			return TextSentimentType.Positive;
		
		if(negative > positive && negative > neutral)
			return TextSentimentType.Negative;
		
		return TextSentimentType.Neutral;
	}
	
	public int getTotalResults()
	{
		return positive + negative + neutral;
	}
	
	public int getTotalPercentage()
	{
		if(getTotalResults() == 0)
			return 0;
		
		int res = 0;
		switch(getOverallResult())
		{
			case Negative:
				res = negative;
				break;
			case Positive:
				res = positive;
				break;
				
			case Neutral:
				res = neutral;
				break;
				
			default:
				//throw RuntimeException("UNEXPECTED ENUM");
		}
		
		return (int)Math.round(((double)res/getTotalResults())*100);
	}

	//---------------------- METHODS ---------------------
	
	public void acceptResult(TextSentimentType result)
	{
		switch(result)
		{
			case Negative:
				negative++;
				break;
				
			case Positive:
				positive++;
				break;
				
			case Neutral:
				neutral++;
				break;
				
			default:
				//throw RuntimeException("UNEXPECTED ENUM");
		}
	}
	
	@Override
	public String toString() 
	{
		if(!isSuccess)
			return String.format("Query=\"%1$s\" evaluation failed with exception %2$s",
						query,
						exception);
		
		if(getTotalResults() == 0)
			return String.format("Query=\"%1$s\" cannot be evaluated as there's no results for it.", query);
		
		return String.format("Query=\"%1$s\" was evaluated as %2$s (%7$s%%) (positive = %3$s, neutral = %4$s, negative = %5$s). Analyzed %6$s tweets.",
										query,
										getOverallResult().toString().toUpperCase(),
										positive,
										neutral,
										negative,
										getTotalResults(),
										getTotalPercentage());
	}
	
}
