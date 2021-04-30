/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tubemq.server.master.metamanage;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.tubemq.corebase.TBaseConstants;
import org.apache.tubemq.corebase.TErrCodeConstants;
import org.apache.tubemq.corebase.TokenConstants;
import org.apache.tubemq.corebase.cluster.TopicInfo;
import org.apache.tubemq.corebase.utils.KeyBuilderUtils;
import org.apache.tubemq.corebase.utils.TStringUtils;
import org.apache.tubemq.corebase.utils.Tuple2;
import org.apache.tubemq.server.Server;
import org.apache.tubemq.server.common.TServerConstants;
import org.apache.tubemq.server.common.TStatusConstants;
import org.apache.tubemq.server.common.fileconfig.MasterReplicationConfig;
import org.apache.tubemq.server.common.statusdef.ManageStatus;
import org.apache.tubemq.server.common.statusdef.TopicStatus;
import org.apache.tubemq.server.common.statusdef.TopicStsChgType;
import org.apache.tubemq.server.common.utils.ProcessResult;
import org.apache.tubemq.server.common.utils.WebParameterUtils;
import org.apache.tubemq.server.master.bdbstore.MasterGroupStatus;
import org.apache.tubemq.server.master.metamanage.metastore.BdbMetaStoreServiceImpl;
import org.apache.tubemq.server.master.metamanage.metastore.MetaStoreService;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.GroupConsumeCtrlEntity;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.TopicCtrlEntity;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.TopicPropGroup;
import org.apache.tubemq.server.master.nodemanage.nodebroker.BrokerSyncStatusInfo;
import org.apache.tubemq.server.master.web.handler.BrokerProcessResult;
import org.apache.tubemq.server.master.web.handler.GroupProcessResult;
import org.apache.tubemq.server.master.web.handler.TopicProcessResult;
import org.apache.tubemq.server.master.web.model.ClusterGroupVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class MetaDataManager implements Server {

    private static final Logger logger =
            LoggerFactory.getLogger(MetaDataManager.class);
    private static final ClusterSettingEntity defClusterSetting =
            new ClusterSettingEntity().fillDefaultValue();
    private final MasterReplicationConfig replicationConfig;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ConcurrentHashMap<Integer, String> brokersMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> brokersTLSMap =
            new ConcurrentHashMap<>();

    private final MasterGroupStatus masterGroupStatus = new MasterGroupStatus();

    private ConcurrentHashMap<Integer/* brokerId */, BrokerSyncStatusInfo> brokerRunSyncManageMap =
            new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer/* brokerId */, ConcurrentHashMap<String/* topicName */, TopicInfo>>
            brokerRunTopicInfoStoreMap = new ConcurrentHashMap<>();
    private volatile boolean isStarted = false;
    private volatile boolean isStopped = false;
    private MetaStoreService metaStoreService;
    private AtomicLong brokerInfoCheckSum = new AtomicLong(System.currentTimeMillis());
    private long lastBrokerUpdatedTime = System.currentTimeMillis();
    private long serviceStartTime = System.currentTimeMillis();


    public MetaDataManager(String nodeHost, String metaDataPath,
                           MasterReplicationConfig replicationConfig) {
        this.replicationConfig = replicationConfig;
        this.metaStoreService =
                new BdbMetaStoreServiceImpl(nodeHost, metaDataPath, replicationConfig);

        this.scheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "Master Status Check");
                    }
                });
    }

    @Override
    public void start() throws Exception {
        if (isStarted) {
            return;
        }
        // start meta store service
        this.metaStoreService.start();
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    MasterGroupStatus tmpGroupStatus =
                            metaStoreService.getMasterGroupStatus(true);
                    if (tmpGroupStatus == null) {
                        masterGroupStatus.setMasterGroupStatus(false, false, false);
                    } else {
                        masterGroupStatus.setMasterGroupStatus(metaStoreService.isMasterNow(),
                                tmpGroupStatus.isWritable(), tmpGroupStatus.isReadable());
                    }
                } catch (Throwable e) {
                    logger.error(new StringBuilder(512)
                            .append("BDBGroupStatus Check exception, wait ")
                            .append(replicationConfig.getRepStatusCheckTimeoutMs())
                            .append(" ms to try again.").append(e.getMessage()).toString());
                }
            }
        }, 0, replicationConfig.getRepStatusCheckTimeoutMs(), TimeUnit.MILLISECONDS);
        // initial running data
        StringBuilder sBuffer = new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE);
        Map<Integer, BrokerConfEntity> curBrokerConfInfo =
                this.metaStoreService.getBrokerConfInfo(null);
        for (BrokerConfEntity entity : curBrokerConfInfo.values()) {
            updateBrokerMaps(entity);
            if (entity.getManageStatus().isApplied()) {
                boolean needFastStart = false;
                BrokerSyncStatusInfo brokerSyncStatusInfo =
                        this.brokerRunSyncManageMap.get(entity.getBrokerId());
                List<String> brokerTopicSetConfInfo = getBrokerTopicStrConfigInfo(entity, sBuffer);
                if (brokerSyncStatusInfo == null) {
                    brokerSyncStatusInfo =
                            new BrokerSyncStatusInfo(entity, brokerTopicSetConfInfo);
                    BrokerSyncStatusInfo tmpBrokerSyncStatusInfo =
                            brokerRunSyncManageMap.putIfAbsent(entity.getBrokerId(),
                                    brokerSyncStatusInfo);
                    if (tmpBrokerSyncStatusInfo != null) {
                        brokerSyncStatusInfo = tmpBrokerSyncStatusInfo;
                    }
                }
                if (brokerTopicSetConfInfo.isEmpty()) {
                    needFastStart = true;
                }
                brokerSyncStatusInfo.setFastStart(needFastStart);
                brokerSyncStatusInfo.updateCurrBrokerConfInfo(entity.getManageStatus().getCode(),
                        entity.isConfDataUpdated(), entity.isBrokerLoaded(),
                        entity.getBrokerDefaultConfInfo(), brokerTopicSetConfInfo, false);
            }
        }
        isStarted = true;
        serviceStartTime = System.currentTimeMillis();
        logger.info("BrokerConfManager StoreService Started");
    }

    @Override
    public void stop() throws Exception {
        if (isStopped) {
            return;
        }
        this.scheduledExecutorService.shutdownNow();
        isStopped = true;
        logger.info("BrokerConfManager StoreService stopped");
    }


    /**
     * If this node is the master role
     *
     * @return true if is master role or else
     */
    public boolean isSelfMaster() {
        return metaStoreService.isMasterNow();
    }

    public boolean isPrimaryNodeActive() {
        return metaStoreService.isPrimaryNodeActive();
    }

    /**
     * Transfer master role to replica node
     *
     * @throws Exception
     */
    public void transferMaster() throws Exception {
        if (metaStoreService.isMasterNow()
                && !metaStoreService.isPrimaryNodeActive()) {
            metaStoreService.transferMaster();
        }
    }

    public void clearBrokerRunSyncManageData() {
        if (!this.isStarted
                && this.isStopped) {
            return;
        }
        this.brokerRunSyncManageMap.clear();
    }

    public InetSocketAddress getMasterAddress() {
        return metaStoreService.getMasterAddress();
    }

    public ClusterGroupVO getGroupAddressStrInfo() {
        return metaStoreService.getGroupAddressStrInfo();
    }



    public long getBrokerInfoCheckSum() {
        return this.brokerInfoCheckSum.get();
    }

    public ConcurrentHashMap<Integer, String> getBrokersMap(boolean isOverTLS) {
        if (isOverTLS) {
            return brokersTLSMap;
        } else {
            return brokersMap;
        }
    }

    /**
     * Check if consume target is authorization or not
     *
     * @param consumerId
     * @param groupName
     * @param reqTopicSet
     * @param reqTopicCondMap
     * @param sBuffer
     * @return
     */
    public boolean isConsumeTargetAuthorized(String consumerId, String groupName,
                                             Set<String> reqTopicSet,
                                             Map<String, TreeSet<String>> reqTopicCondMap,
                                             StringBuilder sBuffer, ProcessResult result) {
        // check topic set
        if ((reqTopicSet == null) || (reqTopicSet.isEmpty())) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "Request miss necessary subscribed topic data");
            return result.isSuccess();
        }
        if ((reqTopicCondMap != null) && (!reqTopicCondMap.isEmpty())) {
            // check if request topic set is all in the filter topic set
            Set<String> condTopics = reqTopicCondMap.keySet();
            List<String> unSetTopic = new ArrayList<>();
            for (String topic : condTopics) {
                if (!reqTopicSet.contains(topic)) {
                    unSetTopic.add(topic);
                }
            }
            if (!unSetTopic.isEmpty()) {
                result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                        sBuffer.append("Filter's Topic not subscribed :")
                                .append(unSetTopic).toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
        }
        // check if group enable consume
        Set<String> disableCsmTopicSet = new HashSet<>();
        Set<String> enableFltCsmTopicSet = new HashSet<>();
        for (String topicItem : reqTopicSet) {
            if (TStringUtils.isBlank(topicItem)) {
                continue;
            }
            TopicCtrlEntity topicEntity = metaStoreService.getTopicCtrlConf(topicItem);
            if (topicEntity == null) {
                continue;
            }
            if (topicEntity.isAuthCtrlEnable()) {
                // check if consume group is allowed to consume
                GroupConsumeCtrlEntity ctrlEntity =
                        metaStoreService.getConsumeCtrlByGroupAndTopic(groupName, topicItem);
                if (ctrlEntity == null || !ctrlEntity.isEnableConsume()) {
                    disableCsmTopicSet.add(topicItem);
                }
                // check if consume group is required filter consume
                if (ctrlEntity.isEnableFilterConsume()) {
                    enableFltCsmTopicSet.add(topicItem);
                }
            }
        }
        if (!disableCsmTopicSet.isEmpty()) {
            result.setFailResult(TErrCodeConstants.CONSUME_GROUP_FORBIDDEN,
                    sBuffer.append("[unAuthorized Group] ").append(consumerId)
                            .append("'s consumerGroup not authorized by administrator, unAuthorizedTopics : ")
                            .append(disableCsmTopicSet).toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        // check if group enable filter consume
        return checkFilterRstrTopics(groupName, consumerId,
                enableFltCsmTopicSet, reqTopicCondMap, sBuffer, result);
    }

    private boolean checkConsumeRstrTopics(final String groupName, final String consumerId,
                                          Set<String> enableFltCsmTopicSet,
                                          Map<String, TreeSet<String>> reqTopicCondMap,
                                          StringBuilder sBuffer, ProcessResult result) {
        if (enableFltCsmTopicSet == null && enableFltCsmTopicSet.isEmpty()) {
            result.setSuccResult("Ok!");
            return result.isSuccess();
        }
        GroupConsumeCtrlEntity ctrlEntity;
        for (String topicName : enableFltCsmTopicSet) {
            ctrlEntity =
                    metaStoreService.getConsumeCtrlByGroupAndTopic(groupName, topicName);
            if (ctrlEntity == null || !ctrlEntity.isEnableFilterConsume()) {
                continue;
            }
            String allowedCondStr = ctrlEntity.getFilterCondStr();
            if (allowedCondStr.length() == 2
                    && allowedCondStr.equals(TServerConstants.BLANK_FILTER_ITEM_STR)) {
                result.setFailResult(TErrCodeConstants.CONSUME_CONTENT_FORBIDDEN,
                        sBuffer.append("[Restricted Group] ").append(consumerId)
                                .append(" : ").append(groupName)
                                .append(" not allowed to consume any data of topic ")
                                .append(topicName).toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
            TreeSet<String> condItemSet = reqTopicCondMap.get(topicName);
            if (condItemSet == null || condItemSet.isEmpty()) {
                result.setFailResult(TErrCodeConstants.CONSUME_CONTENT_FORBIDDEN,
                        sBuffer.append("[Restricted Group] ").append(consumerId)
                                .append(" : ").append(groupName)
                                .append(" must set the filter conditions of topic ")
                                .append(topicName).toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
            Map<String, List<String>> unAuthorizedCondMap = new HashMap<>();
            for (String item : condItemSet) {
                if (!allowedCondStr.contains(sBuffer.append(TokenConstants.ARRAY_SEP)
                        .append(item).append(TokenConstants.ARRAY_SEP).toString())) {
                    List<String> unAuthConds = unAuthorizedCondMap.get(topicName);
                    if (unAuthConds == null) {
                        unAuthConds = new ArrayList<>();
                        unAuthorizedCondMap.put(topicName, unAuthConds);
                    }
                    unAuthConds.add(item);
                }
                sBuffer.delete(0, sBuffer.length());
            }
            if (!unAuthorizedCondMap.isEmpty()) {
                result.setFailResult(TErrCodeConstants.CONSUME_CONTENT_FORBIDDEN,
                        sBuffer.append("[Restricted Group] ").append(consumerId)
                                .append(" : unAuthorized filter conditions ")
                                .append(unAuthorizedCondMap).toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
        }
        result.setSuccResult("Ok!");
        return result.isSuccess();
    }


    private boolean checkFilterRstrTopics(final String groupName, final String consumerId,
                                          Set<String> enableFltCsmTopicSet,
                                          Map<String, TreeSet<String>> reqTopicCondMap,
                                          StringBuilder sBuffer, ProcessResult result) {
        if (enableFltCsmTopicSet == null && enableFltCsmTopicSet.isEmpty()) {
            result.setSuccResult("Ok!");
            return result.isSuccess();
        }
        GroupConsumeCtrlEntity ctrlEntity;
        for (String topicName : enableFltCsmTopicSet) {
            ctrlEntity =
                    metaStoreService.getConsumeCtrlByGroupAndTopic(groupName, topicName);
            if (ctrlEntity == null || !ctrlEntity.isEnableFilterConsume()) {
                continue;
            }
            String allowedCondStr = ctrlEntity.getFilterCondStr();
            if (allowedCondStr.length() == 2
                    && allowedCondStr.equals(TServerConstants.BLANK_FILTER_ITEM_STR)) {
                result.setFailResult(TErrCodeConstants.CONSUME_CONTENT_FORBIDDEN,
                        sBuffer.append("[Restricted Group] ").append(consumerId)
                                .append(" : ").append(groupName)
                                .append(" not allowed to consume any data of topic ")
                                .append(topicName).toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
            TreeSet<String> condItemSet = reqTopicCondMap.get(topicName);
            if (condItemSet == null || condItemSet.isEmpty()) {
                result.setFailResult(TErrCodeConstants.CONSUME_CONTENT_FORBIDDEN,
                        sBuffer.append("[Restricted Group] ").append(consumerId)
                                .append(" : ").append(groupName)
                                .append(" must set the filter conditions of topic ")
                                .append(topicName).toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
            Map<String, List<String>> unAuthorizedCondMap = new HashMap<>();
            for (String item : condItemSet) {
                if (!allowedCondStr.contains(sBuffer.append(TokenConstants.ARRAY_SEP)
                        .append(item).append(TokenConstants.ARRAY_SEP).toString())) {
                    List<String> unAuthConds = unAuthorizedCondMap.get(topicName);
                    if (unAuthConds == null) {
                        unAuthConds = new ArrayList<>();
                        unAuthorizedCondMap.put(topicName, unAuthConds);
                    }
                    unAuthConds.add(item);
                }
                sBuffer.delete(0, sBuffer.length());
            }
            if (!unAuthorizedCondMap.isEmpty()) {
                result.setFailResult(TErrCodeConstants.CONSUME_CONTENT_FORBIDDEN,
                        sBuffer.append("[Restricted Group] ").append(consumerId)
                                .append(" : unAuthorized filter conditions ")
                                .append(unAuthorizedCondMap).toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
        }
        result.setSuccResult("Ok!");
        return result.isSuccess();
    }


    // ///////////////////////////////////////////////////////////////////////////////

    /**
     * Add broker configure information
     *
     * @param sBuffer   the print information string buffer
     * @param result     the process result return
     * @return true if success otherwise false
    */
    public BrokerProcessResult addOrUpdBrokerConfig(boolean isAddOp, BaseEntity opInfoEntity,
                                                    int brokerId, String brokerIp, int brokerPort,
                                                    int brokerTlsPort, int brokerWebPort,
                                                    int regionId, int groupId,
                                                    ManageStatus mngStatus,
                                                    TopicPropGroup topicProps,
                                                    StringBuilder sBuffer,
                                                    ProcessResult result) {
        BrokerConfEntity entity =
                new BrokerConfEntity(opInfoEntity, brokerId, brokerIp);
        entity.updModifyInfo(opInfoEntity.getDataVerId(), brokerPort,
                brokerTlsPort, brokerWebPort, regionId, groupId, mngStatus, topicProps);
        return addOrUpdBrokerConfig(isAddOp, entity, sBuffer, result);
    }

    public BrokerProcessResult addOrUpdBrokerConfig(boolean isAddOp, BrokerConfEntity entity,
                                                    StringBuilder sBuffer, ProcessResult result) {
        if (isAddOp) {
            if (metaStoreService.getBrokerConfByBrokerId(entity.getBrokerId()) == null &&
                    metaStoreService.getBrokerConfByBrokerIp(entity.getBrokerIp()) == null) {
                metaStoreService.addBrokerConf(entity, sBuffer, result);
            } else {
                result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                        sBuffer.append("Duplicated broker configure record! query index is :")
                                .append("brokerId=").append(entity.getBrokerId())
                                .append(",brokerIp=").append(entity.getBrokerIp()).toString());
                sBuffer.delete(0, sBuffer.length());
            }
        } else {
            BrokerConfEntity curEntity =
                    metaStoreService.getBrokerConfByBrokerId(entity.getBrokerId());
            if (curEntity == null) {
                result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                        sBuffer.append("Not found broker configure by brokerId=")
                                .append(entity.getBrokerId()).toString());
                sBuffer.delete(0, sBuffer.length());
            } else {
                BrokerConfEntity newEntity = curEntity.clone();
                newEntity.updBaseModifyInfo(entity);
                if (entity.updModifyInfo(entity.getDataVerId(), entity.getBrokerPort(),
                        entity.getBrokerTLSPort(), entity.getBrokerWebPort(),
                        entity.getRegionId(), entity.getGroupId(),
                        entity.getManageStatus(), entity.getTopicProps())) {
                    metaStoreService.updBrokerConf(newEntity, sBuffer, result);
                    // update broker configure change status
                    BrokerSyncStatusInfo brokerSyncStatusInfo =
                            getBrokerRunSyncStatusInfo(entity.getBrokerId());
                    if (result.isSuccess()) {
                        if (brokerSyncStatusInfo != null) {
                            updateBrokerConfChanged(entity.getBrokerId(),
                                    true, true, sBuffer, result);
                        }
                    }
                } else {
                    result.setSuccResult(null);
                }
            }
        }
        return new BrokerProcessResult(entity.getBrokerId(), entity.getBrokerIp(), result);
    }

    /**
     * Modify broker configure information
     *
     * @param entity     the broker configure entity will be update
     * @param strBuffer  the print information string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    public boolean modBrokerConfig(BrokerConfEntity entity,
                                   StringBuilder strBuffer,
                                   ProcessResult result) {
        metaStoreService.updBrokerConf(entity, strBuffer, result);
        return result.isSuccess();
    }


    /**
     * Delete broker configure information
     *
     * @param operator  operator
     * @param brokerId  need deleted broker id
     * @param strBuffer  the print information string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    public boolean confDelBrokerConfig(String operator,
                                       int brokerId,
                                       StringBuilder strBuffer,
                                       ProcessResult result) {
        if (!metaStoreService.checkStoreStatus(true, result)) {
            return result.isSuccess();
        }
        // valid configure status
        if (metaStoreService.hasConfiguredTopics(brokerId)) {
            result.setFailResult(DataOpErrCode.DERR_UNCLEANED.getCode(),
                    "The broker's topic configure uncleaned!");
            return result.isSuccess();
        }
        BrokerConfEntity curEntity =
                metaStoreService.getBrokerConfByBrokerId(brokerId);
        if (curEntity == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    "The broker configure not exist!");
            return result.isSuccess();
        }
        if (curEntity.getManageStatus().isOnlineStatus()) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    "Broker manage status is online, please offline first!");
            return result.isSuccess();
        }
        BrokerSyncStatusInfo brokerSyncStatusInfo =
                this.brokerRunSyncManageMap.get(curEntity.getBrokerId());
        if (brokerSyncStatusInfo != null) {
            if (brokerSyncStatusInfo.isBrokerRegister()
                    && (curEntity.getManageStatus() == ManageStatus.STATUS_MANAGE_OFFLINE
                    && brokerSyncStatusInfo.getBrokerRunStatus() != TStatusConstants.STATUS_SERVICE_UNDEFINED)) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        "Broker is processing offline event, please wait and try later!");
                return result.isSuccess();
            }
        }
        if (metaStoreService.delBrokerConf(operator, brokerId, strBuffer, result)) {
            this.brokerRunSyncManageMap.remove(brokerId);
            delBrokerRunData(brokerId);
        }
        return result.isSuccess();
    }

    /**
     * Get broker configure information
     *
     * @param qryEntity
     * @return broker configure information
     */
    public Map<Integer, BrokerConfEntity> confGetBrokerConfInfo(
            BrokerConfEntity qryEntity) {
        return metaStoreService.getBrokerConfInfo(qryEntity);
    }

    /**
     * Get broker configure information
     *
     * @param qryEntity
     * @return broker configure information
     */
    public Map<Integer, BrokerConfEntity> getBrokerConfInfo(Set<Integer> brokerIdSet,
                                                            Set<String> brokerIpSet,
                                                            BrokerConfEntity qryEntity) {
        return metaStoreService.getBrokerConfInfo(brokerIdSet, brokerIpSet, qryEntity);
    }

    /**
     * Change broker configure status
     *
     * @param opEntity      operator
     * @param brokerIdSet   need deleted broker id set
     * @param newMngStatus  manage status
     * @param sBuffer       the print information string buffer
     * @param result        the process result return
     * @return true if success otherwise false
     */
    public List<BrokerProcessResult> changeBrokerConfStatus(BaseEntity opEntity,
                                                            Set<Integer> brokerIdSet,
                                                            ManageStatus newMngStatus,
                                                            StringBuilder sBuffer,
                                                            ProcessResult result) {
        BrokerConfEntity curEntry;
        BrokerConfEntity newEntry;
        List<BrokerProcessResult> retInfo = new ArrayList<>();
        // check target broker configure's status
        for (Integer brokerId : brokerIdSet) {
            curEntry = metaStoreService.getBrokerConfByBrokerId(brokerId);
            if (curEntry == null) {
                result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                        "The broker configure not exist!");
                retInfo.add(new BrokerProcessResult(brokerId, "", result));
                continue;
            }
            if (curEntry.getManageStatus() == newMngStatus) {
                result.setSuccResult(null);
                retInfo.add(new BrokerProcessResult(brokerId, curEntry.getBrokerIp(), result));
                continue;
            }
            if (newMngStatus == ManageStatus.STATUS_MANAGE_OFFLINE) {
                if (curEntry.getManageStatus().getCode()
                        < ManageStatus.STATUS_MANAGE_ONLINE.getCode()) {
                    result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                            sBuffer.append("Broker by brokerId=").append(brokerId)
                                    .append(" on draft status, not need offline operate!")
                                    .toString());
                    sBuffer.delete(0, sBuffer.length());
                    retInfo.add(new BrokerProcessResult(brokerId, "", result));
                    continue;
                }
            }
            newEntry = curEntry.clone();
            newEntry.updBaseModifyInfo(opEntity);
            if (newEntry.updModifyInfo(opEntity.getDataVerId(),
                    TBaseConstants.META_VALUE_UNDEFINED, TBaseConstants.META_VALUE_UNDEFINED,
                    TBaseConstants.META_VALUE_UNDEFINED, TBaseConstants.META_VALUE_UNDEFINED,
                    TBaseConstants.META_VALUE_UNDEFINED, newMngStatus, null)) {
                metaStoreService.updBrokerConf(newEntry, sBuffer, result);
                if (result.isSuccess()) {
                    triggerBrokerConfDataSync(newEntry,
                            curEntry.getManageStatus().getCode(), true, sBuffer, result);
                }
            } else {
                result.setSuccResult(null);
            }
            retInfo.add(new BrokerProcessResult(brokerId, curEntry.getBrokerIp(), result));
        }
        return retInfo;
    }

    /**
     * Change broker configure status
     *
     * @param opEntity      operator
     * @param brokerIdSet   need deleted broker id set
     * @param sBuffer       the print information string buffer
     * @param result        the process result return
     * @return true if success otherwise false
     */
    public List<BrokerProcessResult> reloadBrokerConfInfo(BaseEntity opEntity,
                                                          Set<Integer> brokerIdSet,
                                                          StringBuilder sBuffer,
                                                          ProcessResult result) {
        BrokerConfEntity curEntry;
        List<BrokerProcessResult> retInfo = new ArrayList<>();
        // check target broker configure's status
        for (Integer brokerId : brokerIdSet) {
            curEntry = metaStoreService.getBrokerConfByBrokerId(brokerId);
            if (curEntry == null) {
                result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                        "The broker configure not exist!");
                retInfo.add(new BrokerProcessResult(brokerId, "", result));
                continue;
            }
            if (!curEntry.getManageStatus().isOnlineStatus()) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        sBuffer.append("The broker manage status by brokerId=").append(brokerId)
                                .append(" not in online status, can't reload this configure! ")
                                .toString());
                sBuffer.delete(0, sBuffer.length());
                retInfo.add(new BrokerProcessResult(brokerId, "", result));
                continue;
            }
            triggerBrokerConfDataSync(curEntry,
                    curEntry.getManageStatus().getCode(), true, sBuffer, result);
            retInfo.add(new BrokerProcessResult(brokerId, curEntry.getBrokerIp(), result));
        }
        return retInfo;
    }

    /**
     * Delete broker configure information
     *
     * @param operator  operator
     * @param rsvData   if reserve topic's data info
     * @param brokerIdSet  need deleted broker id set
     * @param sBuffer  the print information string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    public List<BrokerProcessResult> delBrokerConfInfo(String operator, boolean rsvData,
                                                       Set<Integer> brokerIdSet,
                                                       StringBuilder sBuffer,
                                                       ProcessResult result) {
        List<BrokerProcessResult> retInfo = new ArrayList<>();
        Map<Integer, BrokerConfEntity> cfmBrokerMap = new HashMap<>();
        Map<Integer, BrokerConfEntity> tgtBrokerConfMap =
                getBrokerConfInfo(brokerIdSet, null, null);
        // check target broker configure's status
        for (BrokerConfEntity entity : tgtBrokerConfMap.values()) {
            if (entity == null) {
                continue;
            }
            if (!isMatchDeleteConds(entity.getBrokerId(),
                    entity.getManageStatus(), rsvData, sBuffer, result)) {
                retInfo.add(new BrokerProcessResult(
                        entity.getBrokerId(), entity.getBrokerIp(), result));
            }
            cfmBrokerMap.put(entity.getBrokerId(), entity);
        }
        if (cfmBrokerMap.isEmpty()) {
            return retInfo;
        }
        // execute delete operation
        for (BrokerConfEntity entry : cfmBrokerMap.values()) {
            if (entry == null) {
                continue;
            }
            delBrokerConfig(operator, entry.getBrokerId(), rsvData, sBuffer, result);
            retInfo.add(new BrokerProcessResult(
                    entry.getBrokerId(), entry.getBrokerIp(), result));
        }
        return retInfo;
    }

    private boolean isMatchDeleteConds(int brokerId, ManageStatus brokerStatus,
                                       boolean rsvData, StringBuilder sBuffer,
                                       ProcessResult result) {
        Map<String, TopicDeployEntity> topicConfigMap =
                getBrokerTopicConfEntitySet(brokerId);
        if (topicConfigMap == null || topicConfigMap.isEmpty()) {
            result.setSuccResult(null);
            return result.isSuccess();
        }
        if (WebParameterUtils.checkBrokerInOfflining(brokerId,
                brokerStatus.getCode(), this)) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    sBuffer.append("Illegal value: the broker is processing offline event by brokerId=")
                            .append(brokerId).append(", please wait and try later!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        if (rsvData) {
            for (Map.Entry<String, TopicDeployEntity> entry : topicConfigMap.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                if (entry.getValue().isAcceptPublish()
                        || entry.getValue().isAcceptSubscribe()) {
                    result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                            sBuffer.append("The topic ").append(entry.getKey())
                                    .append("'s acceptPublish and acceptSubscribe parameters")
                                    .append(" must be false in broker=")
                                    .append(brokerId)
                                    .append(" before broker delete by reserve data method!").toString());
                    sBuffer.delete(0, sBuffer.length());
                    return result.isSuccess();
                }
            }
        } else {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    sBuffer.append("Topic configure of broker by brokerId=")
                            .append(brokerId)
                            .append(" not deleted, please delete broker's topic configure first!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }


    private boolean delBrokerConfig(String operator, int brokerId, boolean rsvData,
                                    StringBuilder strBuffer, ProcessResult result) {
        BrokerConfEntity curEntity =
                metaStoreService.getBrokerConfByBrokerId(brokerId);
        if (curEntity == null) {
            result.setSuccResult(null);
            return result.isSuccess();
        }
        // process topic's configure
        Map<String, TopicDeployEntity> topicConfigMap =
                metaStoreService.getConfiguredTopicInfo(brokerId);
        if (topicConfigMap != null && !topicConfigMap.isEmpty()) {
            if (rsvData) {
                if (!metaStoreService.delTopicConfByBrokerId(operator,
                        brokerId, strBuffer, result)) {
                    return result.isSuccess();
                }
            } else {
                result.setFailResult(DataOpErrCode.DERR_UNCLEANED.getCode(),
                        "The broker's topic configure uncleaned!");
                return result.isSuccess();
            }
        }
        // check broker's manage status
        if (curEntity.getManageStatus().isOnlineStatus()) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    "Broker manage status is online, please offline first!");
            return result.isSuccess();
        }
        BrokerSyncStatusInfo brokerSyncStatusInfo =
                this.brokerRunSyncManageMap.get(curEntity.getBrokerId());
        if (brokerSyncStatusInfo != null) {
            if (brokerSyncStatusInfo.isBrokerRegister()
                    && (curEntity.getManageStatus() == ManageStatus.STATUS_MANAGE_OFFLINE
                    && brokerSyncStatusInfo.getBrokerRunStatus() != TStatusConstants.STATUS_SERVICE_UNDEFINED)) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        "Broker is processing offline event, please wait and try later!");
                return result.isSuccess();
            }
        }
        if (metaStoreService.delBrokerConf(operator, brokerId, strBuffer, result)) {
            this.brokerRunSyncManageMap.remove(brokerId);
            delBrokerRunData(brokerId);
        }
        return result.isSuccess();
    }

    /**
     * Manual reload broker config info
     *
     * @param entity
     * @param oldManageStatus
     * @param needFastStart
     * @return true if success otherwise false
     * @throws Exception
     */
    public boolean triggerBrokerConfDataSync(BrokerConfEntity entity,
                                             int oldManageStatus,
                                             boolean needFastStart,
                                             StringBuilder strBuffer,
                                             ProcessResult result) {
        if (!metaStoreService.checkStoreStatus(true, result)) {
            return result.isSuccess();
        }
        BrokerConfEntity curEntity =
                metaStoreService.getBrokerConfByBrokerId(entity.getBrokerId());
        if (curEntity == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    "The broker configure not exist!");
            return result.isSuccess();
        }
        String curBrokerConfStr = curEntity.getBrokerDefaultConfInfo();
        List<String> curBrokerTopicConfStrSet =
                getBrokerTopicStrConfigInfo(curEntity, strBuffer);
        BrokerSyncStatusInfo brokerSyncStatusInfo =
                this.brokerRunSyncManageMap.get(entity.getBrokerId());
        if (brokerSyncStatusInfo == null) {
            brokerSyncStatusInfo =
                    new BrokerSyncStatusInfo(entity, curBrokerTopicConfStrSet);
            BrokerSyncStatusInfo tmpBrokerSyncStatusInfo =
                    brokerRunSyncManageMap.putIfAbsent(entity.getBrokerId(), brokerSyncStatusInfo);
            if (tmpBrokerSyncStatusInfo != null) {
                brokerSyncStatusInfo = tmpBrokerSyncStatusInfo;
            }
        }
        if (brokerSyncStatusInfo.isBrokerRegister()
                && brokerSyncStatusInfo.getBrokerRunStatus() != TStatusConstants.STATUS_SERVICE_UNDEFINED) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    strBuffer.append("The broker is processing online event(")
                    .append(brokerSyncStatusInfo.getBrokerRunStatus())
                    .append("), please try later! ").toString());
            strBuffer.delete(0, strBuffer.length());
            return result.isSuccess();
        }
        if (brokerSyncStatusInfo.isFastStart()) {
            brokerSyncStatusInfo.setFastStart(needFastStart);
        }
        int curManageStatus = curEntity.getManageStatus().getCode();
        if (curManageStatus == TStatusConstants.STATUS_MANAGE_ONLINE
                || curManageStatus == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_WRITE
                || curManageStatus == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_READ) {
            boolean isOnlineUpdate =
                    (oldManageStatus == TStatusConstants.STATUS_MANAGE_ONLINE
                            || oldManageStatus == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_WRITE
                            || oldManageStatus == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_READ);
            brokerSyncStatusInfo.updateCurrBrokerConfInfo(curManageStatus,
                    curEntity.isConfDataUpdated(), curEntity.isBrokerLoaded(), curBrokerConfStr,
                    curBrokerTopicConfStrSet, isOnlineUpdate);
        } else {
            brokerSyncStatusInfo.setBrokerOffline();
        }
        strBuffer.append("triggered broker syncStatus info is ");
        logger.info(brokerSyncStatusInfo.toJsonString(strBuffer, false).toString());
        strBuffer.delete(0, strBuffer.length());
        result.setSuccResult(null);
        return result.isSuccess();
    }

    /**
     * Remove broker and related topic list
     *
     * @param brokerId
     * @param rmvTopics
     * @param strBuffer  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    public boolean clearRmvedTopicConfInfo(int brokerId,
                                           List<String> rmvTopics,
                                           StringBuilder strBuffer,
                                           ProcessResult result) {
        result.setSuccResult(null);
        if (rmvTopics == null || rmvTopics.isEmpty()) {
            return result.isSuccess();
        }
        if (!metaStoreService.checkStoreStatus(true, result)) {
            return result.isSuccess();
        }
        Map<String, TopicDeployEntity> confEntityMap =
                metaStoreService.getConfiguredTopicInfo(brokerId);
        if (confEntityMap == null || confEntityMap.isEmpty()) {
            return result.isSuccess();
        }
        for (String topicName : rmvTopics) {
            TopicDeployEntity topicEntity = confEntityMap.get(topicName);
            if (topicEntity != null
                    && topicEntity.getTopicStatus() == TopicStatus.STATUS_TOPIC_SOFT_REMOVE) {
                confDelTopicConfInfo(topicEntity.getModifyUser(),
                        topicEntity.getRecordKey(), strBuffer, result);
            }
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }



    /**
     * Find the broker and delete all topic info in the broker
     *
     * @param brokerId
     * @param operator   operator
     * @param strBuffer  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    public boolean clearAllTopicConfInfo(int brokerId,
                                         String operator,
                                         StringBuilder strBuffer,
                                         ProcessResult result) {
        result.setSuccResult(null);
        if (!metaStoreService.checkStoreStatus(true, result)) {
            return result.isSuccess();
        }
        Map<String, TopicDeployEntity> confEntityMap =
                metaStoreService.getConfiguredTopicInfo(brokerId);
        if (confEntityMap == null || confEntityMap.isEmpty()) {
            return result.isSuccess();
        }
        for (TopicDeployEntity topicEntity : confEntityMap.values()) {
            if (topicEntity == null) {
                continue;
            }
            confDelTopicConfInfo(operator, topicEntity.getRecordKey(), strBuffer, result);
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }

    /**
     * get broker's topicName set,
     * if brokerIds is empty, then return all broker's topicNames
     *
     * @param brokerIdSet
     * @return broker's topicName set
     */
    public Map<Integer, Set<String>> getBrokerTopicConfigInfo(
            Set<Integer> brokerIdSet) {
        return metaStoreService.getConfiguredTopicInfo(brokerIdSet);
    }

    /**
     * get topic's brokerId set,
     * if topicSet is empty, then return all topic's brokerIds
     *
     * @param topicNameSet
     * @return topic's brokerId set
     */
    public Map<String, Map<Integer, String>> getTopicBrokerConfigInfo(Set<String> topicNameSet) {
        return metaStoreService.getTopicBrokerInfo(topicNameSet);
    }

    public Set<String> getTotalConfiguredTopicNames() {
        return metaStoreService.getConfiguredTopicSet();
    }


    public ConcurrentHashMap<Integer, BrokerSyncStatusInfo> getBrokerRunSyncManageMap() {
        return this.brokerRunSyncManageMap;
    }

    public BrokerSyncStatusInfo getBrokerRunSyncStatusInfo(int brokerId) {
        return this.brokerRunSyncManageMap.get(brokerId);
    }

    public BrokerConfEntity getBrokerConfByBrokerId(int brokerId) {
        return metaStoreService.getBrokerConfByBrokerId(brokerId);
    }

    public BrokerConfEntity getBrokerConfByBrokerIp(String brokerIp) {
        return metaStoreService.getBrokerConfByBrokerIp(brokerIp);
    }

    public Map<String, TopicDeployEntity> getBrokerTopicConfEntitySet(int brokerId) {
        return metaStoreService.getConfiguredTopicInfo(brokerId);
    }

    public ConcurrentHashMap<String/* topicName */, TopicInfo> getBrokerRunTopicInfoMap(
            final int brokerId) {
        return this.brokerRunTopicInfoStoreMap.get(brokerId);
    }

    public void removeBrokerRunTopicInfoMap(final int brokerId) {
        this.brokerRunTopicInfoStoreMap.remove(brokerId);
    }

    public void updateBrokerRunTopicInfoMap(final int brokerId,
                                            ConcurrentHashMap<String, TopicInfo> topicInfoMap) {
        this.brokerRunTopicInfoStoreMap.put(brokerId, topicInfoMap);
    }

    public void resetBrokerReportInfo(final int brokerId) {
        BrokerSyncStatusInfo brokerSyncStatusInfo =
                brokerRunSyncManageMap.get(brokerId);
        if (brokerSyncStatusInfo != null) {
            brokerSyncStatusInfo.resetBrokerReportInfo();
        }
        brokerRunTopicInfoStoreMap.remove(brokerId);
    }

    /**
     * Update broker config
     *
     * @param brokerId
     * @param isChanged
     * @param isFasterStart
     * @return true if success otherwise false
     */
    public boolean updateBrokerConfChanged(int brokerId,
                                           boolean isChanged,
                                           boolean isFasterStart,
                                           StringBuilder strBuffer,
                                           ProcessResult result) {
        if (!metaStoreService.checkStoreStatus(true, result)) {
            return result.isSuccess();
        }
        BrokerConfEntity curEntity =
                metaStoreService.getBrokerConfByBrokerId(brokerId);
        if (curEntity == null) {
            return false;
        }
        // This function needs to be optimized continue
        if (isChanged) {
            if (!curEntity.isConfDataUpdated()) {
                curEntity.setConfDataUpdated();
                modBrokerConfig(curEntity, strBuffer, result);
            }
            if (curEntity.getManageStatus().isApplied()) {
                BrokerSyncStatusInfo brokerSyncStatusInfo =
                        brokerRunSyncManageMap.get(curEntity.getBrokerId());
                if (brokerSyncStatusInfo == null) {
                    List<String> newBrokerTopicConfStrSet =
                            getBrokerTopicStrConfigInfo(curEntity, strBuffer);
                    brokerSyncStatusInfo =
                            new BrokerSyncStatusInfo(curEntity, newBrokerTopicConfStrSet);
                    BrokerSyncStatusInfo tmpBrokerSyncStatusInfo =
                            brokerRunSyncManageMap.putIfAbsent(curEntity.getBrokerId(), brokerSyncStatusInfo);
                    if (tmpBrokerSyncStatusInfo != null) {
                        brokerSyncStatusInfo = tmpBrokerSyncStatusInfo;
                    }
                }
                if (brokerSyncStatusInfo.isFastStart()) {
                    brokerSyncStatusInfo.setFastStart(isFasterStart);
                }
                if (!brokerSyncStatusInfo.isBrokerConfChanged()) {
                    brokerSyncStatusInfo.setBrokerConfChanged();
                }
            }
        } else {
            if (curEntity.isConfDataUpdated()) {
                curEntity.setBrokerLoaded();
                modBrokerConfig(curEntity, strBuffer, result);
            }
            if (curEntity.getManageStatus().isApplied()) {
                BrokerSyncStatusInfo brokerSyncStatusInfo =
                        brokerRunSyncManageMap.get(curEntity.getBrokerId());
                if (brokerSyncStatusInfo == null) {
                    List<String> newBrokerTopicConfStrSet =
                            getBrokerTopicStrConfigInfo(curEntity, strBuffer);
                    brokerSyncStatusInfo =
                            new BrokerSyncStatusInfo(curEntity, newBrokerTopicConfStrSet);
                    BrokerSyncStatusInfo tmpBrokerSyncStatusInfo =
                            brokerRunSyncManageMap.putIfAbsent(curEntity.getBrokerId(),
                                    brokerSyncStatusInfo);
                    if (tmpBrokerSyncStatusInfo != null) {
                        brokerSyncStatusInfo = tmpBrokerSyncStatusInfo;
                    }
                }
                if (brokerSyncStatusInfo.isBrokerConfChanged()) {
                    brokerSyncStatusInfo.setBrokerLoaded();
                    brokerSyncStatusInfo.setFastStart(isFasterStart);
                }
            }
        }
        return true;
    }

    public void updateBrokerMaps(BrokerConfEntity entity) {
        if (entity != null) {
            String brokerReg =
                    this.brokersMap.putIfAbsent(entity.getBrokerId(),
                            entity.getSimpleBrokerInfo());
            String brokerTLSReg =
                    this.brokersTLSMap.putIfAbsent(entity.getBrokerId(),
                            entity.getSimpleTLSBrokerInfo());
            if (brokerReg == null
                    || brokerTLSReg == null
                    || !brokerReg.equals(entity.getSimpleBrokerInfo())
                    || !brokerTLSReg.equals(entity.getSimpleTLSBrokerInfo())) {
                if (brokerReg != null
                        && !brokerReg.equals(entity.getSimpleBrokerInfo())) {
                    this.brokersMap.put(entity.getBrokerId(), entity.getSimpleBrokerInfo());
                }
                if (brokerTLSReg != null
                        && !brokerTLSReg.equals(entity.getSimpleTLSBrokerInfo())) {
                    this.brokersTLSMap.put(entity.getBrokerId(), entity.getSimpleTLSBrokerInfo());
                }
                this.lastBrokerUpdatedTime = System.currentTimeMillis();
                this.brokerInfoCheckSum.set(this.lastBrokerUpdatedTime);
            }
        }
    }

    public void delBrokerRunData(int brokerId) {
        if (brokerId == TBaseConstants.META_VALUE_UNDEFINED) {
            return;
        }
        String brokerReg = this.brokersMap.remove(brokerId);
        String brokerTLSReg = this.brokersTLSMap.remove(brokerId);
        if (brokerReg != null || brokerTLSReg != null) {
            this.lastBrokerUpdatedTime = System.currentTimeMillis();
            this.brokerInfoCheckSum.set(this.lastBrokerUpdatedTime);
        }
    }

    // ////////////////////////////////////////////////////////////////////////////

    public TopicProcessResult addOrUpdTopicDeployInfo(boolean isAddOp, BaseEntity opEntity,
                                                      int brokerId, String topicName,
                                                      TopicStatus deployStatus,
                                                      TopicPropGroup topicPropInfo,
                                                      StringBuilder sBuffer,
                                                      ProcessResult result) {
        // check broker configure exist
        BrokerConfEntity brokerConf =
                getBrokerConfByBrokerId(brokerId);
        if (brokerConf == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    sBuffer.append("Not found broker configure record by brokerId=")
                            .append(brokerId)
                            .append(", please create the broker's configure first!").toString());
            sBuffer.delete(0, sBuffer.length());
            return new TopicProcessResult(brokerId, "", result);
        }
        TopicDeployEntity deployConf =
                new TopicDeployEntity(opEntity, brokerId, topicName);
        deployConf.setTopicProps(brokerConf.getTopicProps());
        deployConf.updModifyInfo(opEntity.getDataVerId(),
                TBaseConstants.META_VALUE_UNDEFINED, brokerConf.getBrokerPort(),
                brokerConf.getBrokerIp(), deployStatus, topicPropInfo);
        return addOrUpdTopicDeployInfo(isAddOp, deployConf, sBuffer, result);
    }

    public TopicProcessResult addOrUpdTopicDeployInfo(boolean isAddOp,
                                                      TopicDeployEntity deployEntity,
                                                      StringBuilder sBuffer,
                                                      ProcessResult result) {
        // check broker configure exist
        BrokerConfEntity brokerConf =
                getBrokerConfByBrokerId(deployEntity.getBrokerId());
        if (brokerConf == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    sBuffer.append("Not found broker configure record by brokerId=")
                            .append(deployEntity.getBrokerId())
                            .append(", please create the broker's configure first!").toString());
            sBuffer.delete(0, sBuffer.length());
            return new TopicProcessResult(deployEntity.getBrokerId(), "", result);
        }
        // add topic control configure
        if (!addIfAbsentTopicCtrlConf(deployEntity.getTopicName(),
                deployEntity.getModifyUser(), sBuffer, result)) {
            return new TopicProcessResult(deployEntity.getBrokerId(),
                    deployEntity.getTopicName(), result);
        }
        // add or update topic deployment record
        TopicDeployEntity curEntity =
                metaStoreService.getTopicConfByeRecKey(deployEntity.getRecordKey());
        if (isAddOp) {
            if (curEntity == null) {
                metaStoreService.addTopicConf(deployEntity, sBuffer, result);
            } else {
                if (curEntity.isValidTopicStatus()) {
                    result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                            sBuffer.append("Duplicate topic deploy configure, exist record is: ")
                                    .append("brokerId=").append(curEntity.getBrokerId())
                                    .append(", topicName=").append(curEntity.getTopicName())
                                    .toString());
                } else {
                    result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                            sBuffer.append("Topic of ").append(curEntity.getTopicName())
                                    .append(" is deleted softly in brokerId=")
                                    .append(curEntity.getBrokerId())
                                    .append(", please resume the record or hard removed first!")
                                    .toString());
                }
                sBuffer.delete(0, sBuffer.length());
            }
            return new TopicProcessResult(deployEntity.getBrokerId(),
                    deployEntity.getTopicName(), result);
        } else {
            // update current deployment configure
            if (curEntity == null) {
                result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                        sBuffer.append("Not found the topic ").append(curEntity.getTopicName())
                                .append("'s deploy configure in broker=")
                                .append(curEntity.getBrokerId())
                                .append(", please confirm the configure first!").toString());
                sBuffer.delete(0, sBuffer.length());
                return new TopicProcessResult(deployEntity.getBrokerId(),
                        deployEntity.getTopicName(), result);
            }
            // check deploy status
            if (!curEntity.isValidTopicStatus()) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        sBuffer.append("Topic of ").append(curEntity.getTopicName())
                                .append("is deleted softly in brokerId=")
                                .append(curEntity.getBrokerId())
                                .append(", please resume the record or hard removed first!")
                                .toString());
                sBuffer.delete(0, sBuffer.length());
                return new TopicProcessResult(deployEntity.getBrokerId(),
                        deployEntity.getTopicName(), result);
            }
            // check if shrink data store block
            if (deployEntity.getTopicProps() != null) {
                if (deployEntity.getNumPartitions() != TBaseConstants.META_VALUE_UNDEFINED
                        && deployEntity.getNumPartitions() < curEntity.getNumPartitions()) {
                    result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                            sBuffer.append("Partition value is less than before,")
                                    .append("please confirm the configure first! brokerId=")
                                    .append(curEntity.getBrokerId()).append(", topicName=")
                                    .append(curEntity.getTopicName())
                                    .append(", old Partition value is ")
                                    .append(curEntity.getNumPartitions())
                                    .append(", new Partition value is ")
                                    .append(deployEntity.getNumPartitions()).toString());
                    sBuffer.delete(0, sBuffer.length());
                    return new TopicProcessResult(deployEntity.getBrokerId(),
                            deployEntity.getTopicName(), result);
                }
                if (deployEntity.getNumTopicStores() != TBaseConstants.META_VALUE_UNDEFINED
                        && deployEntity.getNumTopicStores() < curEntity.getNumTopicStores()) {
                    result.setFailResult(DataOpErrCode.DERR_ILLEGAL_VALUE.getCode(),
                            sBuffer.append("TopicStores value is less than before,")
                                    .append("please confirm the configure first! brokerId=")
                                    .append(curEntity.getBrokerId()).append(", topicName=")
                                    .append(curEntity.getTopicName())
                                    .append(", old TopicStores value is ")
                                    .append(curEntity.getNumTopicStores())
                                    .append(", new TopicStores value is ")
                                    .append(deployEntity.getNumTopicStores()).toString());
                    sBuffer.delete(0, sBuffer.length());
                    return new TopicProcessResult(deployEntity.getBrokerId(),
                            deployEntity.getTopicName(), result);
                }
            }
            TopicDeployEntity newEntity = curEntity.clone();
            newEntity.updBaseModifyInfo(deployEntity);
            if (!newEntity.updModifyInfo(deployEntity.getDataVerId(),
                    deployEntity.getTopicId(), deployEntity.getBrokerPort(),
                    deployEntity.getBrokerIp(), deployEntity.getDeployStatus(),
                    deployEntity.getTopicProps())) {
                result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                        sBuffer.append("Data not changed for brokerId=")
                                .append(curEntity.getBrokerId()).append(", topicName=")
                                .append(curEntity.getTopicName()).toString());
                sBuffer.delete(0, sBuffer.length());
            } else {
                metaStoreService.updTopicConf(newEntity, sBuffer, result);
            }
            return new TopicProcessResult(deployEntity.getBrokerId(),
                    deployEntity.getTopicName(), result);
        }
    }

    /**
     * Modify topic deploy status info
     *
     * @param result     the process result return
     * @return true if success otherwise false
     */
    public TopicProcessResult updTopicDeployStatusInfo(BaseEntity opEntity, int brokerId,
                                                       String topicName, TopicStsChgType chgType,
                                                       StringBuilder sBuffer, ProcessResult result) {
        // get broker configure record
        BrokerConfEntity brokerConf = getBrokerConfByBrokerId(brokerId);
        if (brokerConf == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    sBuffer.append("Not found broker configure record by brokerId=")
                            .append(brokerId)
                            .append(", please create the broker's configure first!").toString());
            sBuffer.delete(0, sBuffer.length());
            return new TopicProcessResult(brokerId, topicName, result);
        }
        // get topic deploy configure record
        TopicDeployEntity curEntity = getTopicConfInfo(brokerId, topicName);
        if (curEntity == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    sBuffer.append("Not found the topic ").append(topicName)
                            .append("'s deploy configure in broker=").append(brokerId)
                            .append(", please confirm the configure first!").toString());
            sBuffer.delete(0, sBuffer.length());
            return new TopicProcessResult(brokerId, topicName, result);
        }
        // check deploy status if still accept publish and subscribe
        if (curEntity.isAcceptPublish()
                || curEntity.isAcceptSubscribe()) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    sBuffer.append("The topic ").append(topicName)
                            .append("'s acceptPublish and acceptSubscribe status must be false in broker=")
                            .append(brokerId).append(" before topic deleted!").toString());
            sBuffer.delete(0, sBuffer.length());
            return new TopicProcessResult(brokerId, topicName, result);
        }
        TopicStatus topicStatus;
        if (chgType == TopicStsChgType.STATUS_CHANGE_SOFT_DELETE) {
            if (!curEntity.isValidTopicStatus()) {
                result.setSuccResult("");
                return new TopicProcessResult(brokerId, topicName, result);
            }
            topicStatus = TopicStatus.STATUS_TOPIC_SOFT_DELETE;
        } else if (chgType == TopicStsChgType.STATUS_CHANGE_REMOVE) {
            if (curEntity.getTopicStatus() != TopicStatus.STATUS_TOPIC_SOFT_DELETE) {
                result.setSuccResult("");
                return new TopicProcessResult(brokerId, topicName, result);
            }
            topicStatus = TopicStatus.STATUS_TOPIC_SOFT_REMOVE;
        } else {
            if (curEntity.getTopicStatus() != TopicStatus.STATUS_TOPIC_SOFT_DELETE) {
                if (curEntity.isValidTopicStatus()) {
                    result.setSuccResult(null);
                } else {
                    result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                            sBuffer.append("Topic of ").append(topicName)
                                    .append("is in removing flow in brokerId=")
                                    .append(curEntity.getBrokerId())
                                    .append(", please wait until remove process finished!")
                                    .toString());
                    sBuffer.delete(0, sBuffer.length());
                }
                return new TopicProcessResult(brokerId, topicName, result);
            }
            topicStatus = TopicStatus.STATUS_TOPIC_OK;
        }
        TopicDeployEntity newEntity = curEntity.clone();
        newEntity.updBaseModifyInfo(opEntity);
        if (newEntity.updModifyInfo(opEntity.getDataVerId(),
                curEntity.getTopicId(), brokerConf.getBrokerPort(),
                brokerConf.getBrokerIp(), topicStatus, null)) {
            metaStoreService.updTopicConf(newEntity, sBuffer, result);
        } else {
            result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                    sBuffer.append("Data not changed for brokerId=")
                            .append(curEntity.getBrokerId()).append(", topicName=")
                            .append(curEntity.getTopicName()).toString());
            sBuffer.delete(0, sBuffer.length());
        }
        return new TopicProcessResult(brokerId, topicName, result);
    }

    /**
     * Get broker topic entity, if query entity is null, return all topic entity
     *
     * @param qryEntity query conditions
     * @return topic entity map
     */
    public Map<String, List<TopicDeployEntity>> getTopicConfEntityMap(Set<String> topicNameSet,
                                                                      Set<Integer> brokerIdSet,
                                                                      TopicDeployEntity qryEntity) {
        return metaStoreService.getTopicConfMap(topicNameSet, brokerIdSet, qryEntity);
    }

    public TopicDeployEntity getTopicConfInfo(int brokerId, String topicName) {
        return metaStoreService.getTopicConf(brokerId, topicName);
    }

    /**
     * Get broker topic entity, if query entity is null, return all topic entity
     *
     * @return topic entity map
     */
    public Map<Integer, List<TopicDeployEntity>> getTopicDeployInfoMap(Set<String> topicNameSet,
                                                                       Set<Integer> brokerIdSet) {
        return metaStoreService.getTopicDeployInfoMap(topicNameSet, brokerIdSet);
    }

    public Map<String, List<TopicDeployEntity>> getTopicConfMapByTopicAndBrokerIds(
            Set<String> topicNameSet, Set<Integer> brokerIdSet) {
        return metaStoreService.getTopicDepInfoByTopicBrokerId(topicNameSet, brokerIdSet);
    }




    private boolean confDelTopicConfInfo(String operator,
                                         String recordKey,
                                         StringBuilder strBuffer,
                                         ProcessResult result) {
        return metaStoreService.delTopicConf(operator,
                recordKey, strBuffer, result);
    }

    public List<String> getBrokerTopicStrConfigInfo(
            BrokerConfEntity brokerConfEntity, StringBuilder sBuffer) {
        return inGetTopicConfStrInfo(brokerConfEntity, false, sBuffer);
    }

    public List<String> getBrokerRemovedTopicStrConfigInfo(
            BrokerConfEntity brokerConfEntity, StringBuilder sBuffer) {
        return inGetTopicConfStrInfo(brokerConfEntity, true, sBuffer);
    }

    private List<String> inGetTopicConfStrInfo(BrokerConfEntity brokerEntity,
                                               boolean isRemoved, StringBuilder sBuffer) {
        List<String> topicConfStrs = new ArrayList<>();
        Map<String, TopicDeployEntity> topicEntityMap =
                metaStoreService.getConfiguredTopicInfo(brokerEntity.getBrokerId());
        if (topicEntityMap.isEmpty()) {
            return topicConfStrs;
        }
        TopicPropGroup defTopicProps = brokerEntity.getTopicProps();
        ClusterSettingEntity clusterDefConf =
                metaStoreService.getClusterConfig();
        int defMsgSizeInB = clusterDefConf.getMaxMsgSizeInB();
        for (TopicDeployEntity topicEntity : topicEntityMap.values()) {
            /*
             * topic:partNum:acceptPublish:acceptSubscribe:unflushThreshold:unflushInterval:deleteWhen:
             * deletePolicy:filterStatusId:statusId
             */
            if ((isRemoved && !topicEntity.isInRemoving())
                    || (!isRemoved && topicEntity.isInRemoving())) {
                continue;
            }
            sBuffer.append(topicEntity.getTopicName());
            TopicPropGroup topicProps = topicEntity.getTopicProps();
            if (topicProps.getNumPartitions() == defTopicProps.getNumPartitions()) {
                sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                sBuffer.append(TokenConstants.ATTR_SEP).append(topicProps.getNumPartitions());
            }
            if (topicProps.isAcceptPublish() == defTopicProps.isAcceptPublish()) {
                sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                sBuffer.append(TokenConstants.ATTR_SEP).append(topicProps.isAcceptPublish());
            }
            if (topicProps.isAcceptSubscribe() == defTopicProps.isAcceptSubscribe()) {
                sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                sBuffer.append(TokenConstants.ATTR_SEP).append(topicProps.isAcceptSubscribe());
            }
            if (topicProps.getUnflushThreshold() == defTopicProps.getUnflushThreshold()) {
                sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                sBuffer.append(TokenConstants.ATTR_SEP).append(topicProps.getUnflushThreshold());
            }
            if (topicProps.getUnflushInterval() == defTopicProps.getUnflushInterval()) {
                sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                sBuffer.append(TokenConstants.ATTR_SEP).append(topicProps.getUnflushInterval());
            }
            sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            if (topicProps.getDeletePolicy().equals(defTopicProps.getDeletePolicy())) {
                sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                sBuffer.append(TokenConstants.ATTR_SEP).append(topicProps.getDeletePolicy());
            }
            if (topicProps.getNumTopicStores() == defTopicProps.getNumTopicStores()) {
                sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                sBuffer.append(TokenConstants.ATTR_SEP).append(topicProps.getNumTopicStores());
            }
            sBuffer.append(TokenConstants.ATTR_SEP).append(topicEntity.getTopicStatusId());
            if (topicProps.getUnflushDataHold() == defTopicProps.getUnflushDataHold()) {
                sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                sBuffer.append(TokenConstants.ATTR_SEP).append(topicProps.getUnflushDataHold());
            }
            if (topicProps.getMemCacheMsgSizeInMB() == defTopicProps.getMemCacheMsgSizeInMB()) {
                sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                sBuffer.append(TokenConstants.ATTR_SEP).append(topicProps.getMemCacheMsgSizeInMB());
            }
            if (topicProps.getMemCacheMsgCntInK() == defTopicProps.getMemCacheMsgCntInK()) {
                sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                sBuffer.append(TokenConstants.ATTR_SEP).append(topicProps.getMemCacheMsgCntInK());
            }
            if (topicProps.getMemCacheFlushIntvl() == defTopicProps.getMemCacheFlushIntvl()) {
                sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                sBuffer.append(TokenConstants.ATTR_SEP).append(topicProps.getMemCacheFlushIntvl());
            }
            int maxMsgSize = defMsgSizeInB;
            TopicCtrlEntity topicCtrlEntity =
                    metaStoreService.getTopicCtrlConf(topicEntity.getTopicName());
            if (topicCtrlEntity != null) {
                if (topicCtrlEntity.getMaxMsgSizeInB() != TBaseConstants.META_VALUE_UNDEFINED) {
                    maxMsgSize = topicCtrlEntity.getMaxMsgSizeInB();
                }
            }
            if (maxMsgSize == defMsgSizeInB) {
                sBuffer.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                sBuffer.append(TokenConstants.ATTR_SEP).append(maxMsgSize);
            }
            topicConfStrs.add(sBuffer.toString());
            sBuffer.delete(0, sBuffer.length());
        }
        return topicConfStrs;
    }

    // /////////////////////////////////////////////////////////////////////////////////

    /**
     * Add or Update topic control configure info
     *
     * @param sBuffer  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    public TopicProcessResult addOrUpdTopicCtrlConf(boolean isAddOp, BaseEntity opEntity,
                                                    String topicName, int topicNameId,
                                                    Boolean enableTopicAuth, int maxMsgSizeInMB,
                                                    StringBuilder sBuffer, ProcessResult result) {
        TopicCtrlEntity entity =
                new TopicCtrlEntity(opEntity, topicName);
        entity.updModifyInfo(opEntity.getDataVerId(),
                topicNameId, maxMsgSizeInMB, enableTopicAuth);
        return addOrUpdTopicCtrlConf(isAddOp, entity, sBuffer, result);
    }

    /**
     * Add or Update topic control configure info
     *
     * @param entity  the topic control info entity will be add
     * @param sBuffer   the print info string buffer
     * @param result    the process result return
     * @return true if success otherwise false
     */
    public TopicProcessResult addOrUpdTopicCtrlConf(boolean isAddOp, TopicCtrlEntity entity,
                                                    StringBuilder sBuffer, ProcessResult result) {

        TopicCtrlEntity curEntity =
                metaStoreService.getTopicCtrlConf(entity.getTopicName());
        if (isAddOp) {
            if (curEntity == null) {
                metaStoreService.addTopicCtrlConf(entity, sBuffer, result);
            } else {
                result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                        DataOpErrCode.DERR_EXISTED.getDescription());
            }
        } else {
            if (curEntity == null) {
                result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                        DataOpErrCode.DERR_NOT_EXIST.getDescription());
            } else {
                TopicCtrlEntity newEntity = curEntity.clone();
                newEntity.updBaseModifyInfo(entity);
                if (newEntity.updModifyInfo(entity.getDataVerId(), entity.getTopicId(),
                        entity.getMaxMsgSizeInMB(), entity.isAuthCtrlEnable())) {
                    metaStoreService.updTopicCtrlConf(newEntity, sBuffer, result);
                } else {
                    result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                            DataOpErrCode.DERR_UNCHANGED.getDescription());
                }
            }
        }
        return new TopicProcessResult(0, entity.getTopicName(), result);
    }

    /**
     * Add or Update topic control configure info
     *
     * @param sBuffer  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    public TopicProcessResult addOrUpdTopicCtrlConf(BaseEntity opEntity, String topicName,
                                                    Boolean enableTopicAuth, StringBuilder sBuffer,
                                                    ProcessResult result) {
        TopicCtrlEntity entity =
                new TopicCtrlEntity(opEntity, topicName);
        entity.updModifyInfo(opEntity.getDataVerId(),
                TBaseConstants.META_VALUE_UNDEFINED,
                TBaseConstants.META_VALUE_UNDEFINED, enableTopicAuth);
        return addOrUpdTopicCtrlConf(entity, sBuffer, result);
    }

    public TopicProcessResult addOrUpdTopicCtrlConf(TopicCtrlEntity entity,
                                                    StringBuilder sBuffer,
                                                    ProcessResult result) {
        TopicCtrlEntity newEntity;
        TopicCtrlEntity curEntity =
                metaStoreService.getTopicCtrlConf(entity.getTopicName());
        if (curEntity == null) {
            newEntity = new TopicCtrlEntity(entity, entity.getTopicName());
            newEntity.updModifyInfo(entity.getDataVerId(), entity.getTopicId(),
                    entity.getMaxMsgSizeInMB(), entity.isAuthCtrlEnable());
            metaStoreService.addTopicCtrlConf(newEntity, sBuffer, result);
        } else {
            newEntity = curEntity.clone();
            newEntity.updBaseModifyInfo(entity);
            if (newEntity.updModifyInfo(entity.getDataVerId(), entity.getTopicId(),
                    entity.getMaxMsgSizeInMB(), entity.isAuthCtrlEnable())) {
                metaStoreService.updTopicCtrlConf(newEntity, sBuffer, result);
            } else {
                result.setSuccResult(null);
            }
        }
        return new TopicProcessResult(0, entity.getTopicName(), result);
    }

    /**
     * Delete topic control configure
     *
     * @param operator   operator
     * @param topicName  the topicName will be deleted
     * @param sBuffer  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    public boolean delTopicCtrlConf(String operator,
                                    String topicName,
                                    StringBuilder sBuffer,
                                    ProcessResult result) {
        // check current status
        if (!metaStoreService.checkStoreStatus(true, result)) {
            return result.isSuccess();
        }
        if (metaStoreService.isTopicDeployed(topicName)) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    sBuffer.append("TopicName ").append(topicName)
                            .append(" is in using, please delete the deploy info first!")
                            .toString());
            return result.isSuccess();
        }
        if (metaStoreService.isTopicNameInUsed(topicName)) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    sBuffer.append("TopicName ").append(topicName)
                            .append(" is in using, please delete the consume control info first!")
                            .toString());
            return result.isSuccess();
        }
        metaStoreService.delTopicCtrlConf(operator, topicName, sBuffer, result);
        return result.isSuccess();
    }


    /**
     * Add if absent topic control configure info
     *
     * @param deployEntity  the topic deploy info will be add
     * @param strBuffer     the print info string buffer
     * @param result        the process result return
     * @return true if success otherwise false
     */
    public void addIfAbsentTopicCtrlConf(TopicDeployEntity deployEntity,
                                         StringBuilder strBuffer,
                                         ProcessResult result) {
        TopicCtrlEntity curEntity =
                metaStoreService.getTopicCtrlConf(deployEntity.getTopicName());
        if (curEntity != null) {
            return;
        }
        int maxMsgSizeInMB = TBaseConstants.META_MIN_ALLOWED_MESSAGE_SIZE_MB;
        ClusterSettingEntity defSetting = metaStoreService.getClusterConfig();
        if (defSetting != null) {
            maxMsgSizeInMB = defSetting.getMaxMsgSizeInMB();
        }
        curEntity = new TopicCtrlEntity(deployEntity.getTopicName(),
                deployEntity.getTopicId(), maxMsgSizeInMB, deployEntity.getCreateUser());
        metaStoreService.addTopicCtrlConf(curEntity, strBuffer, result);
        return;
    }

    /**
     * Add if absent topic control configure info
     *
     * @param topicNameSet  the topic name will be add
     * @param operator the topic name id will be add
     * @param operator   operator
     * @param sBuffer  the print info string buffer
     */
    public boolean addIfAbsentTopicCtrlConf(Set<String> topicNameSet, String operator,
                                            StringBuilder sBuffer, ProcessResult result) {
        TopicCtrlEntity curEntity;
        int maxMsgSizeInMB = TBaseConstants.META_MIN_ALLOWED_MESSAGE_SIZE_MB;
        ClusterSettingEntity defSetting = metaStoreService.getClusterConfig();
        if (defSetting != null) {
            maxMsgSizeInMB = defSetting.getMaxMsgSizeInMB();
        }
        for (String topicName : topicNameSet) {
            curEntity = metaStoreService.getTopicCtrlConf(topicName);
            if (curEntity != null) {
                continue;
            }
            curEntity = new TopicCtrlEntity(topicName,
                    TBaseConstants.META_VALUE_UNDEFINED, maxMsgSizeInMB, operator);
            if (!metaStoreService.addTopicCtrlConf(curEntity, sBuffer, result)) {
                return result.isSuccess();
            }
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }

    /**
     * Add if absent topic control configure info
     *
     * @param topicName  the topic name will be add
     * @param operator the topic name id will be add
     * @param operator   operator
     * @param sBuffer  the print info string buffer
     */
    public boolean addIfAbsentTopicCtrlConf(String topicName, String operator,
                                            StringBuilder sBuffer, ProcessResult result) {
        int maxMsgSizeInMB = TBaseConstants.META_MIN_ALLOWED_MESSAGE_SIZE_MB;
        ClusterSettingEntity defSetting = metaStoreService.getClusterConfig();
        if (defSetting != null) {
            maxMsgSizeInMB = defSetting.getMaxMsgSizeInMB();
        }
        TopicCtrlEntity curEntity =
                metaStoreService.getTopicCtrlConf(topicName);
        if (curEntity == null) {
            curEntity = new TopicCtrlEntity(topicName,
                    TBaseConstants.META_VALUE_UNDEFINED, maxMsgSizeInMB, operator);
            metaStoreService.addTopicCtrlConf(curEntity, sBuffer, result);
        } else {
            result.setSuccResult(null);
        }
        return result.isSuccess();
    }

    public TopicCtrlEntity getTopicCtrlByTopicName(String topicName) {
        return this.metaStoreService.getTopicCtrlConf(topicName);
    }

    public int getTopicMaxMsgSizeInMB(String topicName) {
        // get maxMsgSizeInMB info
        ClusterSettingEntity clusterSettingEntity = getClusterDefSetting(false);
        int maxMsgSizeInMB = clusterSettingEntity.getMaxMsgSizeInMB();
        TopicCtrlEntity topicCtrlEntity = getTopicCtrlByTopicName(topicName);
        if (topicCtrlEntity != null) {
            maxMsgSizeInMB = topicCtrlEntity.getMaxMsgSizeInMB();
        }
        return maxMsgSizeInMB;
    }

    /**
     * Get topic control entity list
     *
     * @param qryEntity
     * @return entity list
     */
    public List<TopicCtrlEntity> queryTopicCtrlConf(TopicCtrlEntity qryEntity) {
        return metaStoreService.getTopicCtrlConf(qryEntity);
    }

    public Map<String, TopicCtrlEntity> getTopicCtrlConf(Set<String> topicNameSet,
                                                         TopicCtrlEntity qryEntity) {
        return metaStoreService.getTopicCtrlConf(topicNameSet, qryEntity);
    }



    // //////////////////////////////////////////////////////////////////////////////

    public boolean addClusterDefSetting(BaseEntity opEntity, int brokerPort,
                                        int brokerTlsPort, int brokerWebPort,
                                        int maxMsgSizeMB, int qryPriorityId,
                                        Boolean flowCtrlEnable, int flowRuleCnt,
                                        String flowCtrlInfo, TopicPropGroup topicProps,
                                        StringBuilder strBuffer, ProcessResult result) {
        ClusterSettingEntity newConf =
                new ClusterSettingEntity(opEntity);
        newConf.fillDefaultValue();
        newConf.updModifyInfo(opEntity.getDataVerId(), brokerPort,
                brokerTlsPort, brokerWebPort, maxMsgSizeMB, qryPriorityId,
                flowCtrlEnable, flowRuleCnt, flowCtrlInfo, topicProps);
        return metaStoreService.addClusterConfig(newConf, strBuffer, result);
    }

    /**
     * Update cluster default setting
     *
     * @return true if success otherwise false
     */
    public boolean modClusterDefSetting(BaseEntity opEntity, int brokerPort,
                                        int brokerTlsPort, int brokerWebPort,
                                        int maxMsgSizeMB, int qryPriorityId,
                                        Boolean flowCtrlEnable, int flowRuleCnt,
                                        String flowCtrlInfo, TopicPropGroup topicProps,
                                        StringBuilder strBuffer, ProcessResult result) {
        ClusterSettingEntity curConf =
                metaStoreService.getClusterConfig();
        if (curConf == null) {
            result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                    DataOpErrCode.DERR_EXISTED.getDescription());
            return result.isSuccess();
        }
        ClusterSettingEntity newConf = curConf.clone();
        newConf.updBaseModifyInfo(opEntity);
        if (newConf.updModifyInfo(opEntity.getDataVerId(), brokerPort,
                brokerTlsPort, brokerWebPort, maxMsgSizeMB, qryPriorityId,
                flowCtrlEnable, flowRuleCnt, flowCtrlInfo, topicProps)) {
            metaStoreService.updClusterConfig(newConf, strBuffer, result);
        } else {
            result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                    DataOpErrCode.DERR_UNCHANGED.getDescription());
        }
        return result.isSuccess();
    }

    /**
     * Update cluster default setting
     *
     * @return true if success otherwise false
     */
    public boolean addOrUpdClusterDefSetting(BaseEntity opEntity, int brokerPort,
                                             int brokerTlsPort, int brokerWebPort,
                                             int maxMsgSizeMB, int qryPriorityId,
                                             Boolean flowCtrlEnable, int flowRuleCnt,
                                             String flowCtrlInfo, TopicPropGroup topicProps,
                                             StringBuilder strBuffer, ProcessResult result) {
        ClusterSettingEntity newConf;
        ClusterSettingEntity curConf = metaStoreService.getClusterConfig();
        if (curConf == null) {
            newConf = new ClusterSettingEntity(opEntity);
            newConf.fillDefaultValue();
            newConf.updModifyInfo(opEntity.getDataVerId(), brokerPort,
                    brokerTlsPort, brokerWebPort, maxMsgSizeMB, qryPriorityId,
                    flowCtrlEnable, flowRuleCnt, flowCtrlInfo, topicProps);
            metaStoreService.addClusterConfig(newConf, strBuffer, result);
        } else {
            newConf = curConf.clone();
            newConf.updBaseModifyInfo(opEntity);
            if (newConf.updModifyInfo(opEntity.getDataVerId(), brokerPort,
                    brokerTlsPort, brokerWebPort, maxMsgSizeMB, qryPriorityId,
                    flowCtrlEnable, flowRuleCnt, flowCtrlInfo, topicProps)) {
                metaStoreService.updClusterConfig(newConf, strBuffer, result);
            } else {
                result.setSuccResult(null);
            }
        }
        return result.isSuccess();
    }

    public ClusterSettingEntity getClusterDefSetting(boolean isMustConf) {
        ClusterSettingEntity curClsSetting =
                metaStoreService.getClusterConfig();
        if (!isMustConf && curClsSetting == null) {
            curClsSetting = defClusterSetting;
        }
        return curClsSetting;
    }

    // //////////////////////////////////////////////////////////////////////////////

    public GroupProcessResult addOrUpdGroupResCtrlConf(boolean isAddOp, BaseEntity opEntity,
                                                       String groupName, Boolean resCheckEnable,
                                                       int allowedBClientRate, int qryPriorityId,
                                                       Boolean flowCtrlEnable, int flowRuleCnt,
                                                       String flowCtrlInfo, StringBuilder sBuffer,
                                                       ProcessResult result) {
        GroupResCtrlEntity entity =
                new GroupResCtrlEntity(opEntity, groupName);
        entity.updModifyInfo(opEntity.getDataVerId(), resCheckEnable, allowedBClientRate,
                qryPriorityId, flowCtrlEnable, flowRuleCnt, flowCtrlInfo);
        return addOrUpdGroupResCtrlConf(isAddOp, entity, sBuffer, result);
    }

    /**
     * Add group resource control configure info
     *
     * @param entity     the group resource control info entity will be add
     * @param sBuffer  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    public GroupProcessResult addOrUpdGroupResCtrlConf(boolean isAddOp,
                                                       GroupResCtrlEntity entity,
                                                       StringBuilder sBuffer,
                                                       ProcessResult result) {
        GroupResCtrlEntity curEntity =
                metaStoreService.getGroupResCtrlConf(entity.getGroupName());
        if (isAddOp) {
            if (curEntity == null) {
                metaStoreService.addGroupResCtrlConf(entity, sBuffer, result);
            } else {
                result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                        DataOpErrCode.DERR_EXISTED.getDescription());
            }
        } else {
            if (curEntity == null) {
                result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                        DataOpErrCode.DERR_NOT_EXIST.getDescription());
            } else {
                GroupResCtrlEntity newEntity = curEntity.clone();
                newEntity.updBaseModifyInfo(entity);
                if (newEntity.updModifyInfo(entity.getDataVerId(), entity.isEnableResCheck(),
                        entity.getAllowedBrokerClientRate(), entity.getQryPriorityId(),
                        entity.isFlowCtrlEnable(), entity.getRuleCnt(), entity.getFlowCtrlInfo())) {
                    metaStoreService.updGroupResCtrlConf(newEntity, sBuffer, result);
                } else {
                    result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                            DataOpErrCode.DERR_UNCHANGED.getDescription());
                }
            }
        }
        return new GroupProcessResult(entity.getGroupName(), null, result);
    }

    /**
     * Operate group consume control configure info
     *
     * @param opEntity  the group resource control info entity will be add
     * @param groupName operate target
     * @param sBuffer   the print info string buffer
     * @param result    the process result return
     * @return true if success otherwise false
     */
    // Attention: compatible implementation for the old API
    public GroupProcessResult addOrUpdGroupResCtrlConf(BaseEntity opEntity, String groupName,
                                                       Boolean resChkEnable, int allowedB2CRate,
                                                       StringBuilder sBuffer, ProcessResult result) {
        GroupResCtrlEntity newEntity = new GroupResCtrlEntity(opEntity, groupName);
        newEntity.updModifyInfo(opEntity.getDataVerId(), resChkEnable, allowedB2CRate,
                TBaseConstants.META_VALUE_UNDEFINED, null,
                TBaseConstants.META_VALUE_UNDEFINED, null);
        return addOrUpdGroupResCtrlConf(newEntity, sBuffer, result);
    }

    // Attention: compatible implementation for the old API
    public GroupProcessResult addOrUpdGroupResCtrlConf(BaseEntity opEntity, String groupName,
                                                       int qryPriorityId, Boolean flowCtrlEnable,
                                                       int flowRuleCnt, String flowCtrlRuleInfo,
                                                       StringBuilder sBuffer, ProcessResult result) {
        GroupResCtrlEntity newEntity = new GroupResCtrlEntity(opEntity, groupName);
        newEntity.updModifyInfo(opEntity.getDataVerId(), null,
                TBaseConstants.META_VALUE_UNDEFINED, qryPriorityId,
                flowCtrlEnable, flowRuleCnt, flowCtrlRuleInfo);
        return addOrUpdGroupResCtrlConf(newEntity, sBuffer, result);
    }
    /**
     * add or update if present configure info
     *
     * @param entity  the group resource control info entity will be add
     * @param sBuffer   the print info string buffer
     * @param result    the process result return
     * @return true if success otherwise false
     */
    // Attention: compatible implementation for the old API
    public GroupProcessResult addOrUpdGroupResCtrlConf(GroupResCtrlEntity entity,
                                                       StringBuilder sBuffer,
                                                       ProcessResult result) {
        GroupResCtrlEntity newEntity;
        GroupResCtrlEntity curEntity =
                metaStoreService.getGroupResCtrlConf(entity.getGroupName());
        if (curEntity == null) {
            newEntity = new GroupResCtrlEntity(entity, entity.getGroupName());
            newEntity.fillDefaultValue();
            newEntity.updModifyInfo(entity.getDataVerId(), entity.isEnableResCheck(),
                    entity.getAllowedBrokerClientRate(), entity.getQryPriorityId(),
                    entity.isFlowCtrlEnable(), entity.getRuleCnt(), entity.getFlowCtrlInfo());
            metaStoreService.addGroupResCtrlConf(newEntity, sBuffer, result);
        } else {
            newEntity = curEntity.clone();
            newEntity.updBaseModifyInfo(entity);
            if (newEntity.updModifyInfo(entity.getDataVerId(), entity.isEnableResCheck(),
                    entity.getAllowedBrokerClientRate(), entity.getQryPriorityId(),
                    entity.isFlowCtrlEnable(), entity.getRuleCnt(), entity.getFlowCtrlInfo())) {
                metaStoreService.updGroupResCtrlConf(newEntity, sBuffer, result);
            } else {
                result.setSuccResult(null);
            }
        }
        return new GroupProcessResult(entity.getGroupName(), null, result);
    }

    /**
     * Delete group resource control configure
     *
     * @param operator   operator
     * @param groupNames the group will be deleted
     * @param sBuffer  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    public List<GroupProcessResult> delGroupResCtrlConf(String operator,
                                                        Set<String> groupNames,
                                                        StringBuilder sBuffer,
                                                        ProcessResult result) {
        List<GroupProcessResult> retInfo = new ArrayList<>();
        if (groupNames == null || groupNames.isEmpty()) {
            return retInfo;
        }
        for (String groupName : groupNames) {
            if (metaStoreService.hasGroupConsumeCtrlConf(groupName)) {
                result.setFailResult(DataOpErrCode.DERR_CONDITION_LACK.getCode(),
                        sBuffer.append("Group ").append(groupName)
                                .append(" has consume control configures,")
                                .append(", please delete consume control configures first!")
                                .toString());
                sBuffer.delete(0, sBuffer.length());
                retInfo.add(new GroupProcessResult(groupName, null, result));
                continue;
            }
            metaStoreService.delGroupResCtrlConf(operator, groupName, sBuffer, result);
            retInfo.add(new GroupProcessResult(groupName, null, result));
            sBuffer.delete(0, sBuffer.length());
            result.clear();
        }
        return retInfo;
    }

    public Map<String, GroupResCtrlEntity> confGetGroupResCtrlConf(Set<String> groupSet,
                                                                   GroupResCtrlEntity qryEntity) {
        return metaStoreService.getGroupResCtrlConf(groupSet, qryEntity);
    }

    public GroupResCtrlEntity confGetGroupResCtrlConf(String groupName) {
        return this.metaStoreService.getGroupResCtrlConf(groupName);
    }

    public GroupProcessResult addOrUpdGroupConsumeCtrlInfo(boolean isAddOp, BaseEntity opEntity,
                                                           String groupName, String topicName,
                                                           Boolean enableCsm, String disableRsn,
                                                           Boolean enableFlt, String fltCondStr,
                                                           StringBuilder sBuffer,
                                                           ProcessResult result) {
        GroupConsumeCtrlEntity entity =
                new GroupConsumeCtrlEntity(opEntity, groupName, topicName);
        entity.updModifyInfo(opEntity.getDataVerId(),
                enableCsm, disableRsn, enableFlt, fltCondStr);
        return addOrUpdGroupConsumeCtrlInfo(isAddOp, entity, sBuffer, result);
    }

    public GroupProcessResult addOrUpdGroupConsumeCtrlInfo(boolean isAddOp,
                                                           GroupConsumeCtrlEntity entity,
                                                           StringBuilder sBuffer,
                                                           ProcessResult result) {
        // add group resource control record
        if (!addIfAbsentGroupResConf(entity, entity.getGroupName(), sBuffer, result)) {
            return new GroupProcessResult(entity.getGroupName(), entity.getTopicName(), result);
        }
        if (!addIfAbsentTopicCtrlConf(entity.getTopicName(),
                entity.getModifyUser(), sBuffer, result)) {
            return new GroupProcessResult(entity.getGroupName(), entity.getTopicName(), result);
        }
        GroupConsumeCtrlEntity curEntity =
                metaStoreService.getGroupConsumeCtrlConfByRecKey(entity.getRecordKey());
        if (isAddOp) {
            if (curEntity == null) {
                metaStoreService.addGroupConsumeCtrlConf(entity, sBuffer, result);
            } else {
                result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                        DataOpErrCode.DERR_EXISTED.getDescription());
            }
        } else {
            if (curEntity == null) {
                result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                        DataOpErrCode.DERR_NOT_EXIST.getDescription());
            } else {
                GroupConsumeCtrlEntity newEntity = curEntity.clone();
                newEntity.updBaseModifyInfo(entity);
                if (newEntity.updModifyInfo(entity.getDataVerId(),
                        entity.isEnableConsume(), entity.getDisableReason(),
                        entity.isEnableFilterConsume(), entity.getFilterCondStr())) {
                    metaStoreService.updGroupConsumeCtrlConf(newEntity, sBuffer, result);
                } else {
                    result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                            DataOpErrCode.DERR_UNCHANGED.getDescription());
                }
            }
        }
        return new GroupProcessResult(entity.getGroupName(), entity.getTopicName(), result);
    }

    // Attention: compatible implementation for the old API
    public GroupProcessResult addOrUpdGroupConsumeCtrlInfo(BaseEntity opEntity, String groupName,
                                                           String topicName, Boolean enableCsm,
                                                           String disReason, Boolean enableFlt,
                                                           String fltCondStr, StringBuilder sBuffer,
                                                           ProcessResult result) {
        GroupConsumeCtrlEntity entity =
                new GroupConsumeCtrlEntity(opEntity, groupName, topicName);
        entity.updModifyInfo(opEntity.getDataVerId(),
                enableCsm, disReason, enableFlt, fltCondStr);
        return addOrUpdGroupConsumeCtrlInfo(entity, sBuffer, result);
    }

    // Attention: compatible implementation for the old API
    public GroupProcessResult addOrUpdGroupConsumeCtrlInfo(GroupConsumeCtrlEntity entity,
                                                           StringBuilder sBuffer,
                                                           ProcessResult result) {
        // add group resource control record
        if (!addIfAbsentGroupResConf(entity, entity.getGroupName(), sBuffer, result)) {
            return new GroupProcessResult(entity.getGroupName(), entity.getTopicName(), result);
        }
        // add topic control record
        if (!addIfAbsentTopicCtrlConf(entity.getTopicName(),
                entity.getModifyUser(), sBuffer, result)) {
            return new GroupProcessResult(entity.getGroupName(), entity.getTopicName(), result);
        }
        GroupConsumeCtrlEntity newEntity;
        GroupConsumeCtrlEntity curEntity =
                metaStoreService.getGroupConsumeCtrlConfByRecKey(entity.getRecordKey());
        if (curEntity == null) {
            newEntity = new GroupConsumeCtrlEntity(entity,
                    entity.getGroupName(), entity.getTopicName());
            newEntity.updModifyInfo(entity.getDataVerId(),
                    entity.isEnableConsume(), entity.getDisableReason(),
                    entity.isEnableFilterConsume(), entity.getFilterCondStr());
            metaStoreService.addGroupConsumeCtrlConf(newEntity, sBuffer, result);
        } else {
            newEntity = curEntity.clone();
            newEntity.updBaseModifyInfo(entity);
            if (newEntity.updModifyInfo(entity.getDataVerId(),
                    entity.isEnableConsume(), entity.getDisableReason(),
                    entity.isEnableFilterConsume(), entity.getFilterCondStr())) {
                metaStoreService.updGroupConsumeCtrlConf(newEntity, sBuffer, result);
            } else {
                result.setSuccResult(null);
            }
        }
        return new GroupProcessResult(entity.getGroupName(), entity.getTopicName(), result);
    }

    public List<GroupProcessResult> delGroupConsumeCtrlConf(String operator,
                                                            Set<String> groupNameSet,
                                                            Set<String> topicNameSet,
                                                            StringBuilder sBuffer,
                                                            ProcessResult result) {
        List<GroupProcessResult> retInfo = new ArrayList<>();
        if ((groupNameSet == null || groupNameSet.isEmpty())
                && (topicNameSet == null || topicNameSet.isEmpty())) {
            return retInfo;
        }
        Set<String> rmvRecords = new HashSet<>();
        if (groupNameSet != null && !groupNameSet.isEmpty()) {
            rmvRecords.addAll(metaStoreService.getConsumeCtrlKeyByGroupName(groupNameSet));
        }
        if (topicNameSet != null && !topicNameSet.isEmpty()) {
            rmvRecords.addAll(metaStoreService.getConsumeCtrlKeyByTopicName(topicNameSet));
        }
        for (String recKey : rmvRecords) {
            Tuple2<String, String> groupTopicTuple =
                    KeyBuilderUtils.splitRecKey2GroupTopic(recKey);
            metaStoreService.delGroupConsumeCtrlConf(operator, recKey, sBuffer, result);
            retInfo.add(new GroupProcessResult(groupTopicTuple.getF1(),
                    groupTopicTuple.getF0(), result));
        }
        return retInfo;
    }

    private boolean addIfAbsentGroupResConf(BaseEntity opEntity, String groupName,
                                            StringBuilder sBuffer, ProcessResult result) {
        GroupResCtrlEntity resCtrlEntity =
                this.metaStoreService.getGroupResCtrlConf(groupName);
        if (resCtrlEntity != null) {
            result.setSuccResult(null);
            return true;
        }
        resCtrlEntity = new GroupResCtrlEntity(opEntity, groupName);
        resCtrlEntity.fillDefaultValue();
        return this.metaStoreService.addGroupResCtrlConf(resCtrlEntity, sBuffer, result);
    }

    /**
     * Query group consume control configure by query entity
     *
     * @param qryEntity the entity to get matched condition,
     *                  may be null, will return all group filter condition
     * @return group consume control list
     */
    public List<GroupConsumeCtrlEntity> confGetGroupConsumeCtrlConf(
            GroupConsumeCtrlEntity qryEntity) {
        return metaStoreService.getGroupConsumeCtrlConf(qryEntity);
    }

    /**
     * Get all group consume control record for a specific topic
     *
     * @param topicName
     * @return group consume control list
     */
    public List<GroupConsumeCtrlEntity> getConsumeCtrlByTopic(String topicName) {
        return metaStoreService.getConsumeCtrlByTopicName(topicName);
    }

    public Set<String> getDisableConsumeTopicByGroupName(String groupName) {
        Set<String> disTopicSet = new HashSet<>();
        List<GroupConsumeCtrlEntity> qryResult =
                metaStoreService.getConsumeCtrlByGroupName(groupName);
        if (qryResult.isEmpty()) {
            return disTopicSet;
        }
        for (GroupConsumeCtrlEntity ctrlEntity : qryResult) {
            if (!ctrlEntity.isEnableConsume()) {
                disTopicSet.add(ctrlEntity.getTopicName());
            }
        }
        return disTopicSet;
    }

    /**
     * Get group consume control configure for a topic & group
     *
     * @param topicName the topic name
     * @param groupName the group name
     * @return group consume control record
     */
    public GroupConsumeCtrlEntity getGroupConsumeCtrlConf(String groupName,
                                                          String topicName) {
        return metaStoreService.getConsumeCtrlByGroupAndTopic(groupName, topicName);
    }

    /**
     * Get group consume control configure for topic & group set
     *
     * @param groupSet the topic name
     * @param topicSet the group name
     * @return group consume control record
     */
    public Map<String, List<GroupConsumeCtrlEntity>> getGroupConsumeCtrlConf(
            Set<String> groupSet, Set<String> topicSet, GroupConsumeCtrlEntity qryEntry) {
        return metaStoreService.getConsumeCtrlInfoMap(groupSet, topicSet, qryEntry);
    }

    // //////////////////////////////////////////////////////////////////////////////


}
