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
package at.ac.tuwien.infosys.jcloudscale;

import java.util.UUID;


public interface IJCloudScaleServer {
	
	public String createNewCloudObject(String classname, byte[] params, String[] paramNames);
	
	public String startInvokingCloudObject(String objectId, String method, byte[] params, String[] paramNames);
	
	public byte[] getCloudObjectField(String objectId, String field);
	
	public void setCloudObjectField(String objectId, String field, byte[] value);
	
	public void suspendInvocation(String objectId, String request);
	
	public void resumeInvocation(String objectId, String request);
	
	public void destroyCloudObject(String id);

	public String getCloudObjectType(String id);
	
	public void keepAliveCloudObject(UUID id);
	
	public void shutdown();
	
}
