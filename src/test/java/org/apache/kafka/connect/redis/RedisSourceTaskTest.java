
package org.apache.kafka.connect.redis;

import static java.lang.Thread.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.apache.kafka.connect.source.SourceRecord;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moilioncircle.redis.replicator.cmd.impl.LPushCommand;
import com.moilioncircle.redis.replicator.event.Event;

public class RedisSourceTaskTest {

    @Test
    public void testTaskStart() throws InterruptedException {
        final HashMap<String, String> props = new HashMap<>();
        props.put(RedisSourceConfig.USE_PSYNC2, "false");
        props.put(RedisSourceConfig.TOPIC, "fantacy");
        final RedisSourceTask task = new RedisSourceTask();
        task.start(props);
        sleep(5000);
        task.poll();
        task.stop();
    }

    @Test
    public void shouldGetSourceTaskWithJSONForEvent() throws Exception {
        // LPUSH key value1 value2
        final Event event = new LPushCommand("key".getBytes(StandardCharsets.UTF_8),
                new byte[][] { "value1".getBytes(StandardCharsets.UTF_8), "value2".getBytes(StandardCharsets.UTF_8) });

        final RedisSourceTask task = new RedisSourceTask();
        final SourceRecord sourceRecord = task.getSourceRecord(event);

        assertThat(sourceRecord, is(notNullValue()));
        assertThat(sourceRecord.value(), is(notNullValue()));
        assertThat(sourceRecord.value().toString(), is("{\"key\":\"a2V5\",\"values\":[\"dmFsdWUx\",\"dmFsdWUy\"]}"));

        final ObjectMapper mapper = new ObjectMapper();
        final LPushCommand decoded = mapper.readValue(sourceRecord.value().toString().getBytes(StandardCharsets.UTF_8),
                LPushCommand.class);
        assertThat(decoded, is(notNullValue()));
        assertThat(decoded.getKey(), is(notNullValue()));
        assertThat(new String(decoded.getKey()), is("key"));
        assertThat(decoded.getValues(), is(notNullValue()));
        assertThat(decoded.getValues().length, is(2));
        byte[][] values = decoded.getValues();
        for (int i = 0; i < values.length; i++) {
            final byte[] value = values[i];
            assertThat(new String(value), is("value" + (i + 1)));
        }
    }
}
