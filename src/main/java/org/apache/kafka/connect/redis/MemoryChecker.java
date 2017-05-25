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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MemoryChecker {
    private static final Logger log = LoggerFactory.getLogger(MemoryChecker.class);
    private final double memoryLimitRatio;
    private final RedisBacklogEventBuffer eventBuffer;

    private Thread thread = new Thread() {
        @Override
        public void run() {
            check();
        }
    };

    private void check() {
        // http://stackoverflow.com/questions/12807797/java-get-available-memory
        long maxMem = Runtime.getRuntime().maxMemory();
        long allocatedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long availableMem = maxMem - allocatedMem;
        double memRatio = 1.0 - availableMem * 1.0 / maxMem;
        if (memRatio > memoryLimitRatio) {
            this.eventBuffer.setShouldEvict(true);
        } else {
            this.eventBuffer.setShouldEvict(false);
        }
    }

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);


    public MemoryChecker(RedisBacklogEventBuffer buffer, double ratio) {
        this.eventBuffer = buffer;
        this.memoryLimitRatio = ratio;
        scheduler.scheduleAtFixedRate(thread, 0, 60, TimeUnit.SECONDS);
        log.info("Memory checker started.");
    }
}
