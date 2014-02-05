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
package at.ac.tuwien.infosys.jcloudscale.monitoring;

import java.util.HashMap;

import com.espertech.esper.client.EventBean;

public class MonitoringMetric {
	
	private String name;
	private String epl;
	private String resultField;
	
	public String getResultField() {
		return resultField;
	}
	public void setResultField(String resultField) {
		this.resultField = resultField;
	}
	private Class<?> type;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEpl() {
		return epl;
	}
	public void setEpl(String epl) {
		this.epl = epl;
	}
	public Class<?> getType() {
		return type;
	} 
	public void setType(Class<?> type) {
		this.type = type;
	}
	
	public Object getValueFromBean(EventBean event) throws SecurityException,
		NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		
		if(resultField == null)
			return event.getUnderlying();
		@SuppressWarnings("unchecked")
        HashMap<String, Object> resultmap = (HashMap<String, Object>) event.getUnderlying();
		return resultmap.get(resultField);
		
	}
	
	
}
