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
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import prime.scheduler.splitter.ISplitter;
import prime.scheduler.splitter.LinearSplitter;
import prime.searcher.Range;

public class RegionSplitterTest 
{
	private ISplitter splitter;
	
	@Before
	public void init()
	{
		splitter = new LinearSplitter();
	}
	
	
	@Test
	public void someSimpleTest()
	{
		Range range = new Range(1, 100);
		int splits = 4;
		assertEquals("Splitting was incorrect.", true, isSplittingCorrect(range, splits, splitter.split(range, splits)));
		
		range = new Range(1, 4);
		splits = 1;
		assertEquals("Splitting was incorrect.", true, isSplittingCorrect(range, splits, splitter.split(range, splits)));
		
		range = new Range(1, 10);
		splits = 1;
		assertEquals("Splitting was incorrect.", true, isSplittingCorrect(range, splits, splitter.split(range, splits)));
		
		range = new Range(1, 10);
		splits = 2;
		assertEquals("Splitting was incorrect.", true, isSplittingCorrect(range, splits, splitter.split(range, splits)));
		
		range = new Range(1, 10);
		splits = 6;
		assertEquals("Splitting was incorrect.", true, isSplittingCorrect(range, splits, splitter.split(range, splits)));
		
		range = new Range(1, 100);
		splits = 50;
		assertEquals("Splitting was incorrect.", true, isSplittingCorrect(range, splits, splitter.split(range, splits)));
	}
	
	@Test
	public void cornerCasesTest()
	{
		Range range = new Range(1, 4);
		int splits = 4;
		assertEquals("Splitting was incorrect.", true, isSplittingCorrect(range, splits, splitter.split(range, splits)));
		
		range = new Range(1, 10);
		splits = 6;
		assertEquals("Splitting was incorrect.", true, isSplittingCorrect(range, splits, splitter.split(range, splits)));
		
		range = new Range(1, 10);
		splits = 10;
		assertEquals("Splitting was incorrect.", true, isSplittingCorrect(range, splits, splitter.split(range, splits)));
	}
	
	@Test(expected = RuntimeException.class)
	public void errorTests()
	{
		Range range = new Range(10, 10);
		int splits = 10;
		assertEquals("Splitting was incorrect.", true, isSplittingCorrect(range, splits, splitter.split(range, splits)));
	}
	
	@Test(expected = RuntimeException.class)
	public void errorTests2()
	{
		Range range = new Range(1, 10);
		int splits = 100;
		assertEquals("Splitting was incorrect.", true, isSplittingCorrect(range, splits, splitter.split(range, splits)));
	}
	
	/**
	 * Checks if the splitting is covering the whole range and has desired amount of sub-ranges.
	 * @return <b>true</b> if splitting is correct. Otherwise -- <b>false</b>.
	 */
	private boolean isSplittingCorrect(Range initRange, int splits, Range[] splittingResult)
	{
		//checking amount of splits
		if(splittingResult == null || (splittingResult.length != splits && (initRange.getTo() - initRange.getFrom())>splits))
			return false;
		
		//checking if splits are correct.
		long position = initRange.getFrom();
		for(Range range : splittingResult)
		{
			if(position != range.getFrom())
				return false;
			
			position = range.getTo()+1;
		}
		
		if(position != initRange.getTo() +1)
			return false;
		
		return true;
	}
}
