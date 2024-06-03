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

package org.apache.inlong.manager.service.core.impl;

import org.apache.inlong.audit.AuditOperator;
import org.apache.inlong.audit.entity.AuditInformation;
import org.apache.inlong.audit.entity.FlowType;
import org.apache.inlong.common.enums.IndicatorType;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.AuditQuerySource;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.TimeStaticsDim;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.dao.mapper.AuditEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.pojo.audit.AuditInfo;
import org.apache.inlong.manager.pojo.audit.AuditRequest;
import org.apache.inlong.manager.pojo.audit.AuditVO;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.user.UserRoleCode;
import org.apache.inlong.manager.service.audit.AuditRunnable;
import org.apache.inlong.manager.service.core.AuditService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Audit service layer implementation
 */
@Lazy
@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditServiceImpl.class);
    private static final Gson GSON = new GsonBuilder().create();
    private static final String SECOND_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String HOUR_FORMAT = "yyyy-MM-dd HH";
    private static final String DAY_FORMAT = "yyyy-MM-dd";
    private static final DateTimeFormatter SECOND_DATE_FORMATTER = DateTimeFormat.forPattern(SECOND_FORMAT);
    private static final DateTimeFormatter HOUR_DATE_FORMATTER = DateTimeFormat.forPattern(HOUR_FORMAT);
    private static final DateTimeFormatter DAY_DATE_FORMATTER = DateTimeFormat.forPattern(DAY_FORMAT);
    // key 1: type of audit, like pulsar, hive, key 2: indicator type, value : entity of audit base item
    private final Map<String, Map<Integer, AuditInformation>> auditIndicatorMap = new ConcurrentHashMap<>();
    private final Map<String, AuditInformation> auditItemMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    // defaults to return all audit ids, can be overwritten in properties file
    // see audit id definitions: https://inlong.apache.org/docs/modules/audit/overview#audit-id
    @Value("#{'${audit.admin.ids:3,4,5,6}'.split(',')}")
    private List<String> auditIdListForAdmin;
    @Value("#{'${audit.user.ids:3,4,5,6}'.split(',')}")
    private List<String> auditIdListForUser;

    @Value("${audit.query.source}")
    private String auditQuerySource;
    @Value("${audit.query.url:http://127.0.0.1:10080}")
    private String auditQueryUrl;

    @Autowired
    private AuditEntityMapper auditEntityMapper;
    @Autowired
    private StreamSinkEntityMapper sinkEntityMapper;
    @Autowired
    private StreamSourceEntityMapper sourceEntityMapper;
    @Autowired
    private InlongGroupEntityMapper inlongGroupMapper;
    @Autowired
    private RestTemplate restTemplate;

    @PostConstruct
    public void initialize() {
        LOGGER.info("init audit base item cache map for {}", AuditServiceImpl.class.getSimpleName());
        try {
            refreshBaseItemCache();
        } catch (Throwable t) {
            LOGGER.error("initialize audit base item cache error", t);
        }
    }

    @Override
    public Boolean refreshBaseItemCache() {
        LOGGER.debug("start to reload audit base item info");
        try {
            auditIndicatorMap.clear();
        } catch (Throwable t) {
            LOGGER.error("failed to reload audit base item info", t);
            return false;
        }

        LOGGER.debug("success to reload audit base item info");
        return true;
    }

    @Override
    public String getAuditId(String type, IndicatorType indicatorType) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        Map<Integer, AuditInformation> itemMap = auditIndicatorMap.computeIfAbsent(type, v -> new HashMap<>());
        AuditInformation auditInformation = itemMap.get(indicatorType.getCode());
        if (auditInformation != null) {
            return String.valueOf(auditInformation.getAuditId());
        }
        FlowType flowType = indicatorType.getCode() % 2 == 0 ? FlowType.INPUT : FlowType.OUTPUT;
        auditInformation = AuditOperator.getInstance().buildAuditInformation(type, flowType,
                IndicatorType.isFailedType(indicatorType),
                true,
                IndicatorType.isDiscardType(indicatorType),
                IndicatorType.isRetryType(indicatorType));
        Preconditions.expectNotNull(auditInformation, ErrorCodeEnum.AUDIT_ID_TYPE_NOT_SUPPORTED,
                String.format(ErrorCodeEnum.AUDIT_ID_TYPE_NOT_SUPPORTED.getMessage(), type));
        itemMap.put(indicatorType.getCode(), auditInformation);
        return String.valueOf(auditInformation.getAuditId());
    }

    @Override
    public List<AuditVO> listByCondition(AuditRequest request) throws Exception {
        LOGGER.info("begin query audit list request={}", request);
        Preconditions.expectNotNull(request, "request is null");

        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();

        // for now, we use the first sink type only.
        // this is temporary behavior before multiple sinks in one stream is fully supported.
        String sinkNodeType = null;
        String sourceNodeType = null;
        Integer sinkId = request.getSinkId();
        StreamSinkEntity sinkEntity = null;
        List<StreamSinkEntity> sinkEntityList = sinkEntityMapper.selectByRelatedId(groupId, streamId);
        if (sinkId != null) {
            sinkEntity = sinkEntityMapper.selectByPrimaryKey(sinkId);
        } else if (CollectionUtils.isNotEmpty(sinkEntityList)) {
            sinkEntity = sinkEntityList.get(0);
        }

        // if sink info is existed, get sink type for query audit info.
        if (sinkEntity != null) {
            sinkNodeType = sinkEntity.getSinkType();
        }
        Map<String, String> auditIdMap = new HashMap<>();

        if (StringUtils.isNotBlank(groupId)) {
            InlongGroupEntity groupEntity = inlongGroupMapper.selectByGroupId(groupId);
            List<StreamSourceEntity> sourceEntityList = sourceEntityMapper.selectByRelatedId(groupId, streamId, null);
            if (CollectionUtils.isNotEmpty(sourceEntityList)) {
                sourceNodeType = sourceEntityList.get(0).getSourceType();
            }

            auditIdMap.put(getAuditId(sinkNodeType, IndicatorType.SEND_SUCCESS), sinkNodeType);

            if (CollectionUtils.isEmpty(request.getAuditIds())) {
                // properly overwrite audit ids by role and stream config
                if (InlongConstants.DATASYNC_MODE.equals(groupEntity.getInlongGroupMode())) {
                    auditIdMap.put(getAuditId(sourceNodeType, IndicatorType.RECEIVED_SUCCESS), sourceNodeType);
                    request.setAuditIds(getAuditIds(groupId, streamId, sourceNodeType, sinkNodeType));
                } else {
                    auditIdMap.put(getAuditId(sinkNodeType, IndicatorType.RECEIVED_SUCCESS), sinkNodeType);
                    request.setAuditIds(getAuditIds(groupId, streamId, null, sinkNodeType));
                }
            }
        } else if (CollectionUtils.isEmpty(request.getAuditIds())) {
            throw new BusinessException("audits id is empty");
        }

        List<AuditVO> result = new ArrayList<>();
        AuditQuerySource querySource = AuditQuerySource.valueOf(auditQuerySource);
        CountDownLatch latch = new CountDownLatch(request.getAuditIds().size());
        for (String auditId : request.getAuditIds()) {
            AuditInformation auditInformation = auditItemMap.get(auditId);
            String auditName = auditInformation != null ? auditInformation.getNameInChinese() : "";

            if (AuditQuerySource.MYSQL == querySource) {
                String format = "%Y-%m-%d %H:%i:00";
                // Support min agg at now
                DateTime endDate = DAY_DATE_FORMATTER.parseDateTime(request.getEndDate());
                String endDateStr = endDate.plusDays(1).toString(DAY_DATE_FORMATTER);
                List<Map<String, Object>> sumList =
                        StringUtils.isNotBlank(request.getIp()) ? auditEntityMapper.sumByLogTsAndIp(request.getIp(),
                                auditId, request.getStartDate(), endDateStr, format)
                                : auditEntityMapper.sumByLogTs(groupId, streamId, auditId, request.getStartDate(),
                                        endDateStr, format);
                List<AuditInfo> auditSet = sumList.stream().map(s -> {
                    AuditInfo vo = new AuditInfo();
                    vo.setInlongGroupId((String) s.get("inlongGroupId"));
                    vo.setInlongStreamId((String) s.get("inlongStreamId"));
                    vo.setLogTs((String) s.get("logTs"));
                    vo.setCount(((BigDecimal) s.get("total")).longValue());
                    vo.setDelay(((BigDecimal) s.get("totalDelay")).longValue());
                    vo.setSize(((BigDecimal) s.get("totalSize")).longValue());
                    return vo;
                }).collect(Collectors.toList());
                result.add(new AuditVO(auditId, auditName, auditSet, auditIdMap.getOrDefault(auditId, null)));
            } else {
                this.executor.execute(new AuditRunnable(request, auditId, auditName, result, latch, restTemplate,
                        auditQueryUrl, auditIdMap, false));
            }
        }
        if (AuditQuerySource.MYSQL != querySource) {
            latch.await(30, TimeUnit.SECONDS);
        } else {
            result = aggregateByTimeDim(result, request.getTimeStaticsDim());
        }
        LOGGER.info("success to query audit list for request={}", request);
        return result;
    }

    @Override
    public List<AuditVO> listAll(AuditRequest request) throws Exception {
        List<AuditVO> result = new ArrayList<>();
        AuditQuerySource querySource = AuditQuerySource.valueOf(auditQuerySource);
        CountDownLatch latch = new CountDownLatch(request.getAuditIds().size());
        for (String auditId : request.getAuditIds()) {
            AuditInformation auditInformation = auditItemMap.get(auditId);
            String auditName = "";
            if (auditInformation != null) {
                auditName = auditInformation.getNameInChinese();
            }
            if (AuditQuerySource.MYSQL == querySource) {
                // Support min agg at now
                DateTime endDate = SECOND_DATE_FORMATTER.parseDateTime(request.getEndDate());
                String endDateStr = endDate.plusDays(1).toString(SECOND_DATE_FORMATTER);
                List<Map<String, Object>> sumList = auditEntityMapper.sumGroupByIp(
                        request.getInlongGroupId(), request.getInlongStreamId(), request.getIp(), auditId,
                        request.getStartDate(),
                        endDateStr);
                List<AuditInfo> auditSet = sumList.stream().map(s -> {
                    AuditInfo vo = new AuditInfo();
                    vo.setInlongGroupId((String) s.get("inlongGroupId"));
                    vo.setInlongStreamId((String) s.get("inlongStreamId"));
                    vo.setLogTs((String) s.get("logTs"));
                    vo.setIp((String) s.get("ip"));
                    vo.setCount(((BigDecimal) s.get("total")).longValue());
                    vo.setDelay(((BigDecimal) s.get("totalDelay")).longValue());
                    vo.setSize(((BigDecimal) s.get("totalSize")).longValue());
                    return vo;
                }).collect(Collectors.toList());
                result.add(new AuditVO(auditId, auditName, auditSet, null));
            } else {
                this.executor.execute(new AuditRunnable(request, auditId, auditName, result, latch, restTemplate,
                        auditQueryUrl, null, true));
            }
        }
        if (AuditQuerySource.MYSQL != querySource) {
            latch.await(30, TimeUnit.SECONDS);
        }
        return result;
    }

    @Override
    public List<AuditInformation> getAuditBases() {
        List<AuditInformation> auditInformations = AuditOperator.getInstance().getAllAuditInformation();
        return auditInformations;
    }

    private List<String> getAuditIds(String groupId, String streamId, String sourceNodeType, String sinkNodeType) {
        Set<String> auditSet = LoginUserUtils.getLoginUser().getRoles().contains(UserRoleCode.TENANT_ADMIN)
                ? new HashSet<>(auditIdListForAdmin)
                : new HashSet<>(auditIdListForUser);

        // if no sink is configured, return data-proxy output instead of sort
        if (sinkNodeType == null) {
            auditSet.add(getAuditId(ClusterType.DATAPROXY, IndicatorType.SEND_SUCCESS));
        } else {
            auditSet.add(getAuditId(sinkNodeType, IndicatorType.SEND_SUCCESS));
            InlongGroupEntity inlongGroup = inlongGroupMapper.selectByGroupId(groupId);
            if (InlongConstants.DATASYNC_MODE.equals(inlongGroup.getInlongGroupMode())) {
                auditSet.add(getAuditId(sourceNodeType, IndicatorType.RECEIVED_SUCCESS));
            } else {
                auditSet.add(getAuditId(sinkNodeType, IndicatorType.RECEIVED_SUCCESS));
            }
        }

        // auto push source has no agent, return data-proxy audit data instead of agent
        List<StreamSourceEntity> sourceList = sourceEntityMapper.selectByRelatedId(groupId, streamId, null);
        if (CollectionUtils.isEmpty(sourceList)
                || sourceList.stream().allMatch(s -> SourceType.AUTO_PUSH.equals(s.getSourceType()))) {
            // need data_proxy received type when agent has received type
            boolean dpReceivedNeeded = auditSet.contains(getAuditId(ClusterType.AGENT, IndicatorType.RECEIVED_SUCCESS));
            if (dpReceivedNeeded) {
                auditSet.add(getAuditId(ClusterType.DATAPROXY, IndicatorType.RECEIVED_SUCCESS));
            }
        }

        return new ArrayList<>(auditSet);
    }

    /**
     * Aggregate by time dim
     */
    private List<AuditVO> aggregateByTimeDim(List<AuditVO> auditVOList, TimeStaticsDim timeStaticsDim) {
        List<AuditVO> result;
        switch (timeStaticsDim) {
            case HOUR:
                result = doAggregate(auditVOList, HOUR_FORMAT);
                break;
            case DAY:
                result = doAggregate(auditVOList, DAY_FORMAT);
                break;
            default:
                result = doAggregate(auditVOList, SECOND_FORMAT);
                break;
        }
        return result;
    }

    /**
     * Execute the aggregate by the given time format
     */
    private List<AuditVO> doAggregate(List<AuditVO> auditVOList, String format) {
        List<AuditVO> result = new ArrayList<>();
        for (AuditVO auditVO : auditVOList) {
            AuditVO statInfo = new AuditVO();
            HashMap<String, AtomicLong> countMap = new HashMap<>();
            HashMap<String, AtomicLong> delayMap = new HashMap<>();
            HashMap<String, AtomicLong> sizeMap = new HashMap<>();
            statInfo.setAuditId(auditVO.getAuditId());
            statInfo.setAuditName(auditVO.getAuditName());
            statInfo.setNodeType(auditVO.getNodeType());
            for (AuditInfo auditInfo : auditVO.getAuditSet()) {
                String statKey = formatLogTime(auditInfo.getLogTs(), format);
                if (statKey == null) {
                    continue;
                }
                countMap.computeIfAbsent(statKey, k -> new AtomicLong(0)).addAndGet(auditInfo.getCount());
                delayMap.computeIfAbsent(statKey, k -> new AtomicLong(0)).addAndGet(auditInfo.getDelay());
                sizeMap.computeIfAbsent(statKey, k -> new AtomicLong(0)).addAndGet(auditInfo.getSize());
            }

            List<AuditInfo> auditInfoList = new LinkedList<>();
            for (Map.Entry<String, AtomicLong> entry : countMap.entrySet()) {
                AuditInfo auditInfoStat = new AuditInfo();
                auditInfoStat.setLogTs(entry.getKey());
                long count = entry.getValue().get();
                auditInfoStat.setCount(entry.getValue().get());
                auditInfoStat.setDelay(count == 0 ? 0 : delayMap.get(entry.getKey()).get() / count);
                auditInfoStat.setSize(count == 0 ? 0 : sizeMap.get(entry.getKey()).get() / count);
                auditInfoList.add(auditInfoStat);
            }
            statInfo.setAuditSet(auditInfoList);
            result.add(statInfo);
        }
        return result;
    }

    /**
     * Format the log time
     */
    private String formatLogTime(String dateString, String format) {
        String formatDateString = null;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(format);
            Date date = formatter.parse(dateString);
            formatDateString = formatter.format(date);
        } catch (Exception e) {
            LOGGER.error("format lot time exception", e);
        }
        return formatDateString;
    }

}
