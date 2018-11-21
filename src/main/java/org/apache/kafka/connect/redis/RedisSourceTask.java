package org.apache.kafka.connect.redis;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moilioncircle.redis.replicator.event.Event;

public class RedisSourceTask extends SourceTask {
    private static final Logger log = LoggerFactory.getLogger(RedisSourceTask.class);

    private long in_memory_event_size;
    private double memory_ratio;
    private String event_cache_file_name;
    private RedisBacklogEventBuffer eventBuffer;

    private String topic;

    @Override
    public String version() {
        return AppInfoParser.getVersion();
    }

    @Override
    public void start(Map<String, String> props) {
        Map<String, Object> configuration = RedisSourceConfig.CONFIG_DEF.parse(props);
        in_memory_event_size = (long) configuration.get(RedisSourceConfig.IN_MEMORY_EVENT_SIZE);
        memory_ratio = (double) configuration.get(RedisSourceConfig.MEMORY_RATIO);
        event_cache_file_name = (String) configuration.get(RedisSourceConfig.EVENT_CACHE_FILE);
        topic = (String) configuration.get(RedisSourceConfig.TOPIC);


        eventBuffer = new RedisBacklogEventBuffer(in_memory_event_size, memory_ratio, event_cache_file_name);

        RedisPartialSyncWorker psyncWorker = new RedisPartialSyncWorker(eventBuffer, props);
        Thread workerThread = new Thread(psyncWorker);
        workerThread.start();
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        ArrayList<SourceRecord> records = new ArrayList<>();

        Event event = eventBuffer.poll();

        if (event != null) {
            //log.debug(ev.toJson());
            SourceRecord sourceRecord = getSourceRecord(event);
            log.debug(sourceRecord.toString());
            records.add(sourceRecord);
        }

        return records;
    }

    private SourceRecord getSourceRecord(Event event) {
        Map<String, String> partition = Collections.singletonMap(RedisSourceConfig.SOURCE_PARTITION_KEY, RedisSourceConfig.SOURCE_PARTITION_VALUE);
        SchemaBuilder bytesSchema = SchemaBuilder.bytes();

        // Redis backlog has no offset or timestamp
        Timestamp ts = new Timestamp(System.currentTimeMillis()); // avoid invalid timestamp exception
        long timestamp = ts.getTime();

        // set timestamp as offset
        Map<String, ?> offset = Collections.singletonMap(RedisSourceConfig.OFFSET_KEY, timestamp);

        return new SourceRecord(partition, offset, this.topic, null, bytesSchema, ByteBuffer.wrap(event.getClass().toString().getBytes()), null, event, timestamp);
    }

    @Override
    public void stop() {

    }
}
