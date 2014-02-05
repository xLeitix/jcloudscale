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
package at.ac.tuwien.infosys.jcloudscale.sample.service.openstack;

import java.io.Closeable;

import org.lightcouch.CouchDbClient;
import org.lightcouch.NoDocumentException;
import org.lightcouch.Response;

import at.ac.tuwien.infosys.jcloudscale.sample.service.dto.TestResults;

/**
 * Couch database helper that allows to work with the database independently from jcloudscale
 */
public class CouchDbHelper implements Closeable 
{
	private CouchDbClient dbClient; //may need closing at some point.
	
	public CouchDbHelper(String host, int port, String dbName)
	{
		//
		// Initializing database
		//
//		final String dbName = "testresults";
		final boolean createIfNotExist = true;
		final String protocol = "http";
//		final String host = OpenstackConfiguration.getCouchdbHostname();
//		final int port = 5984;
		final String username = null;
		final String password = null;
		
		dbClient = new CouchDbClient(dbName, createIfNotExist, protocol, host, port, username, password);
	}

	@Override
	public void close()
	{
		if(dbClient != null)
		{
			dbClient.shutdown();
			dbClient = null;
		}
	}
	
	public String save(TestResults object)
	{
		Response response = dbClient.save(object);
		return response.getId();
	}
	
	public TestResults load(String testsId) 
	{
		try
		{
			return dbClient.find(TestResults.class, testsId);
		}
		catch(NoDocumentException ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	
	public void delete(TestResults testResults)
	{
		dbClient.remove(testResults);
	}

	public void update(TestResults results) 
	{
		dbClient.update(results);
	}
}
