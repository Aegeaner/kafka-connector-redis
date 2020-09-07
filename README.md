Notice
--
I used [RedisReplicator](https://github.com/leonchen83/redis-replicator) as the Redis comand parser, so e.g. the List push command is defined as: [LPushCommand.java](https://github.com/leonchen83/redis-replicator/blob/master/src/main/java/com/moilioncircle/redis/replicator/cmd/impl/LPushCommand.java). The connector wrapped the command using its name as the key, with the serialization of the command as the value.
So in the short, the answer is nothing should you do, just parse the command string like this: [LPushCommand.java#L83](https://github.com/leonchen83/redis-replicator/blob/f6711bd347e644ef53c1d28d7716dea957230150/src/main/java/com/moilioncircle/redis/replicator/cmd/impl/LPushCommand.java#L83)
If you dislike parsing the command string instead of raw redis protocol, feel free to change the value parameter here to `cmd.getRawValues()`: [RedisSourceTask.java#L94](https://github.com/Aegeaner/kafka-connector-redis/blob/1c6af9f1f26b5732fcabdad098ce28b8677c5175/src/main/java/org/apache/kafka/connect/redis/RedisSourceTask.java#L94)



Redis replication
--

Redis replication is a very simple to use and configure master-slave replication that allows slave Redis servers to be exact copies of master servers. The following are some very important facts about Redis replication:

* Redis uses asynchronous replication. Starting with Redis 2.8, however, slaves periodically acknowledge the amount of data processed from the replication stream.
* A master can have multiple slaves.
* Slaves are able to accept connections from other slaves. Aside from connecting a number of slaves to the same master, slaves can also be connected to other slaves in a cascading-like structure.
* Redis replication is non-blocking on the master side. This means that the master will continue to handle queries when one or more slaves perform the initial synchronization.
* Replication is also non-blocking on the slave side. While the slave is performing the initial synchronization, it can handle queries using the old version of the dataset, assuming you configured Redis to do so in redis.conf. Otherwise, you can configure Redis slaves to return an error to clients if the replication stream is down. However, after the initial sync, the old dataset must be deleted and the new one must be loaded. The slave will block incoming connections during this brief window (that can be as long as many seconds for very large datasets).
* Replication can be used both for scalability, in order to have multiple slaves for read-only queries (for example, slow O(N) operations can be offloaded to slaves), or simply for data redundancy.
* It is possible to use replication to avoid the cost of having the master write the full dataset to disk: a typical technique involves configuring your master redis.conf to avoid persisting to disk at all, then connect a slave configured to save from time to time, or with AOF enabled. However this setup must be handled with care, since a restarting master will start with an empty dataset: if the slave tries to synchronized with it, the slave will be emptied as well.

Partial resynchronization
--

Starting with Redis 2.8, master and slave are usually able to continue the replication process without requiring a full resynchronization after the replication link went down.

This works by creating an in-memory backlog of the replication stream on the master side. The master and all the slaves agree on a replication offset and a master run ID, so when the link goes down, the slave will reconnect and ask the master to continue the replication. Assuming the master run ID is still the same, and that the offset specified is available in the replication backlog, replication will resume from the point where it left off. If either of these conditions are unmet, a full resynchronization is performed (which is the normal pre-2.8 behavior). As the run ID of the connected master is not persisted to disk, a full resynchronization is needed when the slave restarts.

The new partial resynchronization feature uses the PSYNC command internally, while the old implementation uses the SYNC command. Note that a Redis slave is able to detect if the server it is talking with does not support PSYNC, and will use SYNC instead.


On Redis version 4.x
--

* Get plugin jar file of kafka-connect-redis
* Start redis-server (in order to use redis replication define Redis Master server in redis.conf file within Redis database directory and start Redis database by `redis-server ../redis.conf` command
* Edit `connect-standalone.properties` file (located in confluent-4.1.1/etc/schema-registry/)
* Add location of the plugin jar file in property plugin.path for example plugin.path=/usr/local/share/java,/usr/local/share/kafka/plugins,/opt/connectors/
* Set following properties to connect-standalone.properties file:
`key.converter=org.apache.kafka.connect.converters.ByteArrayConverter`
`value.converter=org.apache.kafka.connect.storage.StringConverter`
`key.converter.schemas.enable=false`
`value.converter.schemas.enable=false`

*Remove following properties from connect-standalone.properties file:
`key.converter.schema.registry.url=http://localhost:8081`
`value.converter.schema.registry.url=http://localhost:8081`

*Create new file named redis.config in following directory /confluent-4.1.1/etc/schema-registry/ and add following properties to this file:
`name=redis-config`
`connector.class=org.apache.kafka.connect.redis.RedisSourceConnector`
`tasks.max=1`
`topic=your-topic-name`
`host=localhost`
`port=6379`
* Open terminal and add jar files namely `commons-logging-1.2.jar, redis-replicator-2.4.5.jar and jedis-2.9.0.jar` to the CLASSPATH variable 
* Navigate to confluent/bin directory and start the connector by executing following command:
`./connect-standalone ../etc/schema-registry/connect-standalone.properties ../etc/schema-registry/redis.properties`

# On Redis version 5.x
- Get plugin jar file for kafka-connect-redis
- Start redis-server (start redis database from folder 'redis-stable/src' by executing command *./redis-server)
- Edit connect-standalone.properties file (located in confluent-4.1.1/etc/schema-registry/ ) add location of the plugin jar file in property plugin.path for example:
plugin.path=/home/yahussain/Tools/confluent-4.0.0/share/kafka-rest/
- Add following properties to connect-avro-standalone.properties
`key.converter=org.apache.kafka.connect.converters.ByteArrayConverter`
`value.converter=org.apache.kafka.connect.storage.StringConverter`
`key.converter.schemas.enable=false`
`value.converter.schemas.enable=false`
- Comment / remove following properties from connect-avro-standalone.properties file:
`key.converter.schema.registry.url=http://localhost:8081`
`value.converter.schema.registry.url=http://localhost:8081`
- Create new file named redis.config in following directory /confluent-4.1.1/etc/schema-registry/ and add following properties to this file:
name=redis-config
connector.class=org.apache.kafka.connect.redis.RedisSourceConnector
tasks.max=1
topic=your-topic-name
host=localhost
port=6379
- In plugin path (/home/yahussain/Tools/confluent-4.0.0/share/kafka-rest/) download and place the following plugins:
commons-logging-1.2.jar
jedis-2.9.0.jar
kafka-connect-redis-1.0-SNAPSHOT.jar
redis-replicator-3.0.1.jar
- Navigate to confluent/bin directory and set the CLASSPATH by executing command
- export CLASSPATH=/home/yahussain/Tools/confluent-4.0.0/share/kafka-rest/*
- Start kafka by executing command
`./confluent start kafka-rest`
- Start the connector by executing following command:
`./connect-standalone ../etc/schema-registry/connect-avro-standalone.properties ../etc/schema-registry/redis.properties`

# On Redis version 5.x/6.x with Confluent 5.1.0
- Get plugin jar file for kafka-connect-redis (kafka-connect-redis-1.0-SNAPSHOT.jar) by building this project (mvn clean package) and copying it from the ./target directory
- Create the plugin folder (/opt/confluent-5.1.0/share/java/kafka-connect-redis/)
- Start redis-server (start redis database from folder 'redis-stable/src' by executing command *./redis-server)
- Edit connect-avro-standalone.properties file (located in confluent-5.1.0/etc/schema-registry/ ) add location of the plugin jar file in property plugin.path for example:
plugin.path=share/java,/opt/confluent-5.1.0/share/confluent-hub-components
- Change the following property values in connect-avro-standalone.properties as specified here:
`key.converter=org.apache.kafka.connect.converters.ByteArrayConverter`
`value.converter=org.apache.kafka.connect.storage.StringConverter`
`key.converter.schemas.enable=false`
`value.converter.schemas.enable=false`
- Comment / remove following properties from connect-avro-standalone.properties file:
`key.converter.schema.registry.url=http://localhost:8081`
`value.converter.schema.registry.url=http://localhost:8081`
- Create new file named redis.properties in following directory /confluent-5.1.0/etc/schema-registry/ and add following properties to this file:
`name=redis-config`
`connector.class=org.apache.kafka.connect.redis.RedisSourceConnector`
`tasks.max=1`
`topic=your-topic-name`
`host=localhost`
`port=6379`
-If your redis server has the password enabled add these properties (dbName is always 0 unless you have configured otherwise):
`password=yourpassword`
`dbName=0`
- In plugin folder (/opt/confluent-5.1.0/share/java/kafka-connect-redis/) download and place the following plugins:
`commons-logging-1.2.jar`
`jedis-2.9.0.jar`
`kafka-connect-redis-1.0-SNAPSHOT.jar`
`redis-replicator-3.0.1.jar`
- Navigate to confluent/bin directory and set the CLASSPATH by executing command
- export CLASSPATH=/opt/confluent-5.1.0/share/java/kafka-connect-redis/*
- Start kafka by executing command
`./confluent start kafka-rest`
- Start the connector by executing following command:
`./connect-standalone ../etc/schema-registry/connect-avro-standalone.properties ../etc/schema-registry/redis.properties`