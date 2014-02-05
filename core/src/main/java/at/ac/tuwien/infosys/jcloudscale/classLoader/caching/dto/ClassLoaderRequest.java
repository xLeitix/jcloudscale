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

public class ClassLoaderRequest extends MessageObject implements Serializable 
{
	private static final long serialVersionUID = 465964226126161070L;

	private String requestedClassName;
	private ClassLoaderOffer[] offers;
	
	//-----------------------------------------------------------------
	
	public ClassLoaderRequest(){}
	
	public ClassLoaderRequest(String requestedClassName)
	{
		this.requestedClassName = requestedClassName;
	}
	
	public ClassLoaderRequest(String requestedClassName, ClassLoaderOffer[] offers)
	{
		this(requestedClassName);
		this.offers = offers;
	}
	
	//-----------------------------------------------------------------
	
	public String getRequestedClassName() {
		return requestedClassName;
	}

	public void setRequestedClassName(String requestedClassName) {
		this.requestedClassName = requestedClassName;
	}

	public ClassLoaderOffer[] getOffers() {
		return offers;
	}

	public void setOffers(ClassLoaderOffer[] offers) {
		this.offers = offers;
	}

	//-----------------------------------------------------------------
	
	@Override
	public String toString() 
	{
		return "REQUEST for "+this.requestedClassName+
				" has "+ (this.offers == null ? "no" : this.offers.length) +" offers";
	}
	
	public ClassLoaderOffer getOfferByName(String name)
	{
		if(name == null || this.offers == null || this.offers.length == 0)
			return null;
		
		for(ClassLoaderOffer offer : this.offers)
			if(offer != null && name.equals(offer.getName()))
				return offer;
		
		return null;
	}
}
