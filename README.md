# Distributed Systems

## Motivation
Sky require a system that will integrate with an existing upstream. This upstream will want to call CMS to get channel-metadata, containing streaming metadata critical with provisioning existing and new channels for streaming propositions globally. The following system is designed to be highly avaialable, scalable and resillient. It also needs to be performant and fail fast. In the event there is DB downtime, the system should serve a last known good to make a best effort of serving data for critical BAU processes, that can impact customer playout.

## Overview
Distributed Systems is a modular microservice domain designed for comprehensive monitoring and management of various components within an environment. It integrates essential tools like Prometheus, Grafana, and Alert Manager to ensure optimal system performance and visibility. 

It integrates a Springboot Application, Channel-Metadata-Store (CMS) with a MySQL database (DB), persisting country streaming channels metadata. CMS also utilizes a hazelcast cluser, used as distributed cache in a 'write-through' context. This strategy ensures that we update the cache eachtime we update the database, and in the event the DB fell over, we can use this as a last known good until services are fully restored, presenting key resilliency in availability and design. 

In the event the DB did go over, we want to ensure any cached items added that weren't able to be populated in the database, are. We did this by enabling CMS to produce topics (db-retry-cache-topic:3-3) to indicate a need to retry updating DB with cache data, to a Kafka Cluster (3 brokers and a zoo keeper instance). Then, a seperate Springboot Application, Cache-Retry-Service (CRS), will consume these topics. Once CMS has produced to Kafka and CRS consuming topics, we extract key metadata containing timestamps of the database failover. We store this in a seperate MySql database (Cache-retry-DB). After updating the DB, and regardless of success or failure with the write, CRS calls CMS's GET /force-cache-update to attempt a cache update to DB.

There is then a Admin Web Interface, used by a user that would like to create/update streaming channels for various countries manually. This Admin interface has a form to build queries against CMS/CRS, and has a view of the database downtime captured in Cache-retry-DB to show general downtime.

## Tech Stack
Languages:
- Java 17
- Gradle
- Groovy
- Bash
- Scala
- HTML
- CSS
- JavaScript

Frameworks:
- SpringBoot

Monitoring:
- Prometheus
- Alert Manager
- Grafana

Networking:
- NGINX 

Persistence/Cache/Event processing
- MySQL
- Hazelcast
- Kafka/Zookeeper

Testing:
- Cucumber
- Gatling

Infrastructure as Code:
- Docker/Docker-compose

## Modules

### alert-manager
Handles alert notifications based on predefined thresholds. It routes alerts to appropriate channels and supports integration with multiple notification platforms.

### cache-retry
A springboot application that consumes from kafka, integrates with CMS and a MySQL DB (cache-retry-db). A Web app interfaces with this application to receive DB database downtime data from database.

### channel-metadata-store
A springboot application that produces kafka, integrates with CRS and a MySQL DB (DB). A Web app interfaces with this application to update streaming channel-metadata in the database.

### channel-metadata-nft
A non-functional-test suite that runs gatling simulation tests against the domain.

### container-scan-tests
A bash script that is run in conjunction with docker-compose, it runs scans against active containers as they're built to identify and report vulnrabilities based on status (low, medium, high, severe).

### func-tests
A test suite of cucumber functional tests, written in Java. These are functional tests confirming API functionality.

### grafana
A visualization tool that creates dashboards for real-time monitoring of system metrics. It allows users to customize and share dashboards for effective data interpretation.

### hazelcast
A distributed cache that is used to store channel-metadata written to DB by CMS, and updated here to be used as a last-known good resilliency pattern.

### init
Used as a directive for docker volume mounting, used by DB to create tables and update table with data at the point the DB container is created.

### init-cache-retry
Same use case as 'init' above.

### kafka
An event messaging system used to enable CMS to produce and CRS to consume topics from.

