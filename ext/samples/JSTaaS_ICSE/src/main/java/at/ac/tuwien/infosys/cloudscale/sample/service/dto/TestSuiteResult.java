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

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * A set of results from the tests defined by a test suite.
 */
@XmlRootElement(name = "SuiteResult")
public class TestSuiteResult {
	
	private String name;
	private Map<Integer,TestResult> results = new HashMap<>();
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Map<Integer,TestResult> getResults() {
		return results;
	}
	public void setResults(Map<Integer,TestResult> results) {
		this.results = results;
	}
	public void setResult(int idx, TestResult result) {
		this.results.put(idx, result);
	}
	
}
