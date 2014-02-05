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
package at.ac.tuwien.infosys.jcloudscale.exception;

public class JCloudScaleException extends RuntimeException  {

	private static final long serialVersionUID = 3658744L;

	public JCloudScaleException(Throwable e, String message) 
	{
		super(message, e);
	}
	
	public JCloudScaleException(Throwable e) {
		super(e);
	}
	
	public JCloudScaleException(String message) {
		super(message);
	}
}
