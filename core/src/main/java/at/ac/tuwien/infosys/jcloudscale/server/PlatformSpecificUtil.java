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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.UUID;

/**
 * 
 * This class holds helper methods that do some ugly, platform-specific
 * tasks. Main point to collect them here is that it's easy to change
 * platform-specific behavior if necessary.
 * 
 * @author philipp
 *
 */
public class PlatformSpecificUtil {
	
	/**
	 * This method gets a UUID from the name of a server. Assumption is that
	 * JCloudScale instantiates new dynamic hosts with the UUID as host name.
	 * Obviously, the whole business with '.novalocal' will not work in e.g.,
	 * AWS, so we should improve this at some point.
	 * 
	 * @return The server name as UUID, IF the name is a valid UUID. A random
	 * UUID otherwise.
	 */
	public static UUID tryLookupIdFromNovaName() {
		
		final String awsMetadataUrl = "http://169.254.169.254/latest/meta-data/hostname";
		
		try {
			URL metaUrl = new URL(awsMetadataUrl);
			URLConnection conn = metaUrl.openConnection();
			try(BufferedReader in = new BufferedReader(new InputStreamReader(
	                                    conn.getInputStream())))
	        {
		        String line = in.readLine();
		        
		        if(line == null)
		        	return null;
		        
		        line = line.replace(".novalocal", "");
		        return UUID.fromString(line);
	        }
		} catch(IOException e) {
			return UUID.randomUUID();
		}
		
	}
	
	/**
	 * This method tries to guess the most 'useful' IP address from the list
	 * of interfaces returned by NetworkInterface.getNetworkInterfaces().
	 * 
	 * @return
	 * @throws UnknownHostException
	 * @throws SocketException
	 */
	public static String findBestIP() throws UnknownHostException, SocketException {
		
		InetAddress selectedAddress = null;
		int selectedPriority = 0;

		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while(interfaces.hasMoreElements())
		{
			Enumeration<InetAddress> addresses = interfaces.nextElement().getInetAddresses();
			while(addresses.hasMoreElements())
			{
				InetAddress address = addresses.nextElement();
				if(!(address instanceof Inet4Address))
					continue;
				int addressPriority = getAddressPriority(address);
				if(selectedPriority < addressPriority)
				{
					selectedPriority = addressPriority;
					selectedAddress = address;
				}
			}
		}
		return selectedAddress != null ? selectedAddress.getHostAddress() : "localhost";

	}
	
	  private static int getAddressPriority(InetAddress addr) 
	    {
	        //if it's some bullshit -- 0.
	        if(addr.isLoopbackAddress() ||
	           addr.isAnyLocalAddress() ||
	           addr.isMulticastAddress())
	            return 0;
	        
	        //if it's local due to RFC1918 -- 1
	        byte[] octets = addr.getAddress();
	        if((octets[0] == 10) ||
	           (octets[0] == (byte)172 && octets[1] >= 16 && octets[1] <= 32)||
	           (octets[0] == (byte)192 && octets[1] == (byte)168))
	            return 1;
	        
	        //no, this should be some nice address.
	        return 2;
	    }
	
}
