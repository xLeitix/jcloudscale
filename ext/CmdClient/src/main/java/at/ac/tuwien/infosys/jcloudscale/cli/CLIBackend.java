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
package at.ac.tuwien.infosys.jcloudscale.cli;

import java.io.IOException;
import java.util.UUID;

import javax.jms.JMSException;
import javax.naming.NamingException;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.Shell;
import asg.cliche.ShellDependent;
import asg.cliche.ShellFactory;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.management.CloudscaleUIConfiguration;
import at.ac.tuwien.infosys.jcloudscale.messaging.IMQWrapper;
import at.ac.tuwien.infosys.jcloudscale.messaging.InvalidMQWrapperStateException;
import at.ac.tuwien.infosys.jcloudscale.messaging.TimeoutException;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.CreateServerRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.CreateServerResponse;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.DestroyServerRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.GetMetricRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.GetMetricResponse;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ListCloudObjectsRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ListCloudObjectsResponse;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ListServersRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ListServersResponse;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ServerDetailsRequest;
import at.ac.tuwien.infosys.jcloudscale.messaging.objects.ui.ServerDetailsResponse;

public class CLIBackend implements ShellDependent 
{
	private Shell theShell;
	private CLIDemoClientBackend demoBackend = new CLIDemoClientBackend();
	
	private IMQWrapper mqWrapper;
	private static UUID clientId = UUID.randomUUID(); 
	private CloudscaleUIConfiguration cfg;

	public CLIBackend() throws NamingException, JMSException
	{
		this.cfg = CLI.getConfiguration().common().UI();
	}
	
	@Command(name="connect", abbrev="con")
	public void connectToMQ(
		@Param(name="mq_address") String address
		) throws NamingException, JMSException 
	{
		if(mqWrapper != null)
			mqWrapper.close();

		CLI.getConfiguration().common().communication().setServerAddress(address);
		
		mqWrapper = JCloudScaleConfiguration.createMQWrapper(CLI.getConfiguration());
		mqWrapper.createQueueProducer(cfg.getMessageQueue());
		mqWrapper.createTopicConsumer(cfg.getMessageQueue(), "JMSCorrelationID = '"+clientId.toString()+"'");
	}
	
	@Command(name="disconnect", abbrev="discon")
	public void disconnect() throws JMSException, NamingException 
	{
		mqWrapper.disconnect();
	}
	
	@Command(name="set-command-timeout", abbrev="timeout")
	public void setCommandTimeout(@Param(name = "timeout") long timeout) throws JMSException, NamingException 
	{
		CLI.getConfiguration().common().communication().setRequestTimeout(timeout);
	}
	
	@Command(name="list-ch", abbrev="ls-ch")
	public void listServers() throws JMSException {
		
		MessageObject response = null;
		
		try {
			response = mqWrapper.requestResponse(new ListServersRequest(), clientId);
		} catch(TimeoutException e) {
			handleTimeout(e);
		} catch(InvalidMQWrapperStateException e) {
			handleWrapperException(e);
		}
		
		displayResult(response, ListServersResponse.class);
		
	}
	
	@Command(name="list-co", abbrev="ls-co")
	public void listCloudObjects(
		@Param(name="co_type") String fqType
		) throws ClassNotFoundException, JMSException {
		
		ListCloudObjectsRequest request = new ListCloudObjectsRequest();
		if(fqType != null) {
			request.setCoType(fqType);
		}
		
		MessageObject response = null;
		try {
			response = mqWrapper.requestResponse(request, clientId);
		} catch(TimeoutException e) {
			handleTimeout(e);
		} catch(InvalidMQWrapperStateException e) {
			handleWrapperException(e);
		}
		
		displayResult(response, ListCloudObjectsResponse.class);
		
	}
	
	@Command(name="list-co", abbrev="ls-co")
	public void listCloudObjects() throws ClassNotFoundException, JMSException {
		
		listCloudObjects(null);
		
	}
	
	@Command(name="ch-details", abbrev="ch")
	public void giveDetailsToCH(@Param(name="server_id") String id) throws JMSException {
		
		ServerDetailsRequest request = new ServerDetailsRequest();
		request.setServerId(UUID.fromString(id));
		
		MessageObject response = null;
		try {
			response = mqWrapper.requestResponse(request, clientId);
		} catch(TimeoutException e) {
			handleTimeout(e);
		} catch(InvalidMQWrapperStateException e) {
			handleWrapperException(e);
		}
		
		displayResult(response, ServerDetailsResponse.class);
		
	}
	
	@Command(name="get-monitoring-metric", abbrev="get-metric")
	public void getValueToMetric(@Param(name="metric_name") String metric) throws JMSException {
		
		GetMetricRequest request = new GetMetricRequest();
		request.setMetricName(metric);
		
		MessageObject response = null;
		try {
			response = mqWrapper.requestResponse(request, clientId);
		} catch(TimeoutException e) {
			handleTimeout(e);
		} catch(InvalidMQWrapperStateException e) {
			handleWrapperException(e);
		}
		
		displayResult(response, GetMetricResponse.class);
		
	}
	
	@Command(name="add-ch", abbrev="add-ch")
	public void addNewHost() throws JMSException {
		
		CreateServerRequest request = new CreateServerRequest();
		MessageObject response = null;
		try {
			response = mqWrapper.requestResponse(request, clientId);
		} catch(TimeoutException e) {
			handleTimeout(e);
		} catch(InvalidMQWrapperStateException e) {
			handleWrapperException(e);
		}
		
		displayResult(response, CreateServerResponse.class);
		
	}
	
	@Command(name="destroy-ch", abbrev="del-ch")
	public void destroyHost(@Param(name="server_id") String id) throws JMSException {
		
		DestroyServerRequest request = new DestroyServerRequest();
		request.setServerId(UUID.fromString(id));
		MessageObject response = null;
		try {
			response = mqWrapper.requestResponse(request, clientId);
		} catch(TimeoutException e) {
			handleTimeout(e);
		} catch(InvalidMQWrapperStateException e) {
			handleWrapperException(e);
		}
		
		displayResult(response, ListServersResponse.class);
		
	}
	
	@Command(name="destroy-co", abbrev="del-co")
	public void destructCloudObject() {
		throw new JCloudScaleCLIException("Not implemented");
	}

	@Command(name="demo-mode", abbrev="demo")
	public void enableDemoMode() throws IOException {
		
        ShellFactory.createSubshell("demo", theShell, "... going into demo mode ...", demoBackend)
                .commandLoop();
		
	}
	
    @Override
	public void cliSetShell(Shell theShell) {
        this.theShell = theShell;
    }
	
	private void displayResult(MessageObject response,
			Class<? extends MessageObject> expectedType) {
		
		if(response == null)
			return;
		
		if(expectedType.isInstance(response)) {
			System.out.println(response);
		} else {
			System.out.println("Internal error, received unexpected object from server");
		}
		
	}
	
	private void handleTimeout(TimeoutException e) {
		
		System.out.println("Received no answer from JCloudScale - maybe no JCloudScale instance is running and/or using this MQ?");
		
	}
	
	private void handleWrapperException(InvalidMQWrapperStateException e) {
		
		System.out.println("Received JMS Exception - maybe you are not connected to any MQ?");
		
	}
	
}
