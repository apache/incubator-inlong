/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.network;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.inlong.dataproxy.ProxyClientConfig;
import org.apache.inlong.dataproxy.ConfigConstants;
import org.apache.inlong.dataproxy.codec.EncodeObject;
import org.apache.inlong.dataproxy.config.ProxyConfigEntry;
import org.apache.inlong.dataproxy.config.ProxyConfigManager;
import org.apache.inlong.dataproxy.config.EncryptConfigEntry;
import org.apache.inlong.dataproxy.config.HostInfo;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMgr {
    private static final Logger logger = LoggerFactory
            .getLogger(ClientMgr.class);


    private final Map<HostInfo, NettyClient> clientMapData = new ConcurrentHashMap<HostInfo, NettyClient>();

    private final ConcurrentHashMap<HostInfo, NettyClient> clientMapHB = new ConcurrentHashMap<HostInfo, NettyClient>();
    // clientMapData + clientMapHB = clientMap
    private final ConcurrentHashMap<HostInfo, NettyClient> clientMap = new ConcurrentHashMap<HostInfo, NettyClient>();

    private final ConcurrentHashMap<HostInfo, AtomicLong> lastBadHostMap = new ConcurrentHashMap<>();

    private final ArrayList<NettyClient> clientList = new ArrayList<NettyClient>();
    private List<HostInfo> proxyInfoList = new ArrayList<HostInfo>();

    //    private final Map<HostInfo, Integer> channelLoadMap = new ConcurrentHashMap<HostInfo, Integer>();
    private final Map<HostInfo, int[]> channelLoadMapData = new ConcurrentHashMap<HostInfo, int[]>();
    private final Map<HostInfo, int[]> channelLoadMapHB = new ConcurrentHashMap<HostInfo, int[]>();


    private ClientBootstrap bootstrap;
    private int currentIndex = 0;
    private ProxyClientConfig configure;
    private Sender sender;
    private int aliveConnections;
    private int realSize;
    //    private ConnectionCheckThread connectionCheckThread;
    private SendHBThread sendHBThread;
    private ProxyConfigManager ipManager;

    private int bidNum = 0;
    private String bid = "";
    private Map<String, Integer> tidMap = new HashMap<String, Integer>();
    private int loadThreshold;
    private int loadCycle = 0;
    private static final int[] weight = {
            1, 1, 1, 1, 1,
            2, 2, 2, 2, 2,
            3, 3, 3, 3, 3,
            6, 6, 6, 6, 6,
            12, 12, 12, 12, 12,
            48, 96, 192, 384, 1000};
//    private static final int total_weight = 240;
    /**
     * Lock to protect FSNamesystem.
     */
    private final ReentrantReadWriteLock fsLock = new ReentrantReadWriteLock(true);

    public int getLoadThreshold() {
        return loadThreshold;
    }

    public void setLoadThreshold(int loadThreshold) {
        this.loadThreshold = loadThreshold;
    }

    public int getBidNum() {
        return bidNum;
    }

    public void setBidNum(int bidNum) {
        this.bidNum = bidNum;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public Map<String, Integer> getTidMap() {
        return tidMap;
    }

    public void setTidMap(Map<String, Integer> tidMap) {
        this.tidMap = tidMap;
    }

    public EncryptConfigEntry getEncryptConfigEntry() {
        return this.ipManager.getEncryptConfigEntry(configure.getUserName());
    }

    public List<HostInfo> getProxyInfoList() {
        return proxyInfoList;
    }

    public int getAliveConnections() {
        return aliveConnections;
    }

    public void setAliveConnections(int aliveConnections) {
        this.aliveConnections = aliveConnections;
    }

    public void readLock() {
        this.fsLock.readLock().lock();
    }

    public void readUnlock() {
        this.fsLock.readLock().unlock();
    }

    public void writeLock() {
        this.fsLock.writeLock().lock();
    }

    public void writeLockInterruptibly() throws InterruptedException {
        this.fsLock.writeLock().lockInterruptibly();
    }

    public void writeUnlock() {
        this.fsLock.writeLock().unlock();
    }

    public boolean hasWriteLock() {
        return this.fsLock.isWriteLockedByCurrentThread();
    }

    public boolean hasReadLock() {
        return this.fsLock.getReadHoldCount() > 0;
    }

    public boolean hasReadOrWriteLock() {
        return hasReadLock() || hasWriteLock();
    }

    public ClientMgr(ProxyClientConfig configure, Sender sender) throws Exception {
        this(configure, sender, null);
    }

    // Build up the connection between the server and client.
    public ClientMgr(ProxyClientConfig configure, Sender sender, ChannelFactory selfDefineFactory) throws Exception {
        /* Initialize the bootstrap. */
        if (selfDefineFactory == null) {
            selfDefineFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool());
        }
        bootstrap = new ClientBootstrap(selfDefineFactory);
        bootstrap.setPipelineFactory(new ClientPipelineFactory(this, sender));
        bootstrap.setOption(ConfigConstants.RECEIVE_BUFFER_SIZE, ConfigConstants.DEFAULT_RECEIVE_BUFFER_SIZE);
        bootstrap.setOption(ConfigConstants.SEND_BUFFER_SIZE, ConfigConstants.DEFAULT_SEND_BUFFER_SIZE);
        if (configure.getNetTag().equals("bobcat")) {
            bootstrap.setOption("trafficClass", 96);
        }

        /* ready to Start the thread which refreshes the proxy list. */
        ipManager = new ProxyConfigManager(configure, Utils.getLocalIp(), this);
        ipManager.setName("proxyConfigManager");
        if (configure.getBid() != null) {
            ipManager.setBusinessId(configure.getBid());
            bid = configure.getBid();
        }

        /*
         * Request the IP before starting, so that we already have three
         * connections.
         */
        this.configure = configure;
        this.sender = sender;
        this.aliveConnections = configure.getAliveConnections();

        try {
            ipManager.doProxyEntryQueryWork();
        } catch (IOException e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        }
        ipManager.setDaemon(true);
        ipManager.start();

        this.sendHBThread = new SendHBThread();
        this.sendHBThread.setName("SendHBThread");
        this.sendHBThread.start();
    }

    public ProxyConfigEntry getBidConfigureInfo() throws Exception {
        return ipManager.getBidConfigure();
    }

    private boolean initConnection(HostInfo host) {
        NettyClient client = clientMap.get(host);
        if (client != null && client.isActive()) {
            logger.info("this client {} has open!", host.getHostName());
            throw new IllegalStateException(
                    "The channel has already been opened");
        }
        client = new NettyClient(bootstrap, host.getHostName(),
                host.getPortNumber(), configure);
        boolean bSuccess = client.connect();

        if (clientMapData.size() < aliveConnections) {

            if (bSuccess) {
                clientMapData.put(host, client);
                clientList.add(client);
                clientMap.put(host, client);
                logger.info("build a connection success! {},channel {}", host.getHostName(), client.getChannel());
            } else {
                logger.info("build a connection fail! {}", host.getHostName());
            }
            logger.info("client map size {},client list size {}", clientMapData.size(), clientList.size());
        } else {

            if (bSuccess) {
                clientMapHB.put(host, client);
                clientMap.put(host, client);
                logger.info("build a HBconnection success! {},channel {}", host.getHostName(), client.getChannel());
            } else {
                logger.info("build a HBconnection fail! {}", host.getHostName());
            }
        }
        return bSuccess;
    }

    public void resetClient(Channel channel) {
        if (channel == null) {
            return;
        }
        logger.info("reset this channel {}", channel);
        for (HostInfo hostInfo : clientMap.keySet()) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMap.get(hostInfo);
            if (client != null && client.getChannel() != null
                    && client.getChannel().getId().equals(channel.getId())) {
                client.reconnect();
                break;
            }
        }
    }

    public void setConnectionFrozen(Channel channel) {
        if (channel == null) {
            return;
        }
        logger.info("set this channel {} frozen", channel);
        for (HostInfo hostInfo : clientMap.keySet()) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMap.get(hostInfo);
            if (client != null && client.getChannel() != null
                    && client.getChannel().getId().equals(channel.getId())) {
                client.setFrozen();
                logger.info("end to froze this channel {}", client.getChannel().toString());
                break;
            }
        }
    }

    public void setConnectionBusy(Channel channel) {
        if (channel == null) {
            return;
        }
        logger.info("set this channel {} busy", channel);
        for (HostInfo hostInfo : clientMap.keySet()) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMap.get(hostInfo);
            if (client != null && client.getChannel() != null
                    && client.getChannel().getId().equals(channel.getId())) {
                client.setBusy();
                break;
            }
        }
    }

    public synchronized NettyClient getClientByRoundRobin() {
        NettyClient client = null;
        if (clientList.isEmpty()) {
            return null;
        }
        int currSize = clientList.size();
        for (int retryTime = 0; retryTime < currSize; retryTime++) {
            currentIndex = (++currentIndex) % currSize;
            client = clientList.get(currentIndex);
            if (client != null && client.isActive()) {
                break;
            }
        }
        if (client == null || !client.isActive()) {
            return null;
        }
        //logger.info("get a client {}", client.getChannel());
        return client;
    }

    public NettyClient getContainProxy(String proxyip) {
        if (proxyip == null) {
            return null;
        }
        for (NettyClient tmpClient : clientList) {
            if (tmpClient != null && tmpClient.getServerIP() != null && tmpClient.getServerIP().equals(proxyip)) {
                return tmpClient;
            }
        }
        return null;
    }

    public void shutDown() {
        bootstrap.releaseExternalResources();
//        bootstrap.shutdown();

        ipManager.shutDown();

//        connectionCheckThread.shutDown();
        sendHBThread.shutDown();
        closeAllConnection();

    }

    private void closeAllConnection() {
        if (!clientMap.isEmpty()) {
            logger.info("ready to close all connections!");
            for (HostInfo hostInfo : clientMap.keySet()) {
                if (hostInfo == null) {
                    continue;
                }
                NettyClient client = clientMap.get(hostInfo);
                if (client != null && client.isActive()) {
                    sender.waitForAckForChannel(client.getChannel());
                    client.close();
                }
            }
        }
        clientMap.clear();
        clientMapData.clear();
        clientMapHB.clear();

        channelLoadMapData.clear();
        channelLoadMapHB.clear();
        clientList.clear();
        sender.clearCallBack();
    }

    private void updateAllConnection(List<HostInfo> hostInfos) {
        closeAllConnection();
        /* Build new channels*/
        for (HostInfo hostInfo : hostInfos) {
            initConnection(hostInfo);
        }
    }

    public void notifyHBAck(Channel channel, short loadvalue) {
        try {
            if (loadvalue == (-1) || loadCycle == 0) {
                return;
            } else {
                for (Map.Entry<HostInfo, NettyClient> entry : clientMapData.entrySet()) {
                    NettyClient client = entry.getValue();
                    HostInfo hostInfo = entry.getKey();
                    if (client != null && client.getChannel() != null
                            && client.getChannel().getId().equals(channel.getId())) {
//                        logger.info("channel" + channel + "; Load:" + load);
                        if (!channelLoadMapData.containsKey(hostInfo)) {
                            channelLoadMapData.put(hostInfo, new int[ConfigConstants.CYCLE]);
                        }
                        if ((loadCycle - 1) >= 0) {
                            channelLoadMapData.get(hostInfo)[loadCycle - 1] = loadvalue;
                        } else {
                            return;
                        }
                        break;
                    }
                }

                for (Map.Entry<HostInfo, NettyClient> entry : clientMapHB.entrySet()) {
                    NettyClient client = entry.getValue();
                    HostInfo hostInfo = entry.getKey();
                    if (client != null && client.getChannel() != null
                            && client.getChannel().getId().equals(channel.getId())) {
//                        logger.info("HBchannel" + channel + "; Load:" + load);
                        if (!channelLoadMapHB.containsKey(hostInfo)) {
                            channelLoadMapHB.put(hostInfo, new int[ConfigConstants.CYCLE]);
                        }
                        if ((loadCycle - 1) >= 0) {
                            channelLoadMapHB.get(hostInfo)[loadCycle - 1] = loadvalue;
                        } else {
                            return;
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("{} , {}", e.toString(), e.getStackTrace());
        }
    }


    private void loadDataInfo(Map<HostInfo, Integer> loadData) {
        for (Map.Entry<HostInfo, int[]> entry : channelLoadMapData.entrySet()) {
            HostInfo key = entry.getKey();
            int[] value = entry.getValue();
            int numerator = 0;
            int denominator = 0;
            for (int i = 0; i < value.length; i++) {
                if (value[i] > 0) {
                    numerator = numerator + value[i] * weight[i];
                    denominator = denominator + weight[i];
                }
            }
            int sum = numerator / denominator;
            loadData.put(key, sum);
        }
    }

    private void loadHBInfo(Map<HostInfo, Integer> loadHB) {
        for (Map.Entry<HostInfo, int[]> entry : channelLoadMapHB.entrySet()) {
            HostInfo key = entry.getKey();
            int[] value = entry.getValue();
            int numerator = 0;
            int denominator = 0;
            for (int i = 0; i < value.length; i++) {
                if (value[i] > 0) {
                    numerator = numerator + value[i] * weight[i];
                    denominator = denominator + weight[i];
                }
            }
            int sum = numerator / denominator;
            loadHB.put(key, sum);
        }
    }

    public void notifyHBControl() {
        try {
            writeLock();
            logger.info("check if there is need to start balancing!");

            Map<HostInfo, Integer> loadData = new ConcurrentHashMap<HostInfo, Integer>();
            Map<HostInfo, Integer> loadHB = new ConcurrentHashMap<HostInfo, Integer>();
            loadDataInfo(loadData);
            loadHBInfo(loadHB);

            List<Map.Entry<HostInfo, Integer>> listData = new ArrayList<>(loadData.entrySet());
            Collections.sort(listData, new Comparator<Map.Entry<HostInfo, Integer>>() {
                @Override
                public int compare(Map.Entry<HostInfo, Integer> o1, Map.Entry<HostInfo, Integer> o2) {
                    if (o2.getValue() != null && o1.getValue() != null && o1.getValue() > o2.getValue()) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
            List<Map.Entry<HostInfo, Integer>> listHB = new ArrayList<>(loadHB.entrySet());
            Collections.sort(listHB, new Comparator<Map.Entry<HostInfo, Integer>>() {
                @Override
                public int compare(Map.Entry<HostInfo, Integer> o1, Map.Entry<HostInfo, Integer> o2) {
                    if (o2.getValue() != null && o1.getValue() != null && o2.getValue() > o1.getValue()) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });


            logger.info("show info: last compute result!");
            for (Map.Entry<HostInfo, Integer> item : listData) {
//                System.out.println("listData:"+listData.get(i));
                logger.info("Client:" + item.getKey() + ";" + item.getValue());
            }
            for (Map.Entry<HostInfo, Integer> item : listHB) {
//                System.out.println("listHB:"+listHB.get(i));
                logger.info("HBClient:" + item.getKey() + ";" + item.getValue());
            }
            boolean isLoadSwitch = false;

//            int smallSize = listData.size() < listHB.size() ? listData.size() : listHB.size();
            int smallSize = 1;
            for (int i = 0; i < smallSize; i++) {
                if ((listData.get(i).getValue() - listHB.get(i).getValue()) >= this.loadThreshold) {
                    isLoadSwitch = true;
                    HostInfo dataHost = listData.get(i).getKey();
                    HostInfo hbHost = listHB.get(i).getKey();
                    logger.info("balancing client:" + dataHost.getHostName() + ",load: " + listData.get(i).getValue()
                            + "; HBclient:" + hbHost.getHostName() + ",load: " + listHB.get(i).getValue());

                    NettyClient client = clientMapData.get(dataHost);
                    client.setFrozen();
                    sender.waitForAckForChannel(client.getChannel());
                    client.close();

                    clientList.remove(clientMapData.get(dataHost));
                    clientMap.remove(dataHost);
                    clientMapData.remove(dataHost);
//                    channelLoadMapData.remove(dataHost);
                    clientMapData.put(hbHost, clientMapHB.get(hbHost));
//                    channelLoadMapData.put(hbHost,listHB.get(i).getValue());
                    clientList.add(clientMapHB.get(hbHost));
                    clientMapHB.remove(hbHost);
                }
            }


            if (!isLoadSwitch) {
                logger.info("Choose other HBClient because there is no load balancing! ");
            }
            for (Map.Entry<HostInfo, NettyClient> entry : clientMapHB.entrySet()) {
                entry.getValue().close();
                clientMap.remove(entry.getKey());
            }
            clientMapHB.clear();

            int realSize = this.realSize - clientMap.size();
            if (realSize > 0) {
                List<HostInfo> hostInfoList = new ArrayList<>(proxyInfoList);
                hostInfoList.removeAll(clientMap.keySet());
                List<HostInfo> replaceHost = getRealHosts(hostInfoList, realSize);
                for (HostInfo hostInfo : replaceHost) {
                    initConnection(hostInfo);
                }
            }
        } catch (Exception e) {
            logger.error("notifyHBcontrol", e);
        } finally {
            writeUnlock();
        }
    }

    private void sendHeartBeat() {
        for (HostInfo hostInfo : clientMap.keySet()) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMap.get(hostInfo);
            if (client == null) {
                continue;
            }
            try {
                if (client.isActive()) {
                    //logger.info("active host to send heartbeat! {}", entry.getKey().getHostName());
                    EncodeObject encodeObject = new EncodeObject("heartbeat".getBytes(StandardCharsets.UTF_8),
                            8, false, false, false, System.currentTimeMillis() / 1000, 1, "", "", "");
                    if (configure.isNeedAuthentication()) {
                        encodeObject.setAuth(configure.isNeedAuthentication(),
                                configure.getUserName(), configure.getSecretKey());
                    }
                    client.write(encodeObject);
                }
            } catch (Throwable e) {
                logger.error("sendHeartBeat to " + hostInfo.getReferenceName()
                        + " exception {}, {}", e.toString(), e.getStackTrace());
            }
        }
    }

    /**
     * fill up client with hb client
     */
    private void fillUpWorkClientWithHBClient() {
        if (clientMapHB.size() > 0) {
            logger.info("fill up work client with HB, clientMapData {}, clientMapHB {}",
                    clientMapData.size(), clientMapHB.size());
        }
        Iterator<Map.Entry<HostInfo, NettyClient>> it = clientMapHB.entrySet().iterator();
        while (it.hasNext() && clientMapData.size() < aliveConnections) {
            Map.Entry<HostInfo, NettyClient> entry = it.next();
            clientMapData.put(entry.getKey(), entry.getValue());
            clientList.add(entry.getValue());
            channelLoadMapHB.remove(entry.getKey());
            it.remove();
        }
    }

    private void fillUpWorkClientWithLastBadClient() {


        int currentRealSize = aliveConnections - clientMapData.size();

        List<HostInfo> pendingBadList = new ArrayList<>();
        for (Map.Entry<HostInfo, AtomicLong> entry : lastBadHostMap.entrySet()) {
            if (pendingBadList.size() < currentRealSize) {
                pendingBadList.add(entry.getKey());
            } else {
                for (int index = 0; index < pendingBadList.size(); index++) {

                    if (entry.getValue().get() < lastBadHostMap
                            .get(pendingBadList.get(index)).get()) {
                        pendingBadList.set(index, entry.getKey());
                    }
                }
            }
        }
        List<HostInfo> replaceHostLists = getRealHosts(pendingBadList, currentRealSize);
        if (replaceHostLists.size() > 0) {
            logger.info("replace bad connection, use last bad list, "
                            + "last bad list {}, client Map data {}",
                    lastBadHostMap.size(), clientMapData.size());
        }
        for (HostInfo hostInfo : replaceHostLists) {

            boolean isSuccess = initConnection(hostInfo);

            if (isSuccess) {
                lastBadHostMap.remove(hostInfo);
            }
        }
    }

    private void removeBadRealClient(List<HostInfo> badHostLists, List<HostInfo> normalHostLists) {
        for (HostInfo hostInfo : clientMapData.keySet()) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMapData.get(hostInfo);
            if (client == null || !client.isActive()) {
                logger.info("this host {} is bad! so remove it", hostInfo.getHostName());
                badHostLists.add(hostInfo);
            } else {
                logger.info("this host {} is active! so keep it", hostInfo.getHostName());
                normalHostLists.add(hostInfo);
            }
        }
    }


    private void removeBadHBClients(List<HostInfo> badHostLists, List<HostInfo> normalHostLists) {
        for (HostInfo hostInfo : clientMapHB.keySet()) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMapHB.get(hostInfo);
            if (client == null || !client.isActive()) {
                logger.info("this HBhost {} is bad! so remove it", hostInfo.getHostName());
                badHostLists.add(hostInfo);
            } else {
                logger.info("this HBhost {} is active! so keep it", hostInfo.getHostName());
                normalHostLists.add(hostInfo);
            }
        }
    }

    private void removeBadClients(List<HostInfo> badHostLists) {
        for (HostInfo hostInfo : badHostLists) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMapData.get(hostInfo);
            if (client != null) {
                sender.waitForAckForChannel(client.getChannel());
                client.close();
                clientMapData.remove(hostInfo);
                clientMap.remove(hostInfo);
                clientList.remove(client);

                channelLoadMapData.remove(hostInfo);
                logger.info("remove this client {}", hostInfo.getHostName());
            }
            client = clientMapHB.get(hostInfo);
            if (client != null) {
                clientMapHB.get(hostInfo).close();
                clientMapHB.remove(hostInfo);
                clientMap.remove(hostInfo);

                channelLoadMapHB.remove(hostInfo);
                logger.info("remove this HBclient {}", hostInfo.getHostName());
            }
        }
    }


    public void replaceBadConnectionHB() {
        try {
            writeLock();

            List<HostInfo> badHostLists = new ArrayList<>();
            List<HostInfo> normalHostLists = new ArrayList<>();
            removeBadRealClient(badHostLists, normalHostLists);
            removeBadHBClients(badHostLists, normalHostLists);
            removeBadClients(badHostLists);

            if (badHostLists.size() == 0 && normalHostLists.size() != 0 && clientMapData.size() >= aliveConnections) {
                logger.info("hasn't bad host! so keep it");
                if (loadCycle >= ConfigConstants.CYCLE) {
                    if (loadThreshold == 0) {
                        logger.info("the proxy cluster is being updated!");
                    } else if (clientMapHB.size() != 0 && clientMapData.size() != 0) {
                        notifyHBControl();
                    } else if (this.realSize != clientMap.size()) {
                        logger.info("make the amount of proxy to original value");
                        int realSize = this.realSize - clientMap.size();
                        if (realSize > 0) {
                            List<HostInfo> hostInfoList = new ArrayList<>(proxyInfoList);
                            hostInfoList.removeAll(clientMap.keySet());
                            List<HostInfo> replaceHost = getRealHosts(hostInfoList, realSize);
                            for (HostInfo hostInfo : replaceHost) {
                                initConnection(hostInfo);
                            }
                        }
                    }
                    loadCycle = 0;
                    channelLoadMapData.clear();
                    channelLoadMapHB.clear();
                }
                return;
            } else {
                loadCycle = 0;
                channelLoadMapData.clear();
                channelLoadMapHB.clear();
            }

            List<HostInfo> hostLists = new ArrayList<HostInfo>(this.proxyInfoList);
            hostLists.removeAll(badHostLists);
            hostLists.removeAll(lastBadHostMap.keySet());
            hostLists.removeAll(normalHostLists);

            int realSize = this.realSize - clientMap.size();
            if (realSize > hostLists.size()) {
                realSize = hostLists.size();
            }

            if (realSize != 0) {
                List<HostInfo> replaceHostLists = getRealHosts(hostLists, realSize);
                /* Build new channels.*/
                for (HostInfo hostInfo : replaceHostLists) {
                    initConnection(hostInfo);
                }
            }


            if (clientMapData.size() < aliveConnections) {
                fillUpWorkClientWithHBClient();
            }


            if (clientMapData.size() < aliveConnections) {
                fillUpWorkClientWithLastBadClient();
            }


            for (HostInfo hostInfo : badHostLists) {
                AtomicLong tmpValue = new AtomicLong(0);
                AtomicLong hostValue = lastBadHostMap.putIfAbsent(hostInfo, tmpValue);
                if (hostValue == null) {
                    hostValue = tmpValue;
                }
                hostValue.incrementAndGet();
            }

            for (HostInfo hostInfo : normalHostLists) {
                lastBadHostMap.remove(hostInfo);
            }

            logger.info(
                    "replace bad connection ,client map size {},client list size {}",
                    clientMapData.size(), clientList.size());


        } catch (Exception e) {
            logger.error("replaceBadConnection exception {}, {}", e.toString(), e.getStackTrace());
        } finally {
            writeUnlock();
        }

    }

    public void setProxyInfoList(List<HostInfo> proxyInfoList) {
        try {
            /* Close and remove old client. */
            writeLock();
            this.proxyInfoList = proxyInfoList;

            if (loadThreshold == 0) {
                if (aliveConnections >= proxyInfoList.size()) {
                    realSize = proxyInfoList.size();
                    aliveConnections = realSize;
                    logger.error("there is no enough proxy to work!");
                } else {
                    realSize = aliveConnections;
                }
            } else {
                if (aliveConnections >= proxyInfoList.size()) {
                    realSize = proxyInfoList.size();
                    aliveConnections = realSize;
                    logger.error("there is no idle proxy to choose for balancing!");
                } else if ((aliveConnections + 4) > proxyInfoList.size()) {
                    realSize = proxyInfoList.size();
                    logger.warn("there is only {} idle proxy to choose for balancing!",
                            proxyInfoList.size() - aliveConnections);
                } else {
                    realSize = aliveConnections + 4;
                }
            }

            List<HostInfo> hostInfos = getRealHosts(proxyInfoList, realSize);

            /* Refresh the current channel connections. */
            updateAllConnection(hostInfos);

            logger.info(
                    "update all connection ,client map size {},client list size {}",
                    clientMapData.size(), clientList.size());

        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            writeUnlock();
        }
    }

    private List<HostInfo> getRealHosts(List<HostInfo> hostList, int realSize) {
        if (realSize > hostList.size()) {
            return hostList;
        }
        Collections.shuffle(hostList);
        List<HostInfo> resultHosts = new ArrayList<HostInfo>(realSize);
        for (int i = 0; i < realSize; i++) {
            resultHosts.add(hostList.get(i));
            logger.info("host={}", hostList.get(i));
        }
        return resultHosts;
    }


    private class SendHBThread extends Thread {

        private boolean bShutDown = false;
        private final int[] random = {17, 19, 23, 31, 37};

        public SendHBThread() {
            bShutDown = false;
        }

        public void shutDown() {
            logger.info("begin to shut down SendHBThread!");
            bShutDown = true;
        }

        @Override
        public void run() {
            while (!bShutDown) {
                try {
                    loadCycle++;
                    if (!clientMapHB.isEmpty()) {
                        sendHeartBeat();
                    }
                    replaceBadConnectionHB();
                    try {
                        int index = (int) (Math.random() * random.length);
                        Thread.sleep((random[index]) * 1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        logger.error(e.toString());
                    }
                } catch (Throwable e) {
                    logger.error("SendHBThread throw exception: ", e);
                }
            }
        }
    }


}
