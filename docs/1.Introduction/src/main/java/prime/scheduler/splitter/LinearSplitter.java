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
package prime.scheduler.splitter;

import java.util.ArrayList;
import java.util.List;

import prime.searcher.Range;


/**
 * @author RST
 * Splits initial range linearly. If the amount of elements in initial range is insufficient, 
 * throws <b>RuntimeException</b>
 * 
 * Note, that linear splitting is not the best way to split range for prime numbers search 
 * as smaller numbers take less time to analyze than bigger.
 */
public class LinearSplitter implements ISplitter {

	@Override
	public Range[] split(Range range, int count) 
	{
		if(range.getTo() - range.getFrom() + 1 < count)
			throw new RuntimeException("Not enough elements for split.");
		
		List<Range> ranges = new ArrayList<Range>();
		
		double step = Math.max(0, (double)(range.getTo() - range.getFrom() + 1)/count - 1);
		
		double start = range.getFrom();
		double end = start;
		
		for(int i=0; i <count; ++i)
		{
			end = start + step;
			end = Math.min(range.getTo(), end);
			
			ranges.add(new Range((int)Math.round(start), (int)Math.round(end)));
			
			start = end+1;
		}
		
		return ranges.toArray(new Range[ranges.size()]);
	}

}
