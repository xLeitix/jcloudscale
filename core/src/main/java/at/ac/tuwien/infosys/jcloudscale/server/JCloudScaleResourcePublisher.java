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

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfigurationBuilder;
import at.ac.tuwien.infosys.jcloudscale.messaging.MessageQueueConfiguration;
import at.ac.tuwien.infosys.jcloudscale.vm.JCloudScaleClient;

public class JCloudScaleResourcePublisher {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 1)
        {
            showHelp();
            return;
        }

        //
        // detecting parameters.
        //
        String service = args[0].toLowerCase().trim();
        String address = PlatformSpecificUtil.findBestIP();
        if(args.length > 1)
            address = args[1];

        int port = 0;
        if(args.length > 2)
        {
            try
            {
                port = Integer.parseInt(args[2]);
            }
            catch(NumberFormatException ex)
            {
                System.out.println("ERROR: Failed to parse port: "+args[2]);
                showHelp();
                return;
            }
        }

        //
        // Starting services.
        //
        switch(service)
        {
        case "mq":
            startMq(address, port);
            break;
            //		case "riak":
            //			startRiak(address, port);
            //			break;

        default:
            showHelp();
        }
    }

    private static void startMq(String hostname, int port) throws IOException
    {
        JCloudScaleConfiguration cfg = new JCloudScaleConfigurationBuilder()
        .withLogging(Level.INFO)
        .build();
        JCloudScaleClient.setConfiguration(cfg);

        MessageQueueConfiguration mqConfig = cfg.common().communication();
        mqConfig.setServerAddress(hostname);
        if(port > 0)
            mqConfig.setServerPort(port);

        try(Closeable serverPublisher = mqConfig.createServerPublisher())
        {
            System.out.println("Started publishing MQ connection information: ("+hostname+":"+ (port>0 ? port : "<default-port>")+").");
            waitInterruption(serverPublisher);
        }
        finally
        {
            System.out.println("Execution finished");
        }
    }

    public static volatile boolean isFinished = false;//we never finish this cycle, but this hides FindBugs message
    private static void waitInterruption(Object runningObject)
    {
        try
        {
            synchronized (runningObject)
            {
                while(isFinished)
                    runningObject.wait();
            }
        } catch (InterruptedException e)
        {
        }
    }


    private static void showHelp()
    {
        System.out.println("USAGE:");
        System.out.println(" MQ|RIAK [ip-address [port]]");
        System.out.println(" EXAMPLE:");
        System.out.println("\tmq");
        System.out.println("\tmq localhost");
        System.out.println("\tmq localhost 61616");
        System.out.println("\triak 10.1.1.1");
        System.out.println("\triak 10.1.1.1 5555");
    }

}
