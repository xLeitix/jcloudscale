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
package at.ac.tuwien.infosys.jcloudscale.logging;

import java.util.logging.Logger;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;

@Aspect
public class BasicLoggingAspect {
	
	private Logger log = null;
	
	//TODO: Check if this indeed intercepts all except ignored.
	@Before("execution(!@at.ac.tuwien.infosys.jcloudscale.logging.Ignore * (@at.ac.tuwien.infosys.jcloudscale.logging.Logged *).*(..))")
	public void logMethodStarted(JoinPoint jp) {
		
		String className = jp.getSignature().getDeclaringTypeName();
		
		if(log == null) 
		{
			log = JCloudScaleConfiguration.getLogger(className);
		}
		
		String methodName = jp.getSignature().getName();
		
		log.entering(className, methodName);
		
	}
	
	@AfterReturning(pointcut="execution(!@at.ac.tuwien.infosys.jcloudscale.logging.Ignore * (@at.ac.tuwien.infosys.jcloudscale.logging.Logged *).*(..))", returning="ret")
	public void logMethodFinished(JoinPoint jp, Object ret) {
		
		String className = jp.getSignature().getDeclaringTypeName();
		
		if(log == null) 
		{
			log = JCloudScaleConfiguration.getLogger(className);
		}
		
		String methodName = jp.getSignature().getName();
		
		log.exiting(className, methodName);
		
	}
		
	
}
