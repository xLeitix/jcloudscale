/*
 * Copyright 2013 Philipp Leitner Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package at.ac.tuwien.infosys.jcloudscale.vm;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.naming.NamingException;

import at.ac.tuwien.infosys.jcloudscale.IJCloudScaleServer;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
import at.ac.tuwien.infosys.jcloudscale.logging.Logged;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.TimeoutException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.CreateObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.CreateReturnObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.DeleteObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.GetFieldValueObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.GetFieldValueResponseObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.InvocationResultReturnObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.KeepaliveObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ReturnObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.SetFieldValueObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ShutdownObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.StartInvokationObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.StartInvokationReturnObject;
import at.ac.tuwien.infosys.jcloudscale.server.ServerConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.localVm.ClientReturnListener;

@Logged
public class VirtualHostProxy implements IJCloudScaleServer, Closeable {

    protected IMQWrapper mq;
    protected Logger log;
    protected UUID serverId;
    protected long invocationTimeout;

    private ClientReturnListener returnListener;
    /**
     * Unlocked latch that is used for thread-safe unlock operation.
     */
    private static final CountDownLatch unlockedLatch = new CountDownLatch(0);
    
    private ConcurrentHashMap<UUID, CountDownLatch> locks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<UUID, ReturnObject> returnValues = new ConcurrentHashMap<>();

    public VirtualHostProxy(UUID id) {
        this.serverId = id;
        this.log = JCloudScaleConfiguration.getLogger(this);
        initQueue();
        registerResponseListener();
    }

    private void initQueue() {
        try {
            mq = JCloudScaleConfiguration.createMQWrapper();

            ServerConfiguration config = JCloudScaleConfiguration
                    .getConfiguration().server();
            invocationTimeout = config.getInvocationTimeout();

            mq.createTopicProducer(config.getRequestQueueName());
            mq.createTopicConsumer(config.getResponseQueueName());

        } catch (Exception e) {
            log.severe("Unable to init message queue in JCloudScale client: "
                    + e.getMessage());
            //            e.printStackTrace();
            throw new JCloudScaleException(e,
                    "Unable to init message queue in JCloudScale client");
        }
    }

    private void registerResponseListener() throws JCloudScaleException {

        returnListener = new ClientReturnListener(this);
        try {
            mq.registerListener(returnListener);
        } catch (JMSException e) {
            log.severe("Unable to register message listener in JCloudScale client: "
                    + e.getMessage());
            //            e.printStackTrace();
            throw new JCloudScaleException(e);
        }

    }

    @Override
    public void close() {
        if (this.returnListener != null) {
            returnListener.close();
            returnListener = null;
        }

        try {
            if (mq != null) {
                mq.close();
                mq = null;
            }

            locks.clear();
            returnValues.clear();
        } catch (Exception e) {
            log.severe(e.getMessage());
            throw new JCloudScaleException(e);
        }
    }

    @Override
    public String createNewCloudObject(String classname, byte[] params,
            String[] paramNames) {

        UUID corrId = UUID.randomUUID();
        CreateObject create = new CreateObject();
        create.setClassname(classname);
        create.setParams(params);
        create.setParamNames(paramNames);

        try {
            lockForResponse(corrId);
            mq.onewayToCSHost(create, corrId, serverId);

        } catch (JMSException | InterruptedException e) {
            log.severe(e.getMessage());
            throw new JCloudScaleException(e);
        }

        CreateReturnObject ret;
        try {
            ret = (CreateReturnObject) waitForResponse(corrId);
        } catch (InterruptedException e) {
            log.severe(e.getMessage());
            throw new JCloudScaleException(e);
        }
        return ret.getReturnval();
    }

    @Override
    public String startInvokingCloudObject(String objectId, String method,
            byte[] params, String[] paramNames) throws JCloudScaleException {

        StartInvokationObject start = new StartInvokationObject();
        start.setId(objectId);
        start.setMethod(method);
        start.setParams(params);
        start.setParamNames(paramNames);
        UUID corrId = UUID.randomUUID();

        try {
            lockForResponse(corrId);
            mq.onewayToCSHost(start, corrId, serverId);
        } catch (JMSException | InterruptedException e) {
            log.severe(e.getMessage());
            throw new JCloudScaleException(e);
        }

        StartInvokationReturnObject ret;
        try {
            ret = (StartInvokationReturnObject) waitForResponse(corrId);
        } catch (InterruptedException e) {
            log.severe(e.getMessage());
            throw new JCloudScaleException(e);
        }

        String retval = ret.getReturnval();
        try {
            lockForResponse(UUID.fromString(retval));
        } catch (InterruptedException e) {
            log.severe(e.getMessage());
            throw new JCloudScaleException(e);
        }
        return retval;
    }

    @Override
    public byte[] getCloudObjectField(String objectId, String field) {

        GetFieldValueObject request = new GetFieldValueObject();
        request.setField(field);
        request.setObject(objectId);
        UUID corrId = UUID.randomUUID();
        GetFieldValueResponseObject ret = null;
        try (IMQWrapper mq = JCloudScaleConfiguration.createMQWrapper()) {
            //TODO: creation of MQWrapper on every request introduces significant overhead.
            mq.createTopicProducer(JCloudScaleConfiguration.getConfiguration()
                    .server().getRequestQueueName());
            mq.createTopicConsumer(JCloudScaleConfiguration.getConfiguration()
                    .server().getResponseQueueName(), "JMSCorrelationID = '"
                            + corrId.toString() + "'");

            ret = (GetFieldValueResponseObject) mq.requestResponseToCSHost(
                    request, corrId, serverId);

        } catch (NamingException | JMSException e) {
            log.severe(e.getMessage());
            throw new JCloudScaleException(e);
        } catch (TimeoutException e) {
            log.severe("Timed out waiting for response from server to getfieldvalue request: "
                    + e.getMessage());
            throw new JCloudScaleException(e);
        }

        return ret.getFieldValue();

    }

    @Override
    public void setCloudObjectField(String objectId, String field, byte[] value) {

        SetFieldValueObject request = new SetFieldValueObject();
        request.setField(field);
        request.setObject(objectId);
        request.setValue(value);
        UUID corrId = UUID.randomUUID();
        try (IMQWrapper mq = JCloudScaleConfiguration.createMQWrapper()) {
            //TODO: creation of MQWrapper on every request introduces significant overhead.
            mq.createTopicProducer(JCloudScaleConfiguration.getConfiguration()
                    .server().getRequestQueueName());
            mq.createTopicConsumer(JCloudScaleConfiguration.getConfiguration()
                    .server().getResponseQueueName(), "JMSCorrelationID = '"
                            + corrId.toString() + "'");
            mq.requestResponseToCSHost(request, corrId, serverId);

        } catch (NamingException | JMSException e) {
            log.severe(e.getMessage());
            throw new JCloudScaleException(e);
        } catch (TimeoutException e) {
            log.severe("Timed out waiting for response from server to set-field field request: "
                    + e.getMessage());
            throw new JCloudScaleException(e);
        }

    }

    @Override
    public void suspendInvocation(String objectId, String request)
            throws JCloudScaleException {
        throw new JCloudScaleException("Not yet ported");
    }

    @Override
    public void resumeInvocation(String objectId, String request)
            throws JCloudScaleException {
        throw new JCloudScaleException("Not yet ported");
    }

    @Override
    public void destroyCloudObject(String id) throws JCloudScaleException {

        UUID corrId = UUID.randomUUID();
        DeleteObject delete = new DeleteObject();
        delete.setId(id);
        try {
            lockForResponse(corrId);
            mq.onewayToCSHost(delete, corrId, serverId);
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
	public void keepAliveCloudObject(UUID id) throws JCloudScaleException {
		
		KeepaliveObject keep = new KeepaliveObject();
		keep.setId(id);
		UUID corrId = UUID.randomUUID();
		try {
			mq.onewayToCSHost(keep, corrId, serverId);
		} catch (JMSException e) {
            log.severe(e.getMessage());
            throw new JCloudScaleException(e);
		}
		
	}
    
    @Override
    public String getCloudObjectType(String id) throws JCloudScaleException {
        throw new JCloudScaleException("Not yet ported");
    }

    @Override
    public void shutdown() {

        ShutdownObject shutdown = new ShutdownObject();
        try {
            mq.onewayToCSHost(shutdown, null, serverId);
        } catch (JMSException e) {
            log.severe(e.getMessage());
            throw new JCloudScaleException(e);
        }
        close();
    }

    public void unlock(UUID corrid) {
    	//
    	// If there was latch in the collection, we have to unlock it.
    	// if not, we put there unlocked already latch.
    	//
        CountDownLatch previousLatch = locks.putIfAbsent(corrid, unlockedLatch);

        if(previousLatch != null)
            previousLatch.countDown();
    }

    public void setResult(UUID corrId, ReturnObject ret) {
        returnValues.put(corrId, ret);
    }

    public byte[] waitForResult(String invocationId)
            throws JCloudScaleException, InterruptedException {

        ReturnObject ret = waitForResponse(UUID.fromString(invocationId));
        InvocationResultReturnObject invResult = (InvocationResultReturnObject) ret;
        return invResult.getResult();

    }

    protected void lockForResponse(UUID corrId) throws InterruptedException {
        // we have to try putting new lock to handle properly the case when the reply arrives faster than we lock.
        if(locks.putIfAbsent(corrId, new CountDownLatch(1)) != null)
            log.fine("Attempted to lock for request "+corrId+", but request with this id was already locked (possibly because the reply is already here).");
    }

    protected ReturnObject waitForResponse(UUID corrId)
            throws InterruptedException, JCloudScaleException {

        // when we get response, the latch will be unlocked.
        CountDownLatch latch = locks.get(corrId);

        if(latch != null) // if the latch is null, we are released already (or not locked yet?).
        {
            if(!latch.await(invocationTimeout, TimeUnit.MILLISECONDS))
                throw new JCloudScaleException("Failed to receive reply to the invocation "+corrId+" within timeout "+invocationTimeout+" ms.");
            else
                if(!locks.remove(corrId, latch))
                    throw new JCloudScaleException("Failed to remove locking object responsible for invocation "+corrId+": lock object was replaced during response waiting.");
        }

        if (returnValues.containsKey(corrId)) {
            ReturnObject ret = returnValues.get(corrId);
            returnValues.remove(corrId);

            checkForException(ret);

            return ret;
        } else {
            throw new JCloudScaleException(
                    "Internal Exception: got notified of incoming result, but no result is available");
        }

    }

    private void checkForException(ReturnObject ret) throws JCloudScaleException {

        if (ret.hasFailed()) {
            // If a JCloudScaleException was received, rethrow it immediately
            // instead of wrapping it
            Throwable ex = ret.getException();
            if (ex instanceof JCloudScaleException)
                throw (JCloudScaleException) ex;
            else
                throw new JCloudScaleException(ret.getException());
        }

    }

}
