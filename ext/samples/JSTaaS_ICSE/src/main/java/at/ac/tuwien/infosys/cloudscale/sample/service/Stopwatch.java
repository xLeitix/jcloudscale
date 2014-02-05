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
package at.ac.tuwien.infosys.jcloudscale.sample.service;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.io.Files;

/**
 * Provides a uniform and comfortable way to measure duration of the operation.
 */
public class Stopwatch implements Closeable, Comparable<Stopwatch>
{
	private String filename = null;
	private String message = null;
	private List<Stopwatch> children = new ArrayList<Stopwatch>();
	private long startTime = System.nanoTime();
	private long totalTime = -1;
	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
	
	private Object syncRoot;
	
	/**
	 * Creates a new stopwatch instance and starts time measurement.
	 * After <b>close</b> method is called, results will be written to the specified file.
	 * @param filename The name of the file where results should be written.
	 */
	public Stopwatch(String filename)
	{
		this.filename = filename;
		syncRoot = new Object();
	}
	
	/**
	 * Creates a new instance of the stopwatch with parent stopwatch specified. 
	 * Can be used to measure partial execution within some larger time span.
	 * @param parent The parent stopwatch that sets the boundaries and defines the output of this instance.
	 */
	public Stopwatch(Stopwatch parent)
	{
		parent.children.add(this);
		this.syncRoot = parent.syncRoot;
	}
	
	/**
	 * Defines the test message that defines this instance measurement.
	 */
	public Stopwatch withMessage(String message)
	{
		this.message = message;
		return this;
	}
	
	/**
	 * Defines the precision that should be used for result output.
	 * Default precision is <b>TimeUnit.MILLISECONDS</b>
	 */
	public Stopwatch withPrecision(TimeUnit timeUnit)
	{
		this.timeUnit = timeUnit;
		return this;
	}
	
	/**
	 * Defines a syncRoot to synchronize access to file between multiple Stopwatch instances.
	 * @param syncRoot
	 * @return
	 */
	public Stopwatch withSyncRoot(Object syncRoot)
	{
		this.syncRoot = syncRoot;
		return this;
	}
	
	/**
	 * Allows to determine if this instance of stopwatch is running.
	 */
	public boolean isRunning()
	{
		return totalTime < 0;
	}
	
	/**
	 * Allows to determine the duration measured by this stopwatch with specified precision. 
	 * If the stopwatch is still running, 0 will be returned.  
	 * @return
	 */
	public long duration()
	{
		if(isRunning())
			return 0;
		
		return timeUnit.convert(totalTime, TimeUnit.NANOSECONDS);
	}
	
	@Override
	public int compareTo(Stopwatch that) 
	{
		if(that == null)
			return 1;
		
		if(this.message == null)
		{
			if(that.message == null)
				return 0;
			else 
				return -1;
		}
		
		if(that.message == null)
			return 1;
		
		if(this.message.equals(that.message))
			return 0;
		
		return this.message.compareTo(that.message);
	}
	
	@Override
	public void close()
	{
		if(isRunning())
		{
			totalTime = System.nanoTime() - startTime;
			
			for(Stopwatch child : children)
				child.close();
			
			// sorting child stopwatches.
			Collections.sort(children);
		
			if(filename != null)
			{
				try
				{
					synchronized (syncRoot) 
					{
						// creating folders
						File file = new File(filename).getCanonicalFile();
						if(!file.exists())
							file.getParentFile().mkdirs();
						
						// writing header
						if(file.length() == 0L)
							writeHeader(file);
						
						// writing data
						try(FileWriter writer = new FileWriter(file, true))
						{
							writer.write(Long.toString(duration()));
							for(Stopwatch sw : children)
							{
								writer.write(", ");
								writer.write(Long.toString(sw.duration()));
							}
							
							writer.append(System.lineSeparator());
						}
					}
				}
				catch(IOException ex)
				{
					ex.printStackTrace();
				}
			}
		}
	}

	private void writeHeader(File file) throws FileNotFoundException, IOException 
	{
		try(BufferedWriter writer = Files.newWriter(file, Charset.defaultCharset()))
		{
			writer.write(message);
			
			for(Stopwatch sw : children)
			{
				writer.write(", ");
				writer.write(sw.message == null ? "NULL" : sw.message);
			}
			
			writer.append(System.lineSeparator());
		}
	}

}
