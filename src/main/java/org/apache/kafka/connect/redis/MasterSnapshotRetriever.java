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


import redis.clients.jedis.Jedis;

import java.util.Properties;

public class MasterSnapshotRetriever {
    private Jedis jedis;

    private Properties extractRedisInfoSection(String section) {
        String[] lines = jedis.info(section).split("\r\n");
        Properties props = new Properties();
        for(String line: lines) {
            String[] kv = line.split(":");
            if (kv.length > 1)
                props.put(kv[0], kv[1]);
        }
        return props;
    }

    public MasterSnapshotRetriever(String host, int port) {
        jedis = new Jedis(host, port);
    }

    public MasterSnapshotRetriever(String host, int port, String password) {
        String url = "redis://:"+password+"@"+host+":"+port+"/"+"0";//default database is usually 0
        jedis = new Jedis(url);
    }

    public MasterSnapshotRetriever(String host, int port, String password, String dbName) {
        String url = "redis://:"+password+"@"+host+":"+port+"/"+dbName;
        jedis = new Jedis(url);
    }

    public MasterSnapshot snapshot(Boolean use_psync2) {
        jedis.connect();

        Properties serverProps = extractRedisInfoSection("server");
        Properties replicationProps = extractRedisInfoSection("replication");
        String run_id;
        if(use_psync2) {
            run_id = (String) replicationProps.get("master_replid");
        } else {
            run_id = (String) serverProps.get("run_id");
        }

        String master_repl_offset = (String) replicationProps.get("master_repl_offset");
        MasterSnapshot snapshot = new MasterSnapshot(run_id, master_repl_offset);

        jedis.disconnect();
        return snapshot;
    }
}
