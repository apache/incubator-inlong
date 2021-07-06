/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.plugin.message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.agent.message.ProxyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle List of BusMessage, which belong to the same tid.
 */
public class PackProxyMessage {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackProxyMessage.class);

    private final String tid;

    private int currentSize;

    private final int maxPackSize;

    private final int maxQueueNumber;
    // ms
    private final int cacheTimeout;

    // tid -> list of proxyMessage
    private final LinkedBlockingQueue<ProxyMessage> messageQueue;

    private volatile long currentCacheTime = System.currentTimeMillis();
    private final AtomicLong queueSize = new AtomicLong(0);

    /**
     * Init PackBusMessage
     *
     * @param maxPackSize - max pack size for one bid
     * @param cacheTimeout - cache timeout for one proxy message
     * @param tid - tid
     */
    public PackProxyMessage(int maxPackSize, int maxQueueNumber, int cacheTimeout, String tid) {
        this.maxPackSize = maxPackSize;
        this.maxQueueNumber = maxPackSize * 10;
        this.cacheTimeout = cacheTimeout;
        // double size of package
        this.messageQueue = new LinkedBlockingQueue<>(maxQueueNumber);
        this.tid = tid;
    }

    /**
     * Check whether queue is nearly full
     *
     * @return true if is nearly full else false.
     */
    private boolean queueIsFull() {
        return messageQueue.size() >= maxQueueNumber - 1;
    }

    /**
     * Add proxy message to cache, proxy message should belong to the same tid.
     */
    public void addProxyMessage(ProxyMessage message) {
        assert tid.equals(message.getTid());
        try {
            if (queueIsFull()) {
                LOGGER.warn("message queue is greater than {}, stop adding message, "
                                + "maybe proxy get stuck", maxQueueNumber);
            }
            messageQueue.put(message);
            queueSize.addAndGet(message.getBody().length);
        } catch (Exception ex) {
            LOGGER.error("exception caught", ex);
        }
        messageQueue.offer(message);
    }

    /**
     * check message queue is empty or not
     * @return
     */
    public boolean isEmpty() {
        return messageQueue.isEmpty();
    }

    /**
     * Fetch batch of proxy message, timeout message or max number of list satisfied.
     *
     * @return map of message list, key is tid for the batch.
     * return null if there are no valid messages.
     */
    public Pair<String, List<byte[]>> fetchBatch() {
        // if queue is nearly full or package size is satisfied or timeout
        long currentTime = System.currentTimeMillis();
        if (queueSize.get() > maxPackSize || queueIsFull()
                || currentTime - currentCacheTime > cacheTimeout) {
            // refresh cache time.
            currentCacheTime = currentTime;
            long resultBatchSize = 0;
            List<byte[]> result = new ArrayList<>();
            while (!messageQueue.isEmpty()) {
                // pre check message size
                ProxyMessage peekMessage = messageQueue.peek();
                if (peekMessage == null
                        || resultBatchSize + peekMessage.getBody().length > maxPackSize) {
                    break;
                }
                ProxyMessage message = messageQueue.remove();
                if (message != null) {
                    int bodySize = message.getBody().length;
                    resultBatchSize += bodySize;
                    // decrease queue size.
                    queueSize.addAndGet(-bodySize);
                    result.add(message.getBody());
                }
            }
            // make sure result is not empty.
            if (!result.isEmpty()) {
                return Pair.of(tid, result);
            }
        }
        return null;
    }
}
