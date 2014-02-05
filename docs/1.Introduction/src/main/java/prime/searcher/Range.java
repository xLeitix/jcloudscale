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

/**
 * @author RST
 * The range class that encapsulates two integer values describing the beginning and end of the range.
 */
public class Range 
{
	private long from;
	private long to;
	
	/**
	 * Creates the new instance of the range class with the specified starting and ending point.
	 * @param from The beginning of the range.
	 * @param to The end of the range.
	 * @throws RuntimeException if from is greater than to.
	 */
	public Range(long from, long to)
	{
		if(from > to)
			throw new RuntimeException("Range is incorrect! From ("+from+") > To ("+to+")");
		
		this.from = from;
		this.to = to;
	}

	public long getFrom() {
		return from;
	}

	public long getTo() {
		return to;
	}
	
	@Override
	public String toString() 
	{
		return "(" + from + ", " + to + ")";
	}
}
