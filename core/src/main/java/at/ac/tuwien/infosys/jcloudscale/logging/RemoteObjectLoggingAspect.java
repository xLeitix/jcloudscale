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

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.StartCallbackObject;
import at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost;

@Aspect
public class RemoteObjectLoggingAspect {
	
	private Logger log = null;
	
	public RemoteObjectLoggingAspect() 
	{
		log = JCloudScaleConfiguration.getLogger(CloudManager.class);
	}
	
	@AfterReturning(
		pointcut="execution(java.util.UUID at.ac.tuwien.infosys.jcloudscale.management.CloudManager.createNewInstance(..)) && target(manager) && args(type, *, *, *)",
		returning="returnedId")
	public void logObjectDeployed(UUID returnedId, CloudManager manager, Class<?> type) {
		
		log.info(String.format("Deployed new cloud object of type %s with id %s on host %s. We now manage %d cloud objects on %d hosts.",
				type.getName(),
				returnedId.toString(),
				manager.getHost(returnedId).getIpAddress(),
				manager.countCloudObjects(),
				manager.countVirtualMachines()
		));
		
	}
	
	@Around("call(void at.ac.tuwien.infosys.jcloudscale.management.CloudManager.destructCloudObject(..)) && target(manager)")
	public void logObjectDestructed(ProceedingJoinPoint jp, CloudManager manager) throws Throwable {
		
		UUID coId = (UUID) jp.getArgs()[0]; 
		String address = manager.getHost(coId).getIpAddress();
		
		jp.proceed();
		
		log.info(String.format("Destructed cloud object with id %s on host %s. We now manage %d cloud objects on %d hosts.",
				coId.toString(),
				address,
				manager.countCloudObjects(),
				manager.countVirtualMachines()
		));
		
	}
	
	@Around("execution(Object at.ac.tuwien.infosys.jcloudscale.management.CloudManager.invokeCloudObject(..)) && target(manager) && args(*, method, *, *)")
	public Object logInvokedCloudObject(ProceedingJoinPoint jp, CloudManager manager, Method method) throws Throwable {
		
		UUID coId = (UUID) jp.getArgs()[0];
//		Object[] args = (Object[])jp.getArgs()[2];
		String address = coId == null ? null: manager.getHost(coId).getIpAddress();
		String methodName = method.getName();
		
		long before = System.currentTimeMillis();
		Object ret = jp.proceed();
		long after = System.currentTimeMillis();
		
//		log.info(String.format("Invoked method %s of cloud object %s with parameters %s on host %s. Result= %s Invocation took %d ms.",
//				methodName,
//				coId.toString(),
//				Arrays.toString(args),
//				address,
//				ret,
//				(after - before)
//		));
		
		log.info(String.format("Invoked method %s of cloud object %s on host %s. Invocation took %d ms.",
				methodName,
				coId,
				address,
				(after - before)
		));
		
		return ret;
	}
	
	@Around("call(at.ac.tuwien.infosys.jcloudscale.vm.IVirtualHost at.ac.tuwien.infosys.jcloudscale.vm.VirtualHostPool.findFreeHost(..))")
	public IVirtualHost logSelectedCloudHost(ProceedingJoinPoint jp) throws Throwable {
		
		long before = System.currentTimeMillis();
		IVirtualHost ret = (IVirtualHost) jp.proceed();
		long after = System.currentTimeMillis();
		
		log.info(String.format("Finding a suitable host took %d ms in total.",
				(after - before)
		));
		
		return ret;
		
	}
	
	@Around("execution(void at.ac.tuwien.infosys.jcloudscale.management.JCloudScaleReferenceCallbackListener.MessageHandler.executeCallback(..)) && args(callback)")
	public void referenceHandlerInvoked(ProceedingJoinPoint jp, StartCallbackObject callback) throws Throwable {
		
		log.info(String.format("Starting to execute callback of reference object %s (method %s)",
				callback.getRef().getReferenceObjectId().toString(),
				callback.getMethod()
		));
		
		jp.proceed();
		
		log.info("Finished callback on client side");
		
	}
	
}
