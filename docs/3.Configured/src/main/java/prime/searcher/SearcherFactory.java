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
 * Represents the search factory that allows to instantiate the configured instance of the <b>ISearcher</b> interface implementation. 
 */
public class SearcherFactory 
{
	/**
	 * Creates the predefined searcher implementation initialized with the provided range.
	 * @param range The range for searcher to work on.
	 * @return The <b>ISearcher</b> implementation that allows to run search of prime numbers.
	 */
	public static ISearcher createSearcher(Range range)
	{
		// currently we have only 1 implementation, but noone knows what will happen in future.
		return new SimpleSearcher(range);
	}
}
