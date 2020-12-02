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


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

public class RedisPsyncTest {

    private boolean use_psync2;

    @Before
    public void setUp() {
        this.use_psync2 = false;
    }

    @Test
    public void testGetRedisInfo() {
        MasterSnapshotRetriever msr = new MasterSnapshotRetriever("localhost", 6379);
        MasterSnapshot ms = msr.snapshot(use_psync2);
        assertNotNull(ms.getRunId());
        assertNotNull(ms.getMasterReplOffset());
    }

    @Test
    public void testGetRedisInfoUsingRedisPassword() {

   /*this test assumes the local instance running Redis is NOT using the "requirepass" feature in redis.conf

    Which yields the following Error:
    ERR AUTH <password> called without any password configured for the default user. Are you sure your configuration is correct?
    */
        Optional<Exception> exceptionCaught = Optional.empty();
        try {
            MasterSnapshotRetriever msr = new MasterSnapshotRetriever("localhost", 6379, "testpassword");
            MasterSnapshot ms = msr.snapshot(use_psync2);
        } catch (Exception e){
            exceptionCaught = Optional.of(e);
        }
        Assert.assertTrue(exceptionCaught.isPresent());
        Assert.assertTrue(exceptionCaught.get().getMessage().contains("ERR AUTH <password> called without any password configured for the default user."));
    }

    @Test
    public void testGetRedisInfoUsingRedisPasswordAndDB() {//this test assumes the local instance running Redis is NOT using the "requirepass" feature in redis.conf

        Optional<Exception> exceptionCaught = Optional.empty();
        try {
            MasterSnapshotRetriever msr = new MasterSnapshotRetriever("localhost", 6379, "testpassword", "0");
            MasterSnapshot ms = msr.snapshot(use_psync2);
        } catch (Exception e){
            exceptionCaught = Optional.of(e);
        }

        Assert.assertTrue(exceptionCaught.isPresent());
        Assert.assertTrue(exceptionCaught.get().getMessage().contains("ERR AUTH <password> called without any password configured for the default user."));
    }

    @Test
    public void testPartialSync() throws IOException, InterruptedException {
        RedisBacklogEventBuffer eventBuffer = new RedisBacklogEventBuffer(1024 * 1024L, 1.0, "events");
        HashMap<String, String> props = new HashMap();
        props.put(RedisSourceConfig.USE_PSYNC2, String.valueOf(use_psync2));
        RedisPartialSyncWorker psyncWorker = new RedisPartialSyncWorker(eventBuffer, props);
        Thread workerThread = new Thread(psyncWorker);
        ExecutorService service = Executors.newFixedThreadPool(1);
        service.submit(workerThread);
        service.awaitTermination(50, TimeUnit.SECONDS);
    }
}
