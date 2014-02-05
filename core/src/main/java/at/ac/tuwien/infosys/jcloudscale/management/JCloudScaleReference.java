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
package at.ac.tuwien.infosys.jcloudscale.management;

import java.io.Serializable;
import java.util.UUID;

import at.ac.tuwien.infosys.jcloudscale.utility.CgLibUtil;

public class JCloudScaleReference implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private UUID referenceObjectId;
	private UUID referencingHostId;
	private String referenceObjectClassName;
	
	public JCloudScaleReference() {}
	
	public JCloudScaleReference(UUID ref, UUID referencingHost, Object referencingObject) {
		this.referenceObjectId = ref;
		this.referencingHostId = referencingHost;
		this.referenceObjectClassName = findBaseClassName(referencingObject.getClass());
	}
	
	private String findBaseClassName(Class<?> clazz) {
		if(!CgLibUtil.isCGLibEnhancedClass(clazz))
			return clazz.getName();
		else
			return findBaseClassName(clazz.getSuperclass());
	}
	
	public UUID getReferencingHostId() {
		return referencingHostId;
	}

	public void setReferencingHostId(UUID referencingHostId) {
		this.referencingHostId = referencingHostId;
	}

	public UUID getReferenceObjectId() {
		return referenceObjectId;
	}

	public void setReferenceObjectId(UUID referenceObjectId) {
		this.referenceObjectId = referenceObjectId;
	}
	
	public String getReferenceObjectClassName() {
		return referenceObjectClassName;
	}

	public void setReferenceObjectClassName(String referenceObjectClassName) {
		this.referenceObjectClassName = referenceObjectClassName;
	}

	@Override
	public String toString() {
		
		return String.format(super.toString()+"("+referenceObjectClassName+")");
		
	}
	
}