### prometheus
A robust monitoring tool that collects and stores metrics. It serves as the core of the system, powering alerting and visualization tools.

### web-app
A html/css/javascript front end, built to interface with live API's. It is used by admins that want to update channel-metadata in CMS's DB.

### zookeeper
Used in a kafka cluster to elect primary and secondary brokers (in the event primary broker had downtime). Elected leaders deal with ensuring all brokers in the cluster are hanlding evenly distributed workloads. Zookeeper manages the clusters health and ensures Kafka is always available regardless of troublesome nodes experiencing issues/downtime.

## How to Run
### Start the containers
To run the containers and get integration out the box, run the following - this is advised to repeat each time you rebuild the cluster:
```
# Gracefully end the cluster
docker-compose down

# Remove all unused data (images, containers, networks, volumes, etc.)
docker system prune -a -f

# Build containers from docker-compose configuration
docker-compose build

# Start containers
docker-compose up
```

These are the following available hosts:
- NGINX - "80:80"
- Channel Metadata Store via NGINX - "http://localhost:80" - used to load balance across replicas
- Cache Retry Service via NGINX - "http://localhost:90" - used to load balance across replicas
- Channel Metadata Store without NGINX - "http://localhost:8080"
- Cache Retry Service without NGINX - "http://localhost:9090"
- Kafka Brokers - kafka-1:9093, kafka-2:9095, kafka-3:9097 
- Zookeeper - zookeeper:2181
- Hazelcast Cluster - hazelcast:5701, hazelcast:5702, hazelcast:5703, hazelcast:5704 
- DB - jdbc:mysql://db:3306/channel_metadata_db
- Cache Retry DB - jdbc:mysql://cache-retry-db:3306/cache_retry_db
- Prometheus - "http://localhost:9090"
- Alert Manager - "http://localhost:9093"
- Grafana - "http://localhost:3000

