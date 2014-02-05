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
package at.ac.tuwien.infosys.jcloudscale.sample.service.openstack.commands;

import java.io.Serializable;

/**
 * Response that suite is executed successfully and results are stored into database.
 */
public class TestSuiteExecutionResponse implements Serializable 
{
	private static final long serialVersionUID = 1L;
	
	private int suiteNr;

	public TestSuiteExecutionResponse(){}
	
	public TestSuiteExecutionResponse(int suiteNr)
	{
		this.suiteNr = suiteNr;
	}
	
	public int getSuiteNr() {
		return suiteNr;
	}

	public void setSuiteNr(int suiteNr) {
		this.suiteNr = suiteNr;
	}
}
