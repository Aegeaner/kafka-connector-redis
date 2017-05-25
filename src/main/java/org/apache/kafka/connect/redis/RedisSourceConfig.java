package org.apache.kafka.connect.redis;

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


import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.HashMap;
import java.util.Map;

public class RedisSourceConfig extends AbstractConfig {
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String POLL_BATCH_SIZE = "poll_batch_size";
    public static final String IN_MEMORY_EVENT_SIZE = "in_memory_event_size";
    public static final String MEMORY_RATIO = "memory_ratio";
    public static final String EVENT_CACHE_FILE = "event_cache_file";
    public static final String SOURCE_PARTITION_KEY = "Partition";
    public static final String SOURCE_PARTITION_VALUE = "Redis";
    public static final String OFFSET_KEY = "Offset";
    public static final String TOPIC = "topic";
    public static final String USE_PSYNC2 = "use_psync2";


    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(HOST, ConfigDef.Type.STRING, "localhost", ConfigDef.Importance.HIGH, "MongoDB Server host")
            .define(PORT, ConfigDef.Type.INT, 6379, ConfigDef.Importance.HIGH, "MongoDB Server port")
            .define(TOPIC, ConfigDef.Type.STRING, "redis", ConfigDef.Importance.HIGH, "Redis database topic in kafka")
            .define(POLL_BATCH_SIZE, ConfigDef.Type.INT, 100, ConfigDef.Importance.HIGH, "Task poll batch size")
            .define(IN_MEMORY_EVENT_SIZE, ConfigDef.Type.LONG, 1024L, ConfigDef.Importance.HIGH, "In memory event size")
            .define(MEMORY_RATIO, ConfigDef.Type.DOUBLE, 0.5, ConfigDef.Importance.HIGH, "Memory ratio limit")
            .define(EVENT_CACHE_FILE, ConfigDef.Type.STRING, "events", ConfigDef.Importance.HIGH, "Event cache file name")
            .define(USE_PSYNC2, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.HIGH, "Whether use Psync 2 introduced by redis 4.0");

    public RedisSourceConfig(Map<?, ?> originals) {
        super(CONFIG_DEF, originals);
    }

    public Map<String, String> defaultsStrings() {
        Map<String, String> copy = new HashMap<>();
        for (Map.Entry<String, ?> entry : values().entrySet()) {
            copy.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return copy;
    }
}
