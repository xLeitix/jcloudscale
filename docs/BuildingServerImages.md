# Introduction

JCloudScale uses custom server images to scale up and down. These images are relatively vanilla Linux-based servers running a small JCloudScale service. The following steps explain how to build such an image for your cloud. The discussion has an [Openstack](http://www.openstack.org/) private cloud in mind, but the same steps can also be applied to [Amazon EC2](http://aws.amazon.com/ec2/). 

# Building your Image

1. Start off by launching a new instance in your cloud. This instance will serve as the blueprint of our JCloudScale image. The size of the instance does not matter, but the operating system should be Linux-based. The following discussion will assume that you are using Ubuntu 12.04, 12.10 or 13.04. Other Linux-based distributions will work as well, but you may need to use different distro-specific mechanisms to e.g., install the JCloudScale service. We assume that you are logged into the server using a user named `ubuntu`.
1. Install Maven 3 and Java 7 on this host, e.g., run `sudo apt-get update; sudo apt-get install openjdk-7-jdk maven`
1. Create a new directory `mkdir /home/ubuntu/JCloudScaleService`. In this directory, add the following `pom.xml` Maven POM file:

        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
            <artifactId>jcloudscale.server</artifactId>
            <name>JCloudScale Server Component</name>
            <description>JCloudScale Server Component</description>
            <groupId>jcloudscale</groupId>
            <version>0.4.0</version>
            <repositories>
               <repository>
                       <id>infosys-repository</id>
                       <url>http://www.infosys.tuwien.ac.at/mvn</url>
               </repository>
            </repositories>
            <dependencies>
                 <dependency>
                    <groupId>jcloudscale</groupId>
                    <artifactId>jcloudscale.core</artifactId>
                    <version>0.4.0</version>
                 </dependency>
                 <dependency>
                    <groupId>jcloudscale</groupId>
                    <artifactId>jcloudscale.datastorelib</artifactId>
                    <version>0.4.0</version>
                 </dependency>
            </dependencies>
            <build>
                 <plugins>
                           <plugin>
                                <groupId>org.codehaus.mojo</groupId>
                                <artifactId>exec-maven-plugin</artifactId>
                                <executions>
                                     <execution>
                                          <goals>
                                               <goal>exec</goal>
                                          </goals>
                                     </execution>
                                </executions>
                                <configuration>
                                     <executable>java</executable>
                                     <arguments>
                                          <argument>-classpath</argument>
                                          <classpath />
                                       <argument>at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServerRunner</argument>
                                     </arguments>
                                </configuration>
                           </plugin>
                      </plugins>
            </build>
        </project>

1. This POM file will load version `0.4.0` from the Infosys Maven repository (as well as all dependencies of JCloudScale) and start the server process. Test the build file via `mvn exec:exec` from the same directory as the POM file. You should see plenty of dependencies being downloaded and the service being started. When the service comes up successfully, you will not see any output.
1. Now we need to create a Linux startup service that uses this Maven directive. For Ubuntu, create a file `/etc/init/jcloudscale.conf` with the following content:
       
        # jcloudscaleService - jcloudscale job file
        
        description "JCloudScale service"
        author "Philipp Leitner <leitner@infosys.tuwien.ac.at>"
        
        # Stanzas
        #
        # Stanzas control when and how a process is started and stopped
        # See a list of stanzas here: http://upstart.ubuntu.com/wiki/Stanzas#respawn 
        
        # When to start the service
        start on runlevel [2345]
        
        # When to stop the service
        stop on runlevel [016]
        
        # Automatically restart process if crashed
        # respawn
        
        # Essentially lets upstart know the process will detach itself to the background
        # expect fork
        
        # Start the process
        exec /home/ubuntu/jcloudscale.sh

1. Clearly, we also need to create the shell script `/home/ubuntu/jcloudscale.sh` that we mentioned in our service job file.  This file should have the following content:

       
        #! /bin/bash
        
        cd /home/ubuntu/JCloudScaleService
        mvn exec:exec

1. Make the script executable (`chmod +x /home/ubuntu/jcloudscale.sh`) and give the new service a test run (`sudo service jcloudscale start`). Verify that the service is indeed running in the background, for instance by checking via `ps aux | grep java`.
1. The image is done. Now we only need to snapshot our blueprint server and give the new image / snapshot a name that follows the naming conventions of JCloudScale. Optimally, you will want to name the image `JCloudScale_v[VERSION]`, where `[VERSION]` is the version of the source code as indicated in `JCloudScaleConfiguration.CS_VERSION`. You can also give another name to the image, but then you need to specify the name of the image in your JCloudScale configuration.