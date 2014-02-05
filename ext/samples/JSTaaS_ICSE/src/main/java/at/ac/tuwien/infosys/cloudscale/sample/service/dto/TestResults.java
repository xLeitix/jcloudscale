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

import at.ac.tuwien.infosys.jcloudscale.datastore.annotations.DatastoreId;
import at.ac.tuwien.infosys.jcloudscale.datastore.api.IdStrategy;

import com.google.gson.annotations.SerializedName;

/**
 * Set of test results that defines the object that will be stored to the database.
 */
@XmlRootElement(name = "TestResults")
public class TestResults{
	
	@DatastoreId(strategy=IdStrategy.MANUAL)
	@SerializedName("_id")//TODO: needed only for CouchDB
	private String testsId;
	
	@SerializedName("_rev")
	private String rev=null;//TODO: needed only for CouchDB
	
	private Map<String,TestSuiteResult> results = new HashMap<>();

	public Map<String,TestSuiteResult> getResults() {
		return results;
	}

	public void setResults(Map<String,TestSuiteResult> results) {
		this.results = results;
	}

	public String getTestsId() {
		return testsId;
	}

	public void setTestsId(String testsId) {
		this.testsId = testsId;
	}

	public void putResult(String suiteName, TestSuiteResult result) {
		results.put(suiteName, result);
	}

}
