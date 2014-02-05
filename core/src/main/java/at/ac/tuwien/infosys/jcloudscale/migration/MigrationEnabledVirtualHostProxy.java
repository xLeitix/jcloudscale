/*
   Copyright 2013 Fritz Schrogl

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

import javax.jms.JMSException;

import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.logging.Logged;
import at.ac.tuwien.infosys.jcloudscale.migration.objects.MigratedCODeploymentObject;
import at.ac.tuwien.infosys.jcloudscale.migration.objects.MigratedCORemoveObject;
import at.ac.tuwien.infosys.jcloudscale.migration.objects.SerializedCOStateObject;
import at.ac.tuwien.infosys.jcloudscale.migration.objects.SerializedCOStateReturnObject;
import at.ac.tuwien.infosys.jcloudscale.vm.VirtualHostProxy;
 
/**
 * Migration-enabled version of {@link VirtualHostProxy}
 */
@Logged
public class MigrationEnabledVirtualHostProxy extends VirtualHostProxy implements IMigrationEnabledJCloudScaleHost {

	public MigrationEnabledVirtualHostProxy(UUID id) {
		super(id);
	}

	@Override
	public byte[] serializeToMigrate(String id) throws JCloudScaleException {
		UUID corrId = UUID.randomUUID();
		SerializedCOStateObject pack = new SerializedCOStateObject();
		pack.setId(id);
		try {
			lockForResponse(corrId);
			mq.onewayToCSHost(pack, corrId, serverId);
		} catch (JMSException e) {
			log.severe(e.getMessage());
			throw new JCloudScaleException(e);
		} catch (InterruptedException e) {
			log.severe(e.getMessage());
			throw new JCloudScaleException(e);
		}

		SerializedCOStateReturnObject ret;
		try {
			ret = (SerializedCOStateReturnObject) waitForResponse(corrId);
		} catch (InterruptedException e) {
			log.severe(e.getMessage());
			throw new JCloudScaleException(e);
		}
		return ret.getSerialized();
	}

	@Override
	public void deployMigratedCloudObject(String id, String classname,
			byte[] serializedCloudObject) {

		UUID corrId = UUID.randomUUID();

		MigratedCODeploymentObject unpack = new MigratedCODeploymentObject();
		unpack.setCloudObjectId(id);
		unpack.setObjectType(classname);
		unpack.setData(serializedCloudObject);

		try {
			lockForResponse(corrId);
			mq.onewayToCSHost(unpack, corrId, serverId);
		} catch (JMSException e) {
			log.severe(e.getMessage());
			throw new JCloudScaleException(e);
		} catch (InterruptedException e) {
			log.severe(e.getMessage());
			throw new JCloudScaleException(e);
		}

		try {
			waitForResponse(corrId);
		} catch (InterruptedException e) {
			log.severe(e.getMessage());
			throw new JCloudScaleException(e);
		}

	}

	@Override
	public void removeCloudObject(String id) {
		UUID corrId = UUID.randomUUID();

		MigratedCORemoveObject request = new MigratedCORemoveObject();
		request.setCloudObjectId(id);

		try {
			lockForResponse(corrId);
			mq.onewayToCSHost(request, corrId, serverId);
		} catch (JMSException e) {
			log.severe(e.getMessage());
			throw new JCloudScaleException(e);
		} catch (InterruptedException e) {
			log.severe(e.getMessage());
			throw new JCloudScaleException(e);
		}

		try {
			waitForResponse(corrId);
		} catch (InterruptedException e) {
			log.severe(e.getMessage());
			throw new JCloudScaleException(e);
		}
	}

//	@Override
//	public long getCloudObjectSize(String objectId) {
//
//		UUID corrId = UUID.randomUUID();
//
//		GetCloudObjectSizeObject request = new GetCloudObjectSizeObject();
//		request.setCloudObjectId(objectId);
//
//		try {
//			lockForResponse(corrId);
//			mq.onewayToCSHost(request, corrId, serverId);
//		} catch (JMSException e) {
//			log.severe(e.getMessage());
//			throw new JCloudScaleException(e);
//		} catch (InterruptedException e) {
//			log.severe(e.getMessage());
//			throw new JCloudScaleException(e);
//		}
//
//		GetCloudObjectSizeObjectReturn ret;
//		try {
//			ret = (GetCloudObjectSizeObjectReturn) waitForResponse(corrId);
//		} catch (InterruptedException e) {
//			log.severe(e.getMessage());
//			throw new JCloudScaleException(e);
//		}
//
//		return ret.getCloudObjectSize();
//	}

}