List of network exposed:
```
CONTAINER ID   IMAGE                                        COMMAND                  CREATED          STATUS                          PORTS                                                NAMES
37e6f4715cb2   distributed-systems-cache-retry              "java -jar app.jar .…"   31 minutes ago   Up 15 minutes                   0.0.0.0:54306->8090/tcp                              distributed-systems-cache-retry-3
45aa9e58e3c7   distributed-systems-cache-retry              "java -jar app.jar .…"   31 minutes ago   Up 15 minutes                   0.0.0.0:54309->8090/tcp                              distributed-systems-cache-retry-1
d8db926adc65   distributed-systems-cache-retry              "java -jar app.jar .…"   31 minutes ago   Up 15 minutes                   0.0.0.0:54310->8090/tcp                              distributed-systems-cache-retry-2
28e5500b685e   distributed-systems-cache-retry              "java -jar app.jar .…"   31 minutes ago   Up 15 minutes                   0.0.0.0:54312->8090/tcp                              distributed-systems-cache-retry-4
f3db8ca91331   nginx:latest                                 "/docker-entrypoint.…"   10 hours ago     Up 15 minutes                   0.0.0.0:80->80/tcp, 0.0.0.0:90->90/tcp               distributed-systems-nginx-1
9b1d05a33279   grafana/grafana-enterprise                   "/run.sh"                10 hours ago     Up 15 minutes                   0.0.0.0:3000->3000/tcp                               distributed-systems-grafana-1
31b048be3284   distributed-systems-channel-metadata-store   "java -jar app.jar .…"   10 hours ago     Up 15 minutes                   0.0.0.0:54297->8080/tcp                              distributed-systems-channel-metadata-store-4
eee817546e30   distributed-systems-channel-metadata-store   "java -jar app.jar .…"   10 hours ago     Up 15 minutes                   0.0.0.0:54298->8080/tcp                              distributed-systems-channel-metadata-store-2
db30150b5153   distributed-systems-channel-metadata-store   "java -jar app.jar .…"   10 hours ago     Up 15 minutes                   0.0.0.0:54302->8080/tcp                              distributed-systems-channel-metadata-store-1
4ea6ca2f903c   distributed-systems-channel-metadata-store   "java -jar app.jar .…"   10 hours ago     Up 15 minutes                   0.0.0.0:54305->8080/tcp                              distributed-systems-channel-metadata-store-3
bc1d89c1eadc   prom/prometheus                              "/bin/prometheus --c…"   10 hours ago     Up 15 minutes                   0.0.0.0:9090->9090/tcp                               distributed-systems-prometheus-1
685f8047e613   confluentinc/cp-kafka:7.4.0                  "/etc/confluent/dock…"   10 hours ago     Up 15 minutes                   9092-9093/tcp                                        kafka-1
4f79064028ea   mysql:5.7                                    "docker-entrypoint.s…"   10 hours ago     Up 15 minutes                   33060/tcp, 0.0.0.0:3309->3306/tcp                    cache-retry-db
71faef301a88   wurstmeister/zookeeper:latest                "/bin/sh -c '/usr/sb…"   10 hours ago     Up 15 minutes                   22/tcp, 2888/tcp, 3888/tcp, 0.0.0.0:2181->2181/tcp   distributed-systems-zookeeper-1
cdaaaa1b29de   confluentinc/cp-kafka:7.4.0                  "/etc/confluent/dock…"   10 hours ago     Up 15 minutes                   9092/tcp, 9097/tcp                                   kafka-3
5edd8d03ea27   mysql:5.7                                    "docker-entrypoint.s…"   10 hours ago     Up 15 minutes                   0.0.0.0:3306->3306/tcp, 33060/tcp                    db
fc212bec049b   hazelcast/hazelcast:5.0.3                    "hz start -c /opt/ha…"   10 hours ago     Up 15 minutes                   0.0.0.0:5702->5701/tcp                               distributed-systems-hazelcast-2
61c905331859   hazelcast/hazelcast:5.0.3                    "hz start -c /opt/ha…"   10 hours ago     Up 15 minutes                   0.0.0.0:5703->5701/tcp                               distributed-systems-hazelcast-4
fa7f316b92f1   prom/alertmanager                            "/bin/alertmanager -…"   10 hours ago     Restarting (1) 36 seconds ago                                                        distributed-systems-alertmanager-1
d079ac3a6380   hazelcast/hazelcast:5.0.3                    "hz start -c /opt/ha…"   10 hours ago     Up 15 minutes                   0.0.0.0:5704->5701/tcp                               distributed-systems-hazelcast-3
96b9ca7fc68e   hazelcast/hazelcast:5.0.3                    "hz start -c /opt/ha…"   10 hours ago     Up 15 minutes                   0.0.0.0:5701->5701/tcp                               distributed-systems-hazelcast-1
```

### Accessing MySQL within DB / Cache-retry DB 
To exec into MySQL, you will need to exec onto the container, and then access mysql by signing in as a user. To gain access and view data, use:

```
# Exec into the MySQL container (replace 'mysql-container' with your container name)
docker exec -it mysql-container /bin/bash

# Access MySQL as root and provide password to login, as defined in docker-compose
mysql -u root -p password

# Inside MySQL, switch to the desired database
USE database_name;

# Show tables in the selected database
SHOW TABLES;

# View all records from a table (replace 'table_name' with the table's name)
SELECT * FROM table_name;
```

### Running Website 
You can use any http server and point the resources for it to be served. Avoid using live server as it causes reload issues. 

