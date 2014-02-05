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
package at.ac.tuwien.infosys.jcloudscale.utility;

/**
 * @author RST
 * Cancellation token class that allows to cancel asynchronous operation from the outside.
 */
public class CancellationToken 
{
	private volatile boolean isCancelled = false;
	private CancellationToken parentToken;
	
	/**
	 * Creates a new instance of CancellationToken
	 */
	public CancellationToken()
	{
	}
	
	/**
	 * Creates a new instance of CancellationToken with parentToken specified.
	 * @param parentToken The parent token. 
	 * If the parent token will be cancelled, this token will report as cancelled as well. 
	 */
	public CancellationToken(CancellationToken parentToken)
	{
		this.parentToken = parentToken;
	}
	
	/**
	 * Cancels the token.
	 */
	public void cancel()
	{
		isCancelled = true;
	}
	
	/**
	 * Determines if the operation has to be cancelled.
	 * Asynchronous operations are expected to check this method periodically.
	 * @return <b>true</b> if operation was cancelled. Otherwise, <b>false</b>.
	 */
	public boolean isCancelled()
	{
		return isCancelled || (parentToken != null && parentToken.isCancelled());
	}
	
	/**
	 * Allows to check if provided token is cancelled.
	 * This operation is useful if you don't want to check for a null in addition.
	 * @param token The token to check or null.
	 * @return <b>true</b> if token is provided and it is cancelled. Otherwise, <b>false</b>.
	 */
	public static boolean isCancelled(CancellationToken token)
	{
		return token != null && token.isCancelled();
	}
}
