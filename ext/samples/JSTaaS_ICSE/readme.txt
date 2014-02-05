Brief overview of the code:

JSTaaS evaluation application represents a web-service with 2 possible back-end implementations (cloudscale-based and independent one)
and client that is used to trigger web-service.
Main Client class is at.ac.tuwien.infosys.cloudscale.sample.client.TestClient.java
Main Web-Service class is at.ac.tuwien.infosys.cloudscale.sample.service.JSTaaSServiceRunner.java
Independent worker (used for baseline evaluation, instead of cloudscale) main class is at.ac.tuwien.infosys.cloudscale.sample.service.openstack.worker.OpenstackWorker
 
Cloudscale worker code is located in 2 folders: 
	in "CloudScaleServer_NoCode" is an completely independent cloudscale server that will load application code through remote class loading.
 	in "CloudScaleServer_WithCode" is upgraded version of cloudscale server that has reference to the application code (through maven repository) and, therefore, will not load code through remote class loading.

Our evaluation results are available in "results" folder.

To run the complete evaluation suite, you have the following options:
1) Run everything locally. 
   In this case, you need to do the following:
  -- install and run activemq messaging server with default configuration (http://activemq.apache.org/download.html)
  -- install and run couch db with default configuration (http://couchdb.apache.org/, make sure to have at least version 1.3.X, https://launchpad.net/~nilya/+archive/couchdb-1.3)
  -- ensure that src/main/resources/os.properties file and src/main/resources/META-INF/datastores.xml have localhost in message queue and couch db address fields.
  -- from the project folder(where "pom.xml" file is located), build project with maven (mvn clean package)
  -- start appropriate worker(s):
     -- for non-cloudscale worker, from the project folder, run "mvn exec:exec -Pworker" for each worker you want to start
     -- for cloudscale worker without code:
           - change directory to "CloudScaleServer_NoCode", 
           - ensure that "CloudScaleServerConfig.txt" is pointing to the correct message queue 
		   - run server with "mvn exec:exec"
     -- for cloudscale worker with application code, 
     	   - install application first to the local repository ("mvn clean install" from main project folder), 
     	   - change directory to "CloudScaleServer_WithCode",  
     	   - ensure that "CloudScaleServerConfig.txt" is pointing to the correct message queue 
		   - run server with "mvn exec:exec"
  -- when appropriate amount of workers is started, start server with commands "mvn exec:exec -Pcs" to start CloudScale-aware server and "mvn exec:exec -Pos" to start baseline version.
  -- ensure that server detected appropriate amount of workers (message "..... CloudScale detected X static hosts ...." or "..... Discovered X workers ..... " should appear in the logs) 
  -- run client application with "mvn exec:exec -Pclient" 
  -- results will be available in target/classes/ in a form "eval_platform_X.csv", where X stands for number of hosts used.
  
2) Run application in the cloud.
	In this case, you have to follow these steps:
	-- Build an Communication host image, that will contain activemq and couchdb applications running and accessible from outside.
	-- Build Cloudscale server (with the code reference or without) image. you can use provided code from "CloudScaleServer_X" folders 
	   or follow our manual (http://code.google.com/p/cloudscale/wiki/BuildingServerImages) to build your own image.
	-- Build independent worker image that will have JSTaaS application code and worker service running ("mvn exec:exec -Pworker") on machine startup.
	-- Configure all hosts to be able to discover message queue and couchdb server (see local startup section for details)
	-- from the separate machine (but also within the access of message queue or couch db) run web-service with appropriate back-end implementation 
		("mvn exec:exec -Pcs" to start CloudScale-aware server and "mvn exec:exec -Pos" to start independent from the Cloudscale version)
	-- ensure that appropriate amount of workers is discovered.
	-- from the same machine, start a client application ("mvn exec:exec -Pclient"). 
	   You can start client from different machine as well, but then you have to fix client's code (line 42 of TestClient.java file) from
	   				factory.setAddress("http://localhost:9000/jstaas"); to factory.setAddress("http://<WEB_SERVICE_HOST_NAME>:9000/jstaas");
 	-- results will be available in target/classes/ of the web-service machine in a form "eval_platform_X.csv", where X stands for number of hosts used. 
