/*
   Copyright 2014 Philipp Leitner 

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
package at.ac.tuwien.infosys.jcloudscale.migration;

import java.util.UUID;

import at.ac.tuwien.infosys.jcloudscale.vm.IHost;
import at.ac.tuwien.infosys.jcloudscale.vm.IHostPool;

public interface IMigrationEnabledHostPool extends IHostPool {
	
	void migrateObject(UUID object, IHost targetHost);
	void migrateObjectAsync(UUID object, IHost targetHost);
	void migrateObjectAsync(UUID object, IHost targetHost, IObjectMigratedCallback callback);
	
    public interface IObjectMigratedCallback
    {
        void migrationFinished();
    }
	
}
