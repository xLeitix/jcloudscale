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
package at.ac.tuwien.infosys.jcloudscale.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import at.ac.tuwien.infosys.jcloudscale.InvocationInfo;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.management.JCloudScaleReferenceManager;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.InvocationResultReturnObject;
import at.ac.tuwien.infosys.jcloudscale.utility.InvocationStatus;
import at.ac.tuwien.infosys.jcloudscale.utility.ReflectionUtil;
import at.ac.tuwien.infosys.jcloudscale.utility.SerializationUtil;

public class CloudObjectInvocation {
	
	private JCloudScaleServer parentServer;
	private UUID objectId;
	private UUID requestId;

	private Object cloudObject;
	private Method method;
	private Object[] params;
	private Object returnVal;
	private JCloudScaleException error;
	private InvocationStatus status;
	private IMQWrapper mq; 
	
	private Thread invocation = null;
	
	public CloudObjectInvocation(JCloudScaleServer server, IMQWrapper mq) {
		this.parentServer = server;
		this.mq = mq;
		this.status = InvocationStatus.NOT_STARTED;
	}
	
	public void invoke() {
		
		invocation = new Thread() {
			
			@Override
			public void run() {
				try {
					
					// inject invocation info
					ReflectionUtil.addInvocationInfo(cloudObject,
							new InvocationInfo(objectId.toString(), requestId.toString(), method.getName(), params)
					);
					
					status = InvocationStatus.RUNNING;
					returnVal = method.invoke(cloudObject, params);
					status = InvocationStatus.FINISHED;
				} catch (InvocationTargetException e) {
					Throwable t = e.getCause();
					parentServer.logException(t);
					error = new JCloudScaleException(t, "Error while invoking user object");
					status = InvocationStatus.FAULTED;
				} catch (Throwable e) {
					e.printStackTrace();
					parentServer.logException(e);
					error = new JCloudScaleException(e, "Unable to invoke user object");
					status = InvocationStatus.FAULTED;
				}
				
				
				try {
					ReflectionUtil.removeInvocationInfo(cloudObject, requestId.toString());
					returnInvocationResult();
				} catch (IOException e) {
					parentServer.logException(e);
				}
			}
		};
		invocation.start();
		
	}
	
	@SuppressWarnings("deprecation")
	public void suspend() {
		
		if(status != InvocationStatus.RUNNING)
			throw new JCloudScaleException("Cannot suspend request because of illegal status: "+status);
		
		// is there a better way to do this?
		invocation.suspend();
		status = InvocationStatus.SUSPENDED;
		
	}
	
	@SuppressWarnings("deprecation")
	public void resume() {
		
		if(status != InvocationStatus.SUSPENDED)
			throw new JCloudScaleException("Cannot resume request because of illegal status: "+status);
		
		// is there a better way to do this?
		invocation.resume();
		status = InvocationStatus.RUNNING;
		
	}
	
	public InvocationStatus getStatus() {
		return status;
	}
	
	public JCloudScaleServer getParentServer() {
		return parentServer;
	}
	public UUID getObjectId() {
		return objectId;
	}

	public void setObjectId(UUID objectId) {
		this.objectId = objectId;
	}

	public UUID getRequestId() {
		return requestId;
	}
	public void setRequestId(UUID requestId) {
		this.requestId = requestId;
	}
	public void setParentServer(JCloudScaleServer parentServer) {
		this.parentServer = parentServer;
	}
	public Object getCloudObject() {
		return cloudObject;
	}
	public void setCloudObject(Object cloudObject) {
		this.cloudObject = cloudObject;
	}
	public Method getMethod() {
		return method;
	}
	public void setMethod(Method method) {
		this.method = method;
	}
	public Object[] getParams() {
		return params;
	}
	public void setParams(Object[] params) {
		this.params = params;
	}
	public Object getReturnVal() {
		return returnVal;
	}
	public void setReturnVal(Object returnVal) {
		this.returnVal = returnVal;
	}
	public JCloudScaleException getError() {
		return error;
	}
	public void setError(JCloudScaleException error) {
		this.error = error;
	} 
	
	private void returnInvocationResult() throws IOException {
		
		InvocationResultReturnObject ret = new InvocationResultReturnObject();
		if(returnVal instanceof Throwable)
			ret.setException((Throwable) returnVal);
		else {
			
			// see if the result is a reference, and if it is, replace it
			Object theReturn = JCloudScaleReferenceManager.getInstance().processReturn(method, returnVal);
			
			byte[] retSerialized = SerializationUtil.serializeToByteArray(theReturn);
			ret.setResult(retSerialized);
		}
		ret.setRequestId(requestId.toString());
		ret.setObjectId(objectId.toString());
		ret.setStatus(status);
		ret.setException(error);
		
		try {
			mq.oneway(ret, requestId);
		} catch (Exception e) {
			parentServer.logException(e);
		}
			
		ReflectionUtil.removeInvocationInfo(parentServer.getCloudObject(objectId), requestId.toString());
	}
	
}
