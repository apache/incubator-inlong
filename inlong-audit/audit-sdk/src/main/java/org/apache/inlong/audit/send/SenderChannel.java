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

package org.apache.inlong.audit.send;

import org.apache.inlong.audit.util.EventLoopUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.Attribute;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

public class SenderChannel {

    private static final Logger LOG = LoggerFactory.getLogger(SenderChannel.class);

    public static final int DEFAULT_SEND_THREADNUM = 1;
    public static final int DEFAULT_RECEIVE_BUFFER_SIZE = 16777216;
    public static final int DEFAULT_SEND_BUFFER_SIZE = 16777216;

    private InetSocketAddress addr;
    private Channel channel;
    private String channelKey;
    private Semaphore packToken;
    private Bootstrap client;
    private SenderManager senderManager;

    /**
     * Constructor
     *
     * @param addr
     */
    public SenderChannel(InetSocketAddress addr, int maxSynchRequest, SenderManager senderManager) {
        this.addr = addr;
        this.packToken = new Semaphore(maxSynchRequest);
        this.senderManager = senderManager;
    }

    /**
     * Try acquire channel
     *
     * @return
     */
    public boolean tryAcquire() {
        return packToken.tryAcquire();
    }

    /**
     * Try acquire channel
     *
     * @return
     */
    public boolean acquire() {
        try {
            packToken.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * release channel
     */
    public void release() {
        packToken.release();
    }

    /**
     * toString
     */
    @Override
    public String toString() {
        return addr.toString();
    }

    /**
     * get addr
     * @return the addr
     */
    public InetSocketAddress getAddr() {
        return addr;
    }

    /**
     * set addr
     * @param addr the addr to set
     */
    public void setAddr(InetSocketAddress addr) {
        this.addr = addr;
    }

    /**
     * get channel
     *
     * @return the channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * set channel
     *
     * @param channel the channel to set
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
        Attribute<String> attr = this.channel.attr(SenderGroup.CHANNEL_KEY);
        attr.set(channelKey);
    }

    /**
     * get channelKey
     * @return the channelKey
     */
    public String getChannelKey() {
        return channelKey;
    }

    /**
     * set channelKey
     * @param channelKey the channelKey to set
     */
    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

    private void init() {
        ThreadFactory selfDefineFactory = new DefaultThreadFactory("audit-client-io" + Thread.currentThread().getId(),
                Thread.currentThread().isDaemon());

        EventLoopGroup eventLoopGroup = EventLoopUtil.newEventLoopGroup(DEFAULT_SEND_THREADNUM,
                false, selfDefineFactory);
        client = new Bootstrap();
        client.group(eventLoopGroup);
        client.channel(EventLoopUtil.getClientSocketChannelClass(eventLoopGroup));
        client.option(ChannelOption.SO_KEEPALIVE, true);
        client.option(ChannelOption.TCP_NODELAY, false);
        client.option(ChannelOption.SO_REUSEADDR, false);
        client.option(ChannelOption.SO_RCVBUF, DEFAULT_RECEIVE_BUFFER_SIZE);
        client.option(ChannelOption.SO_SNDBUF, DEFAULT_SEND_BUFFER_SIZE);
        client.handler(new ClientPipelineFactory(senderManager));
    }

    /**
     * connect channel
     *
     * @return
     */
    public boolean connect() {
        if (checkConnect(this.channel)) {
            return true;
        }
        try {
            if (client == null) {
                init();
            }

            synchronized (client) {
                ChannelFuture future = client.connect(this.addr).sync();
                this.channel = future.channel();
                Attribute<String> attr = this.channel.attr(SenderGroup.CHANNEL_KEY);
                attr.set(channelKey);
            }
        } catch (Throwable e) {
            LOG.error("connect {} failed. {}", this.getAddr(), e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * check channeel connect
     *
     * @param channel
     * @return
     */
    private boolean checkConnect(Channel channel) {
        try {
            if (channel == null) {
                return false;
            }
            if (channel.isWritable() || channel.isOpen() || channel.isActive()) {
                return true;
            }
        } catch (Throwable ex) {
            LOG.error("check connect ex." + ex.getMessage());
        }
        return false;
    }
}
