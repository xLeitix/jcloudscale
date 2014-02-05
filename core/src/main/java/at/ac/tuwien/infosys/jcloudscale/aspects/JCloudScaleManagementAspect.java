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
package at.ac.tuwien.infosys.jcloudscale.aspects;

import java.io.IOException;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;

@Aspect
public class JCloudScaleManagementAspect 
{
	@After("execution(@at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown * *(..))")
	public void shutdownJCloudScale()
	{
		if(JCloudScaleConfiguration.isServerContext())
			return;//in case this method is inside cloudObject...
		
		try 
		{
			JCloudScaleClient.closeClient();
		} catch (IOException e) 
		{
			throw new JCloudScaleException(e, "Failed to close JCloudScale Client.");
		}
	}
}
