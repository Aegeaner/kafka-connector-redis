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