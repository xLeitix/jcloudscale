#  Using JCloudScale without Maven

While we encourage users to use [Maven](http://maven.apache.org) for the JCloudScale-based projects, it does not mean that it is impossible to use JCloudScale without Maven. In this short guide we will try to describe the necessary steps you need to perform in order to use JCloudScale without Maven.

## Introduction

As Maven is a project management tool, everything that needs to be performed differently is the way JCloudScale has to be referenced and integrated into your project.

**Note**, that this section of documentation does not reflect the way we perform testing or encourage others to use JCloudScale. Therefore, it may be a bit outdated or expose some issues that do not occur with default approach. Whenever you face such situation, be free to [inform us](https://github.com/xLeitix/jcloudscale/issues) in order to fix  these issues.

## Adding JCloudScale dependency

In order to access JCloudScale functionality or compile the code that references JCloudScale, you need to add JCloudScale jars to your project setup. JCloudScale jar can be obtained directly from [our maven repository](http://www.infosys.tuwien.ac.at/mvn/jcloudscale/jcloudscale.core/), while you still might have difficulties finding jars that JCloudScale depends on. You can either try to figure out all necessary for your particular use case dependencies manually (by adding missing jars as long as you get `ClassNotFoundException`s) or, if you *do* have Maven on your machine, you can run `mvn package` command in the directory with the `pom.xml` file with the following content to collect all dependencies of JCloudScale (some of them might be not necessary for your particular use case, but if you include them all, you will definitely be on the safe side):

    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    	<modelVersion>4.0.0</modelVersion>
    	<groupId>jcloudscale</groupId>
    	<artifactId>jcloudscale.server</artifactId>
    	<version>1.0.0</version>
    	<name>JCloudScale</name>
    	<description>JCloudScale</description>
    	<packaging>pom</packaging>
    	
    	<properties>
    		<!-- Specify desired JCloudScale version here -->
    		<jCloudScaleVersion>0.4.0</jCloudScaleVersion>
    		<!-- Specify the folder to store all dependencies here -->
    		<libraryDirectory>lib</libraryDirectory>
    	</properties>
    	
    	<dependencies>
    		<dependency>
    			<groupId>jcloudscale</groupId>
    			<artifactId>jcloudscale.core</artifactId>
    			<version>${jCloudScaleVersion}</version>
    		</dependency>
    	</dependencies>
    	
    	<repositories>
    		<repository>
    			<id>infosys-repository</id>
    			<url>http://www.infosys.tuwien.ac.at/mvn</url>
    		</repository>
    	</repositories>
    	
    	<build>
    		<plugins>
    			<plugin>
    				<groupId>org.apache.maven.plugins</groupId>
    				<artifactId>maven-dependency-plugin</artifactId>
    				<version>2.5</version>
    				<executions>
    					<execution>
    						<id>copy-dependencies</id>
    						<phase>package</phase>
    						<goals>
    							<goal>copy-dependencies</goal>
    						</goals>
    						<configuration>
    							<outputDirectory>${libraryDirectory}</outputDirectory>
    							<overWriteReleases>false</overWriteReleases>
    							<overWriteSnapshots>false</overWriteSnapshots>
    							<overWriteIfNewer>true</overWriteIfNewer>
    						</configuration>
    					</execution>
    				</executions>
    			</plugin>
    		</plugins>
    	</build>
    </project>
	
## Applying AspectJ Aspects

As JCloudScale seamless integration depends on [AspectJ](http://www.eclipse.org/aspectj/), we need to apply AspectJ aspects defined by JCloudScale to your project in order to achieve the same features as default Maven-based setup provides. Of course, if you don't plan to use annotation-based features of JCloudScale and plan using [JCloudScale API](Documentation.md#jcloudscale-api), you can skip this step.

AcpectJ provides 3 types of aspect weaving:
* [compile-time weaving](https://www.eclipse.org/aspectj/doc/next/devguide/ajc-ref.html) - compile either target source or aspect classes via dedicated AspectJ compiler;
* [post-compile weaving](https://www.eclipse.org/aspectj/doc/next/devguide/ajc-ref.html) - inject aspect instructions to already compiled classes;
* [load-time weaving](https://www.eclipse.org/aspectj/doc/next/devguide/ltw.html) - inject aspect instructions to the byte code during class loading, i.e. load instrumented class instead of the "raw" one;

**Note**, If you are using any IDE, it may have plugins or embedded features that simplify AspectJ usage (e.g., [AJDT](https://www.eclipse.org/ajdt/) for Eclipse, or [AspectJ](https://www.jetbrains.com/idea/webhelp/aspectj.html) support in IntelliJ IDEA). In this manual we briefly describe raw AspectJ usage independently from any IDE. 



### Compile-time weaving

Compile-time weaving requires source code of the application to be compiled by [AspectJ Compiler(ajc)](https://eclipse.org/aspectj/downloads.php) instead of default `javac` compiler (assuming `ajc` is in PATH and [aspectjrt.jar](http://repo1.maven.org/maven2/org/aspectj/aspectjrt/1.7.0/aspectjrt-1.7.0.jar) is in CLASSPATH):

    ajc -aspectpath jcloudscale.core-0.4.0.jar -classpath <your classpath here> -1.7  -sourceroots <source folder here> -outjar <compiled jar here>

for example:

    ajc -aspectpath lib/jcloudscale.core-0.4.0.jar -classpath "lib/*" -1.7 -sourceroots test -outjar code.jar
	
After this, application can be started from the resulting jar as usually:

    java -cp<your classpath here> <specify your main class here>
	
for example:

    java -cp code.jar;lib/* test.Main 

### Post-compile weaving

Post-compile weaving gives you more freedom to compile your source files the way it is usually done, while introduces another step after compilation and before application running. Post-compile weaving is porformed by the same `ajc` utility as compile-time, but with a slightly different set of parameters:

     ajc -aspectpath jcloudscale.core-0.4.0.jar -classpath <your classpath here> -1.7  -inpath <your compiled classes or jars here> -outjar <result jar here>

for example:

    ajc -aspectpath lib/jcloudscale.core-0.4.0.jar -classpath "lib/*" -1.7  -inpath target -outjar code.jar
	
After this, application can be started from the resulting jar as usually:

    java -cp<your classpath here> <specify your main class here>

### Load-time weaving

Load-time weaving does not require anything specific from the code compilation stage. This allows working with code in any IDE or environment without any limitations. However, load-time weaving applies some run-time application code processing and changing, therefore influencing application performance, what might be critical.

In order to enable AspectJ runtime weaving, we need to provide an `aop.xml` file, located in `META-INF` folder in classpath. This file for aspects defined in JCloudScale may have the following content:

    <aspectj>
    	<aspects>
    	  <aspect name="at.ac.tuwien.infosys.jcloudscale.aspects.CloudObjectAspect"/>
    	  <aspect name="at.ac.tuwien.infosys.jcloudscale.aspects.JCloudScaleManagementAspect"/>
    	</aspects>

    	<weaver options="-verbose">
    		<!-- Ignore all classes within -->
    		<exclude within="javax..*"/>
    		<exclude within="java..*"/>
    		<exclude within="org.aspectj..*"/>
    		<exclude within="at.ac.tuwien.infosys.jcloudscale..*"/>
    		
    		<!-- Process classes within (place your packages instead of 'test')-->
    		<include within="test..*"/>
    	</weaver>
    </aspectj>

Additionally, you need an [aspectj weaver](http://repo1.maven.org/maven2/org/aspectj/aspectjweaver/1.7.0/aspectjweaver-1.7.0.jar), that provides necessary functionality for AspectJ runtime weaving. Finally, we need to add `-javaagent` JVM option to application startup:

    java -javaagent:aspectjweaver.jar -cp<specify your classpath here> <specify your main class here>
	
After you run this command, your application should run with JCloudScale aspects applied.