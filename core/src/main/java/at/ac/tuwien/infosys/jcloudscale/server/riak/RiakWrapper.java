package at.ac.tuwien.infosys.jcloudscale.server.riak;
///*
//   Copyright 2013 Philipp Leitner
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//*/
//package at.ac.tuwien.infosys.jcloudscale.server.riak;
//
//import java.io.Closeable;
//import java.io.IOException;
//import java.util.logging.Logger;
//
//import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
//import at.ac.tuwien.infosys.jcloudscale.exception.JCloudScaleException;
//import at.ac.tuwien.infosys.jcloudscale.utility.SerializationUtil;
//
//import com.basho.riak.client.IRiakClient;
//import com.basho.riak.client.IRiakObject;
//import com.basho.riak.client.RiakException;
//import com.basho.riak.client.RiakFactory;
//import com.basho.riak.client.RiakRetryFailedException;
//import com.basho.riak.client.bucket.Bucket;
//import com.basho.riak.client.cap.UnresolvedConflictException;
//import com.basho.riak.client.convert.ConversionException;
//
//public class RiakWrapper implements Closeable
//{
//	private static volatile RiakWrapper instance = null;
//	
//	IRiakClient riakClient = null;
//	private volatile Bucket mainBucket;
//	
//	private Logger log = null;
//	private RiakConfiguration config;
//
//	private RiakWrapper(RiakConfiguration config)
//	{
//		this.config = config;
//		this.log = JCloudScaleConfiguration.getLogger(this);
//
//		String riakIP;
//		try 
//		{
//			riakIP = config.getServerAddress();
//			log.info("Using riak server at "+riakIP);
//		} 
//		catch (IOException e) 
//		{
//			log.severe("Failed to discover riak server, IOException occured: "+e);
//			throw new JCloudScaleException(e, "Failed to discover riak server.");
//		}
//		
//		if (riakIP != null)
//		{
//			this.log.info("Using Riak server with IP " + riakIP +" over "+(config.useHttpProtocol() ? "HTTP" : "Protocol Buffers"));
//			try
//			{
//			this.riakClient = config.useHttpProtocol() ? 
//					RiakFactory.httpClient("http://" + riakIP + ":"+config.getServerPort()+"/riak") : 
//					RiakFactory.pbcClient(riakIP, config.getServerPort());
//			}
//			catch(RiakException ex)
//			{
//				throw new JCloudScaleException(ex, "Failed to connect to Riak server.");
//			}
//		} 
//		else
//		{
//			this.log.severe("No Riak server discovered!");
//			throw new JCloudScaleException("Riak server IP not specified and no Riak server discovered!");
//		}
//	}
//
//	static synchronized RiakWrapper getInstance(RiakConfiguration config)
//	{
//		if (instance == null)
//			instance = new RiakWrapper(config);
//		return instance;
//	}
//	
//	static synchronized void closeInstance()
//	{
//		if(instance != null)
//		{
//			instance.close();
//			instance = null;
//		}
//	}
//
//	public void setValue(String name, String fieldName, String value)
//	{
//		if (this.riakClient != null)
//		{
//			String jointName = getJointName(name, fieldName);
//			
//			this.log.fine("Setting value to Riak key riak/"+this.config.bucketName()+"/" + jointName);
//
//			try 
//			{
//				getRiakBucket().store(jointName, value).execute();
//			} 
//			catch (RiakRetryFailedException | UnresolvedConflictException | ConversionException e) 
//			{
//				throw new JCloudScaleException(e, "Failed to set value to the riak key "+jointName);
//			}
//		}
//	}
//	
//	public void setValue(String name, String fieldName, Object value) throws IOException
//	{
//		if (this.riakClient != null)
//		{
//			String jointName = getJointName(name, fieldName);
//			
//			this.log.fine("Setting value to Riak key riak/"+this.config.bucketName()+"/" + jointName);
//			
//			byte[] serializedValue = SerializationUtil.serializeToByteArray(value);
//			
//			try 
//			{
//					getRiakBucket()
//					.store(jointName, serializedValue).execute();
//			} 
//			catch (RiakRetryFailedException | UnresolvedConflictException | ConversionException e) 
//			{
//				throw new JCloudScaleException(e, "Failed to set value to the riak key "+jointName);
//			}
//		}
//	}
//	
//	public boolean containsValue(String name, String fieldName)
//	{
//		if (this.riakClient != null)
//		{
//			String jointName = getJointName(name, fieldName);
//			this.log.fine("Fetching value from Riak key riak/"+this.config.bucketName()+"/" + jointName);
//			
//			try 
//			{
//				return getRiakBucket().fetch(jointName).execute() != null;
//			} 
//			catch (UnresolvedConflictException | RiakRetryFailedException	| ConversionException e) 
//			{
//				throw new JCloudScaleException(e, "Failed to fetch riak key "+jointName);
//			}
//		}
//		else
//			return false;
//	}
//	
//	public <T> T getValue(String name, String fieldName, Class<T> clazz) throws IOException
//	{
//		return getValue(name, fieldName, clazz.getClassLoader(), clazz);
//	}
//	
//	public <T> T getValue(String name, String fieldName, ClassLoader classloader, Class<T> clazz) throws IOException
//	{
//		if (this.riakClient != null)
//		{
//			String jointName = getJointName(name, fieldName);
//			this.log.fine("Fetching value from Riak key riak/"+this.config.bucketName()+"/" + jointName);
//			
//			IRiakObject riakObject = null;
//			try 
//			{
//				riakObject = getRiakBucket().fetch(jointName).execute();
//			} catch (UnresolvedConflictException | RiakRetryFailedException	| ConversionException e) 
//			{
//				throw new JCloudScaleException(e, "Failed to fetch riak key "+jointName);
//			}
//			
//			//if there's no such key, here is null.
//			if(riakObject == null)
//				return null;
//			
//			// for now we have special treatment for strings only.
//			if(clazz.equals(String.class))
//				return (T)riakObject.getValueAsString();
//			
//			byte[] serializedVal = riakObject.getValue();
//			try {
//				return (T)SerializationUtil.getObjectFromBytes(serializedVal, classloader);
//			} catch (ClassNotFoundException e) 
//			{//this should never happen as we're giving the class itself.
//				throw new JCloudScaleException(e, "Failed to deserialize object from riak key "+jointName+" with the provided classloader.");
//			}
//		} 
//		else
//		{
//			return null;
//		}
//
//	}
//	
//	public boolean deleteValue(String name, String fieldName)
//	{
//		if(this.riakClient != null)
//		{
//			String jointName = getJointName(name, fieldName);
//			
//			this.log.fine("Deleting key riak/"+this.config.bucketName()+"/" + jointName+" from Riak.");
//
//			try 
//			{
//				getRiakBucket().delete(jointName).execute();
//			} 
//			catch (RiakException e) 
//			{
//				throw new JCloudScaleException(e, "Failed to detele key "+jointName+" from Riak server.");
//			}
//			
//			return true;
//		}
//		else
//			return false;
//	}
//	
//	// just a temp method to clean up database. removes everything in our bucket.
//	public void cleanDatabase()
//	{
//		try 
//		{
//			int count = 0;
//			Bucket bucket = getRiakBucket();
//			
//			if(bucket == null)
//			{
//				this.log.info("Cleanup requested, but jcloudscale bucket is not present.");
//				return;
//			}
//			
//			for(String key : bucket.keys())
//			{
//				bucket.delete(key).execute();
//				count++;
//			}
//			
//			this.log.info("RiakWrapper: Done. "+count+" keys deleted.");
//		} catch (RiakException e) 
//		{
//		}
//	}
//
//	@Override
//	public synchronized void close()
//	{
//		if(this.riakClient != null)
//		{
//			this.mainBucket = null;
//			this.riakClient.shutdown();
//			this.riakClient = null;
//		}
//	}
//
//	//---------------------------------------
//	
//	private Bucket getRiakBucket()
//	{
//		if(mainBucket == null)
//			synchronized (this) 
//			{
//				if(mainBucket == null)
//					try {
//						mainBucket = this.riakClient.fetchBucket(this.config.bucketName()).execute();
//					} catch (RiakRetryFailedException e) 
//					{
//						throw new JCloudScaleException(e, "Failed to fetch bucket from Riak server.");
//					}
//			}
//		
//		return mainBucket;
//	}
//	
//	private String getJointName(String name, String fieldName)
//	{
//		return name + "." + fieldName;
//	}
//}
