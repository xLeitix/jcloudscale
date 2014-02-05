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
package at.ac.tuwien.infosys.jcloudscale.messaging.objects;

import at.ac.tuwien.infosys.jcloudscale.utility.InvocationStatus;


public class InvocationResultReturnObject extends ReturnObject  {
	 
	private static final long serialVersionUID = 1L;
	
	private String requestId;
	private String objectId;
	private byte[] result;
	private InvocationStatus status;
	
	public String getObjectId() {
		return objectId;
	}
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}
	public String getRequestId() {
		return requestId;
	}
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	public byte[] getResult() {
		return result;
	}
	public void setResult(byte[] result) {
		this.result = result;
	}
	public InvocationStatus getStatus() {
		return status;
	}
	public void setStatus(InvocationStatus status) {
		this.status = status;
	}

}
