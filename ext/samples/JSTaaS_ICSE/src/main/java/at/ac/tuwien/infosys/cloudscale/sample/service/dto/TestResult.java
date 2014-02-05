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
package at.ac.tuwien.infosys.jcloudscale.sample.service.dto;

import javax.xml.bind.annotation.XmlRootElement;
 
/**
 * Test execution result that defines the message and actual outcome. 
 */
@XmlRootElement(name = "Result")
public class TestResult {
	
	private Integer testId;
	
	private TestOutcome outcome;
	
	private String message;
	
	public Integer getTestId() {
		return testId;
	}
	public void setTestId(Integer testId) {
		this.testId = testId;
	}
	public TestOutcome getOutcome() {
		return outcome;
	}
	public void setOutcome(TestOutcome outcome) {
		this.outcome = outcome;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
}
