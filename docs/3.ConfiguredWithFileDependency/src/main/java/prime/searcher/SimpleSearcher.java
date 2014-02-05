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
package prime.searcher;

import at.ac.tuwien.infosys.jcloudscale.annotations.ByValueParameter;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.DestructCloudObject;

/**
 * @author RST
 * Represents a simple and quite slow prime numbers searching algorithm that goes through 
 * the range and tries to factorize each number in the range.
 * Okay, there are some optimizations to run on appropriate speed.
 */
@CloudObject
public class SimpleSearcher implements ISearcher
{
	// Interesting fact: if you switch for int internally, performance will increase more than twice...
	
	private Range range;
	private long result;
	
	/**
	 * Creates the new instance of the <b>SimpleSearcher</b> with the provided range.
	 * @param range The range to run search on.
	 */
	SimpleSearcher(@ByValueParameter Range range)
	{
		if(range.getFrom() <= 0 || range.getTo() <= 0)
			throw new RuntimeException("Range contains negative or zero parameters.");
		
		this.range = range;
		this.result = 0;
	}
	
	/**
	 * Checks if the provided number is prime or not.
	 * @param number The number to check.
	 * @return <b>true</b> if number is prime. Otherwise -- false.
	 */
	protected boolean isPrime(long number)
	{
		if(number % 2 == 0)
			return false;
		
		long max = (long)Math.floor(Math.sqrt(number));
		
		for(long i=3; i <= max; i+=2)
			if(number % i == 0)
				return false;
		
		return true;
	}
	
	@Override
	public void run() 
	{
		long start = Math.max(1, range.getFrom());
		long finish = Math.max(start, range.getTo());
		
		if (start > 2)
		{
			if(start%2 == 0)
				start = start+1;
		}
		else
		{
			if(start == 1)
				result = finish >= 2 ? 2 : 1;
			else
				if(start == 2)
					result = 1;
			
			start = 3;
		}
		
		for(long i = start; i <= finish; i += 2)
			if(isPrime(i))
				result++;
		
		System.out.println("#### In "+range+" found "+ result +" prime numbers.####");
	}

	@DestructCloudObject
	@Override
	public long getResult() 
	{
		return result;
	}
}
