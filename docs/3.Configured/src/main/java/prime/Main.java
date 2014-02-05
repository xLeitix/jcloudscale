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
package prime;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import prime.scheduler.ThreadPoolScheduler;
import prime.scheduler.splitter.LinearSplitter;
import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleConfigurationProvider;
import at.ac.tuwien.infosys.jcloudscale.annotations.JCloudScaleShutdown;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;
import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfigurationBuilder;
import at.ac.tuwien.infosys.jcloudscale.vm.localVm.LocalCloudPlatformConfiguration;

public class Main {
	
	// some examples of configuration usage and specification.
//	static
//	{
//		JCloudScaleConfiguration config = new JCloudScaleConfigurationBuilder().build();
//		
//		config.common().clientLogging().setDefaultLoggingLevel(Level.OFF);
//		config.server().logging().setDefaultLoggingLevel(Level.OFF);
//		
//		try
//		{
//			config.save(new File("config.xml"));
//			
//			config = JCloudScaleConfiguration.load(new File("config.xml"));
//		}
//		catch(IOException ex)
//		{
//			ex.printStackTrace();
//		}
//
//		JCloudScaleClient.setConfiguration(config);
//	}

	/**
	 * Entrance point of the application.
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@JCloudScaleShutdown
	public static void main(String[] args)
	{
		System.out.println("Starting...");
		long start = System.nanoTime();
		
		// input parameters.
		final int threadCount = 4;
		final int from = 1;
		final int to = Integer.MAX_VALUE/100;
		
		System.out.println(String.format("Searching prime numbers between %s and %s in %s threads.", from, to, threadCount));
		
		ThreadPoolScheduler scheduler = new ThreadPoolScheduler(threadCount, new LinearSplitter());
		System.out.println(String.format("Execution finished. %s prime numbers found.", scheduler.search(from, to)));
		
		long elapsed = (System.nanoTime() - start)/1000000;
		
		long min = elapsed /(60*1000);
		long sec = (elapsed % (60*1000))/1000;
		long msec = elapsed % 1000;
		System.out.println(String.format("Elapsed: %02d:%02d.%03d", min, sec, msec));
	}

	// this method is used to specify configuration for JCloudScale.
	@JCloudScaleConfigurationProvider
	public static JCloudScaleConfiguration getConfiguration()
	{
		return new JCloudScaleConfigurationBuilder(
		            new LocalCloudPlatformConfiguration()
		            // this forces CloudScale servers to use different classpath from our main application,
		            // what brings local testing nearer to production work with remote cloud hosts.
		            .withClasspath(prepareClasspath())
		            // this makes CloudScale start server in temp directory, therefore ensuring that
		            // any file-dependency issues are solved during local testing.
		            .withStartupDirectory(System.getProperty("java.io.tmpdir"))
		                        )
		                .with(new MyScalingPolicy())
		                .withLogging(Level.SEVERE)
				.build();
	}

    /**
     * This method parses classpath of current application and 
     * constructs classpath without source code of current application. 
     */
    private static String prepareClasspath() 
    {
        // splitting elements of classpath
        String[] elems = System.getProperty("java.class.path").split(File.pathSeparator);
        
        // retrieving our code location
        String myCodeLocation = getClassLocation(Main.class);

        // building new classpath without our code
        StringBuilder builder = new StringBuilder();
        for(int i=0;i<elems.length;++i)
        {
            // if there are some other libraries that we want to load through remoteclassloading,
            // we should skip them as well.
            if(!elems[i].equals(myCodeLocation))
            {
                builder.append(elems[i]);
                builder.append(File.pathSeparator);
            }
        }
        builder.deleteCharAt(builder.length() - 1);//last separator was actually not required.
        
        return builder.toString();
    }

    /**
     * Gets location of specified class in system.
     * @return
     */
    private static String getClassLocation(Class<?> cls) 
    {
        return new File(cls.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
    }
}
