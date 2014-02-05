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

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

import prime.searcher.ISearcher;
import prime.searcher.Range;
import prime.searcher.SearcherFactory;
import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown;

public class PrimeSearcherTest 
{
	@After
	@JCloudScaleShutdown
	public void cleanup()
	{
		
	}
	
	@Test
	public void testSomePrimeNumbers() 
	{
		// checking some regions
		ISearcher searcher = SearcherFactory.createSearcher(new Range(1, 10));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 5, searcher.getResult());
		
		searcher = SearcherFactory.createSearcher(new Range(10, 20));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 4, searcher.getResult());
		
		searcher = SearcherFactory.createSearcher(new Range(10, 11));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 1, searcher.getResult());
		
		searcher = SearcherFactory.createSearcher(new Range(99, 101));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 1, searcher.getResult());
		
		searcher = SearcherFactory.createSearcher(new Range(99, 100));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 0, searcher.getResult());
		
		
		// and some separate numbers
		searcher = SearcherFactory.createSearcher(new Range(131, 131));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 1, searcher.getResult());
		
		searcher = SearcherFactory.createSearcher(new Range(10003, 10003));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 0, searcher.getResult());
		
		// and corner cases
		searcher = SearcherFactory.createSearcher(new Range(1, 1));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 1, searcher.getResult());
		
		searcher = SearcherFactory.createSearcher(new Range(2, 2));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 1, searcher.getResult());
		
		searcher = SearcherFactory.createSearcher(new Range(3, 3));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 1, searcher.getResult());
		
		searcher = SearcherFactory.createSearcher(new Range(10, 10));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 0, searcher.getResult());
	}
	
	@Test(expected = RuntimeException.class)
	public void failZeroTest()
	{
		ISearcher searcher = SearcherFactory.createSearcher(new Range(0, 0));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 1, searcher.getResult());
	}
	
	@Test(expected = RuntimeException.class)
	public void failWrongRegionTest()
	{
		ISearcher searcher = SearcherFactory.createSearcher(new Range(10, 1));
		searcher.run();
		assertEquals("Amount of prime numbers in range is not correct", 1, searcher.getResult());
	}

}
