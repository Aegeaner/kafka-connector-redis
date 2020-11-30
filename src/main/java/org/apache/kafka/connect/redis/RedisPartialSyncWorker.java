package org.apache.kafka.connect.redis;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


import com.moilioncircle.redis.replicator.Configuration;
import com.moilioncircle.redis.replicator.RedisReplicator;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.event.EventListener;

public class RedisPartialSyncWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(RedisPartialSyncWorker.class);

    private RedisReplicator replicator = null;
    private final String host;
    private final Integer port;
    private final String password;
    private final String dbName;
    private Boolean use_psync2;

    public Configuration getSourceConfiguration(String host, Integer port, Boolean use_psync2) {
        Configuration conf = Configuration.defaultSetting();
        MasterSnapshotRetriever msr;

        if (password==null || password.isEmpty()){
            msr = new MasterSnapshotRetriever(host, port);// if no password is provided we try to connect without a password
        } else{
            msr = new MasterSnapshotRetriever(host, port, password, dbName ); //dbName defaults to 0 in RedisSourceConfig class so we can trust this will be set
            conf.setAuthPassword(password);
        }

        MasterSnapshot ms = msr.snapshot(use_psync2);

        conf.setReplId(ms.getRunId());
        conf.setReplOffset(Long.valueOf(ms.getMasterReplOffset()));
        return conf;
    }

    public RedisPartialSyncWorker(final RedisBacklogEventBuffer eventBuffer, Map<String, String> props) {
        Map<String, Object> configuration = RedisSourceConfig.CONFIG_DEF.parse(props);
        host = (String) configuration.get(RedisSourceConfig.HOST);
        port = (Integer) configuration.get(RedisSourceConfig.PORT);
        password = (String) configuration.get(RedisSourceConfig.PASSWORD);
        dbName = (String) configuration.get(RedisSourceConfig.DB_NAME);

        use_psync2 = (Boolean)configuration.get(RedisSourceConfig.USE_PSYNC2);

        Configuration sourceOffset = getSourceConfiguration(host, port, use_psync2);
            replicator = new RedisReplicator(host, port, sourceOffset);
            addEventListener(eventBuffer);
    }

    private void addEventListener(final RedisBacklogEventBuffer eventBuffer) {
        replicator.addEventListener(new EventListener() {
            @Override
            public void onEvent(Replicator replicator, Event event) {
                log.debug(event.toString());
                try {
                    eventBuffer.put(event);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void run() {
        try {
            replicator.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
