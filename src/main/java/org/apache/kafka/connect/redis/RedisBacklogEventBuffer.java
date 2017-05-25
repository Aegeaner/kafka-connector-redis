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


import com.moilioncircle.redis.replicator.cmd.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RedisBacklogEventBuffer {
    private static final Logger log = LoggerFactory.getLogger(RedisBacklogEventBuffer.class);
    private final ConcurrentLinkedQueue<Command> queue;
    private final MemoryChecker memoryChecker;
    private File eventCacheFile;
    private final String eventCacheFileName;
    private ObjectInputStream is = null;
    private ObjectOutputStream os = null;
    private long inMemoryLimit;
    private int inMemorySize = 0;
    private int inFileSize = 0;
    private boolean shouldEvict = false;

    // signals that buffer should not accept events any more.
    private volatile boolean closed = false;

    public RedisBacklogEventBuffer(long inMemoryLimit, double ratio, String event_cache_file_name) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.inMemoryLimit = inMemoryLimit;
        this.memoryChecker = new MemoryChecker(this, ratio);
        this.eventCacheFileName = event_cache_file_name;
        this.eventCacheFile = new File(eventCacheFileName);
        try {
            os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(eventCacheFile)));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void put(Command event) throws IOException {
        if(closed) {
            log.error("Attempt to put event to closed buffer. Rejecting event.");
            return;
        }

        if(event == null) {
            log.error("Attempt to put an empty event to buffer. Rejecting event.");
            return;
        }

        inMemorySize++;
        queue.offer(event);
        //log.debug(queue.peek().toJson());

        if (shouldEvict(event)) {
            while (inMemorySize > 0) {
                persist(queue.poll());
                inFileSize++;
                inMemorySize--;
            }
            log.debug("[EVICTED] {}", event);
        }

        return;
    }

    private boolean shouldEvict(Command event) {

        if (shouldEvict) {
            log.debug("Memory full, should begin to evict events");
        }
        return shouldEvict || (inMemorySize >= inMemoryLimit);
    }

    private void persist(Command event) {
        try {
            if(os == null || !eventCacheFile.canWrite()) {
                eventCacheFile.delete();
                eventCacheFile = new File(eventCacheFileName);
                os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(eventCacheFile)));
            }
            if (os != null) {
                os.writeObject(event);
                //os.flush();
            } else {
                log.error("Fail to get available output stream.");
            }
        } catch (IOException e) {
            log.error("Fail to write event cache file: {}", e.getMessage());
        }
    }

    public Command poll()  {
        Command event = null;
        if(inFileSize > 0) {
            if ( is == null ) {
                try {
                    os.flush();
                } catch (IOException e) {
                    log.error("Fail to flush event cache file: {}", e.getMessage());
                }
                try {
                    is = new ObjectInputStream(new BufferedInputStream(new FileInputStream(eventCacheFile)));
                } catch (IOException e) {
                    log.error("Fail to open event cache file to read: {}", e.getMessage());
                }
            }
            try {
                event = (Command) is.readObject();
            } catch (Exception e) {
                log.error("Fail to deserialize event cache file: {}", e.getMessage());
            }
            inFileSize--;
            if (inFileSize == 0) {
                try {
                    os.close();
                    os = null;
                } catch (IOException e) {
                    log.error("Fail to close event cache file: {}", e.getMessage());
                }
            }
        } else if(!queue.isEmpty()) {
            inMemorySize--;
            event = queue.poll();
        }
        return event;
    }

    public boolean empty() {
        return queue.isEmpty();
    }

    public void setShouldEvict(boolean shouldEvict) {
        this.shouldEvict = shouldEvict;
    }

    public void clearEventCacheFile() {
        this.eventCacheFile.delete();
        log.info("Event cache file deleted.");
    }

    public void stop() {
        this.closed = true;
    }
}
