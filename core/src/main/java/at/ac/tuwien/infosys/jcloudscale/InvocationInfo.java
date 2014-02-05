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
package at.ac.tuwien.infosys.jcloudscale;

import java.io.Serializable;

public class InvocationInfo implements Serializable {
	
	private static final long serialVersionUID = 16786234L;
	
	private String objectId;
	private String requestId;
	private String methodName;
	private Object[] params;
	
	public InvocationInfo(String objectId, String requestId, String methodName, Object[] params) {
		this.objectId = objectId;
		this.requestId = requestId;
		this.methodName = methodName;
		this.params = params;
	}
	
	public String getObjectId() {
		return objectId;
	}
	
	public String getRequestId() {
		return requestId;
	}

	public String getMethodName() {
		return methodName;
	}

	public Object[] getParams() {
		return params;
	}
	
	@Override
	public boolean equals(Object other) {
		
		if(!(other instanceof InvocationInfo))
			return false;
		
		return ((InvocationInfo)other).requestId.equals(requestId);
		
	}
	
	@Override
	public int hashCode() {
		return requestId.hashCode();
	}
	
}
