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
package at.ac.tuwien.infosys.jcloudscale.vm;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.exception.ScalingException;

public interface IVirtualHost extends IHost, Closeable {

	public Set<UUID> getManagedObjectIds();

	public boolean isStaticHost();

	public UUID deployCloudObject(ClientCloudObject cloudObjectDescriptor, Object[] args,
			Class<?>[] paramTypes);

	public Object invokeCloudObject(UUID cloudObjectId, Method method, Object[] args, Class<?>[] paramTypes)
			throws JCloudScaleException;

	public Object getCloudObjectFieldValue(UUID id, Field field);

	public void setCloudObjectFieldValue(UUID id, Field field, Object val);

	public void destroyCloudObject(UUID cloudObjectId) throws JCloudScaleException;

	public void startupHost(IHostPool hostPool, String size) throws ScalingException;

	@Override
	public void close();

	public Class<?> getCloudObjectType(UUID cloudObjectId) throws JCloudScaleException;

	public void suspendRunningInvocation(UUID cloudObject, UUID invocation);

	public void continueRunningInvocation(UUID cloudObject, UUID invocation);

	public CloudObjectState getCloudObjectState(UUID cloudObject);

	public List<String> getExecutingMethods(UUID cloudObject);

	public void refreshCloudObjects();
	
	public String getDeclaredInstanceSize();
	
}
