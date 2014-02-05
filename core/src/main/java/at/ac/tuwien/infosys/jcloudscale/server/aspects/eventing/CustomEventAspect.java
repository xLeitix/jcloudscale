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
package at.ac.tuwien.infosys.jcloudscale.server.aspects.eventing;


import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.utility.ReflectionUtil;

@Aspect
public class CustomEventAspect extends EventAspect {
	
	@AfterReturning("this(obj) && execution((@at.ac.tuwien.infosys.jcloudscale.annotations.CloudObject *).new(..))")
	public void matchObjectCreated(Object obj) throws JCloudScaleException {
		
		ReflectionUtil.injectEventSink(obj);
			
	}
	
}
