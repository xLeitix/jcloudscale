#! /bin/bash

if [ ! -d "logs" ]; then
	mkdir logs
fi

if [ -z "$JAVA_HOME" ]; then
	# set JAVA_HOME if we are on our Jenkins server
	JAVA_HOME = "/var/lib/jenkins/tools/JDK/Java_7" 
fi

$JAVA_HOME/bin/java -Djava.util.logging.config.file=META-INF/logging_server.properties -cp ../../core/target/jcloudscale.core-0.0.1.jar:../../core/target/lib/* at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServerRunner $1 $2 >> logs/output.log 2> logs/error.log 
