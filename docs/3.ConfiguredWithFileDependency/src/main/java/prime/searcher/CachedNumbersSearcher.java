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
package prime.searcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import at.ac.tuwien.infosys.jcloudscale.annotations.ByValueParameter;
import at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject;
import at.ac.tuwien.infosys.jcloudscale.annotations.FileDependency;

@CloudObject
@FileDependency(files = {CachedNumbersSearcher.CACHE_FILE_NAME})
public class CachedNumbersSearcher extends SimpleSearcher
{
	public static final String CACHE_FILE_NAME = "primes.txt";
	
	private List<Long> cachedPrimes = null;
	private long maxCachedPrime = 0L;
	
	CachedNumbersSearcher(@ByValueParameter Range range) 
	{
		super(range);
	}

	private void loadCache()
	{
		try
		{
			File cacheFile = new File(CACHE_FILE_NAME);
			if(!cacheFile.exists())
				throw new FileNotFoundException("File "+CACHE_FILE_NAME+" was not found. CachedNumbersSearcher cannot continue");
			
			cachedPrimes = new ArrayList<Long>();
			maxCachedPrime = 0L;
			
			try(Scanner scanner = new Scanner(cacheFile))
			{
				while(scanner.hasNextLong())
				{
					long nextPrime = scanner.nextLong();
					cachedPrimes.add(nextPrime);
					if(nextPrime > maxCachedPrime)
						maxCachedPrime = nextPrime;
				}
			}
			
			System.out.println("Loaded "+cachedPrimes.size()+" prime numbers.");
		}
		catch(IOException ex)
		{
			throw new RuntimeException("Failed to load cache", ex);
		}
	}
	
	private List<Long> getCache()
	{
		if(cachedPrimes == null)
			loadCache();
		
		return cachedPrimes;
	}

	@Override
	protected boolean isPrime(long number) 
	{
		// calculating the maximum number we have to check
		long max = (long)Math.floor(Math.sqrt(number));
		
		// checking with the cached numbers
		for(long i : getCache())
		{
			if(i > max)
				break;
			
			if(number % i == 0)
				return false;
		}
		
		// if we checked already enough numbers, we're done
		if(max < maxCachedPrime)
			return true;
		
		// otherwise we need to continue checking after biggest loaded prime number
		for(long i = maxCachedPrime + 2; i < max; i+=2)
			if(number % i == 0)
				return false;
		
		return true;
	}
	
}
