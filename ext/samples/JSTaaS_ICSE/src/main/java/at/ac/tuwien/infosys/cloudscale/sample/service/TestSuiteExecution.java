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
package at.ac.tuwien.infosys.jcloudscale.sample.service;

import at.ac.tuwien.infosys.jcloudscale.annotations.ByValueParameter;

public class TestSuiteExecution {
	
	private TestStatus[] testStatuses;
	
	public TestSuiteExecution() { }
	
	public TestSuiteExecution(int nrOfTests) {
		testStatuses = new TestStatus[nrOfTests];
		for(int i = 0; i< testStatuses.length; i++)
			testStatuses[i] = TestStatus.NONE; 
	}
	
	public void changeStatus(@ByValueParameter TestStatus newStatus, int nr) {
		testStatuses[nr] = newStatus;
	}
	
	public boolean done() {
		for(int i = 0; i< testStatuses.length; i++)
			if(testStatuses[i] != TestStatus.DONE)
				return false;
		return true;
	}
	
}