I decided to use [http-server ](https://www.npmjs.com/package/http-server) as it didn't impact me in the same way live server did and was easy to set up and use.

Installation steps:
```
# via npm
npm install http-server

# via brew
brew install http-server
```

To run it, you will need to run the following command:
```
# With Cross-origin-resource-sharing (CORS)
http-server -p 2000 --cors

# Without Cross-origin-resource-sharing (CORS)
http-server -p 2000 
```

### Running security scan
To run the security scan for vulnarabilities within our images, please run the following script and run docker-compose up alongside it, so it can successful scan images available at the time they spin up.
```
./container-scan-tests/vuln-scanner.sh && docker-compose up 
```

### Running Func tests
Functional tests, written in cucumber are available to validate black-box / business spec functional tests. To run via gradle, use:
```
# Run functional tests for cache-retry using its test-specific tag
./gradlew test -Dcucumber.options="--tags @cacheRetry"
```

### Running NFT's
Various Peakload NFT's are available to simulate peak load across the domain. You can run them via gradle, using:
```
# Simulate peak load with DB down and half cache capacity
./gradlew gatlingRunDbDownAndHalfCache

# Simulate peak load with a single Hazelcast instance down
./gradlew gatlingRunWithSingleCacheDown

# Simulate peak load with a single Kafka broker down
./gradlew gatlingRunWithSingleKafkaBrokerDown

# Simulate peak load with a single service down
./gradlew gatlingRunWithSingleServiceDown

# Simulate peak load with DB down and cache-retry service integration
./gradlew gatlingRunDbDownCacheRetryServiceIntegrating

# Run a production traffic extended peak load simulation
./gradlew prodSimulation
```

Alternatively, you can also run them via the build.gradle manually, selecting a specific task:
```
task gatlingRunDbDownAndHalfCache(type: JavaExec) {
    classpath = sourceSets.gatling.runtimeClasspath
    main = "io.gatling.app.Gatling"
    args = [
            '--simulation', 'simulationDowntime.PeakLoadWithDbDownAndHalfCacheCapacity'
    ]
}

task gatlingRunWithSingleCacheDown(type: JavaExec) {
    classpath = sourceSets.gatling.runtimeClasspath
    main = "io.gatling.app.Gatling"
    args = [
            '--simulation', 'simulationDowntime.PeakLoadWithSingleHazelcastDown'
    ]
}

task gatlingRunWithSingleKafkaBrokerDown(type: JavaExec) {
    classpath = sourceSets.gatling.runtimeClasspath
    main = "io.gatling.app.Gatling"
    args = [
            '--simulation', 'simulationDowntime.PeakLoadWithSingleKafkaBrokerDown'
    ]
}

task gatlingRunWithSingleServiceDown(type: JavaExec) {
    classpath = sourceSets.gatling.runtimeClasspath
    main = "io.gatling.app.Gatling"
    args = [
            '--simulation', 'simulationDowntime.PeakLoadWithSingleServiceDown'
    ]
}

task gatlingRunDbDownCacheRetryServiceIntegrating(type: JavaExec) {
    classpath = sourceSets.gatling.runtimeClasspath
    main = "io.gatling.app.Gatling"
    args = [
            '--simulation', 'simulationDowntime.PeakLoadWithDbDownCacheRetryServiceIntegrating'
    ]
}


task prodSimulation(type: JavaExec) {
    classpath = sourceSets.gatling.runtimeClasspath
    main = "io.gatling.app.Gatling"
    args = [
            '--simulation', 'simulation.ProductionTrafficExtendedPeakLoad'
    ]

```

### Docker Management Commands
To clean up old Docker images, containers, and volumes and build the environment:
```
# Remove all unused data (images, containers, networks, volumes, etc.)
docker system prune -a -f

# Build containers from docker-compose configuration
docker-compose build

# Start containers
docker-compose up
```

To review running containers, or networks and to inspect a network container:
```
# List running docker processes aka containers
docker ps

# View Docker networks currently running
docker network ls

# Inspect a specific network
docker network inspect <containerID>
```

To exec into a container, please use:
```
# List running docker processes aka containers
docker ps

# Exec onto the container with bash
docker exec -it <containerId> /bin/bash
```

## Contact
For inquiries, please reach out via the Slack channel `#myapp-support`.
