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
package at.ac.tuwien.infosys.jcloudscale.classLoader.caching.dto;

import java.io.Serializable;

import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;

public class ClassLoaderResponse extends MessageObject implements Serializable 
{
	private static final long serialVersionUID = -3753576675023037476L;

	private String requestedClassName;
	private ClassLoaderOffer acceptedOffer;
	
	//----------------------------------------------------------------
	
	public ClassLoaderResponse(){}
	
	public ClassLoaderResponse(String requestedClassName)
	{
		this.requestedClassName = requestedClassName;
	}
	
	public ClassLoaderResponse(String requestedClassname, ClassLoaderOffer acceptedOffer)
	{
		this(requestedClassname);
		this.acceptedOffer = acceptedOffer;
	}
	//----------------------------------------------------------------

	public String getRequestedClassName() {
		return requestedClassName;
	}

	public void setRequestedClassName(String requestedClassName) {
		this.requestedClassName = requestedClassName;
	}

	public ClassLoaderOffer getAcceptedOffer() {
		return acceptedOffer;
	}

	public void setAcceptedOffer(ClassLoaderOffer acceptedOffer) {
		this.acceptedOffer = acceptedOffer;
	}

	@Override
	public String toString() {
		return "RESPONSE for " + this.requestedClassName + " accepted offer:"+ 
				(this.acceptedOffer == null ? "NULL OFFER" : 
					(this.acceptedOffer.getName() == null ? "NEW OFFER" : 
							this.acceptedOffer.getName()));
	}
	
	//----------------------------------------------------------------
	
	
}
