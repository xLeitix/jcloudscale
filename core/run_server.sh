#! /bin/bash

if [ ! -d "logs" ]; then
	mkdir logs
fi

if [ -z "$JAVA_HOME" ]; then
	# set JAVA_HOME if we are on our Jenkins server
	JAVA_HOME = "/var/lib/jenkins/tools/JDK/Java_7" 
fi

java -Djava.library.path=/home/ubuntu/CloudScale/core/target/classes -Djava.util.logging.config.file=META-INF/logging_server.properties -cp target/*:target/lib/* at.ac.tuwien.infosys.jcloudscale.server.JCloudScaleServerRunner $1 $2 >> logs/output.log 2> logs/error.log 
