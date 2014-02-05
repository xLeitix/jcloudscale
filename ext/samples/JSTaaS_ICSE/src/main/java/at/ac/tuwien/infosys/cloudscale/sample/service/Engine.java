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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.Test;
import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestOutcome;
 
/**
 * Testing engine that wraps scripting engine and provides comfortable way to access it.
 */
public class Engine {
	
	private ScriptEngine engine;
	
	private String lastMessage = "";
	private TestOutcome lastOutcome = TestOutcome.SUCCESS;
	
	private Engine() {}
	
	public static Engine create(String engineName) {
		
		Engine engine = new Engine();
		engine.engine =  new ScriptEngineManager().getEngineByName(engineName);
		return engine;
		
	}

	public String lastMessage() {
		return lastMessage;
	}

	public TestOutcome lastOutcome() {
		return lastOutcome;
	}

	public void run(Test test) {
		
		// set parameter bindings
		if(test.getParameterBindings() != null) {
			for(String parameterName : test.getParameterBindings().keySet()) {
				engine.put(parameterName, test.getParameterBindings().get(parameterName));
			}
		}
		
		Object returnval = null;
		try {
			returnval = engine.eval(test.getScript()).toString();
		} catch (ScriptException e) {
			lastOutcome = TestOutcome.ERROR;
			lastMessage = "ERROR: "+e.getMessage();
		}
		
		if(returnval != null) {
			if(returnval.equals(test.getExpectedOutcome())) {
				lastOutcome =  TestOutcome.SUCCESS;
				lastMessage = "SUCCESS";
			} else {
				lastOutcome = TestOutcome.FAIL;
				lastMessage = "FAIL: Expected "+test.getExpectedOutcome()+", but was "+returnval;
			}
		}
		
	}

}
