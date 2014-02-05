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
package at.ac.tuwien.infosys.jcloudscale.datastore.test.aspect;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class JUnitTestSeparatorAspect {
	
	@Before("execution(@org.junit.Test void *.*(..))")
	public void runTest(JoinPoint jp) {
		
		MethodSignature method = (MethodSignature) jp.getSignature();
		Object that = jp.getThis();
		System.err.println("############################################################");
		System.err.println("############################################################");
		System.err.println("Launching test "+that.getClass().getCanonicalName()+"."+method.getName()+"()");
		System.err.println("############################################################");
		System.err.println("############################################################");
		
		
	}
	
}
