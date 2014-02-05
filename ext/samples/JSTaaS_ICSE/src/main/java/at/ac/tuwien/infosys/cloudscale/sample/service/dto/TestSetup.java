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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Test Setup class that defines a set of test suites that should be executed.
 */
@XmlRootElement(name="TestSetup")
public class TestSetup {
	
	private int testId;
	private String engine;
	private List<TestSuite> suites;
	
	public List<TestSuite> getSuites() {
		return suites;
	}

	public void setSuites(List<TestSuite> suites) {
		this.suites = suites;
	}

	public String getEngine() {
		return engine;
	}

	public void setEngine(String engine) {
		this.engine = engine;
	}

	public int getTestId() {
		return testId;
	}

	public void setTestId(int testId) {
		this.testId = testId;
	}
	
	public void addSuite(TestSuite suite) {
		if(suites == null)
			suites = new ArrayList<>();
		suites.add(suite);
	}
	
}
