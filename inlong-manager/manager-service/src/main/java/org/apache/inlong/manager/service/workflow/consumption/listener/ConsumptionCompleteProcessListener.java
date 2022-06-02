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

package org.apache.inlong.manager.service.workflow.consumption.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.common.pojo.dataproxy.PulsarClusterInfo;
import org.apache.inlong.manager.common.beans.ClusterBean;
import org.apache.inlong.manager.common.enums.ConsumptionStatus;
import org.apache.inlong.manager.common.enums.MQType;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.pulsar.PulsarTopicBean;
import org.apache.inlong.manager.common.pojo.tubemq.AddTubeConsumeGroupRequest;
import org.apache.inlong.manager.common.pojo.workflow.form.NewConsumptionProcessForm;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.ConsumptionEntity;
import org.apache.inlong.manager.dao.mapper.ConsumptionEntityMapper;
import org.apache.inlong.manager.service.CommonOperateService;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.mq.util.PulsarOptService;
import org.apache.inlong.manager.service.mq.util.TubeMqOptService;
import org.apache.inlong.manager.service.mq.util.PulsarUtils;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Added data consumption process complete archive event listener
 */
@Slf4j
@Component
public class ConsumptionCompleteProcessListener implements ProcessEventListener {

    @Autowired
    private PulsarOptService pulsarMqOptService;
    @Autowired
    private ClusterBean clusterBean;
    @Autowired
    private CommonOperateService commonOperateService;
    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private ConsumptionEntityMapper consumptionMapper;
    @Autowired
    private TubeMqOptService tubeMqOptService;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        NewConsumptionProcessForm consumptionForm = (NewConsumptionProcessForm) context.getProcessForm();

        // Real-time query of consumption information
        Integer consumptionId = consumptionForm.getConsumptionInfo().getId();
        ConsumptionEntity entity = consumptionMapper.selectByPrimaryKey(consumptionId);
        if (entity == null) {
            throw new WorkflowListenerException("consumption not exits for id=" + consumptionId);
        }

        MQType mqType = MQType.forType(entity.getMqType());
        if (mqType == MQType.TUBE) {
            this.createTubeConsumerGroup(entity);
            return ListenerResult.success("Create Tube consumer group successful");
        } else if (mqType == MQType.PULSAR || mqType == MQType.TDMQ_PULSAR) {
            this.createPulsarTopicMessage(entity);
        } else {
            throw new WorkflowListenerException("Unsupported MQ type [" + mqType + "]");
        }

        this.updateConsumerInfo(consumptionId, entity.getConsumerGroup());
        return ListenerResult.success("Create MQ consumer group successful");
    }

    /**
     * Update consumption after approve
     */
    private void updateConsumerInfo(Integer consumptionId, String consumerGroup) {
        ConsumptionEntity update = new ConsumptionEntity();
        update.setId(consumptionId);
        update.setStatus(ConsumptionStatus.APPROVED.getStatus());
        update.setConsumerGroup(consumerGroup);
        update.setModifyTime(new Date());
        consumptionMapper.updateByPrimaryKeySelective(update);
    }

    /**
     * Create Pulsar consumption information
     */
    private void createPulsarTopicMessage(ConsumptionEntity entity) {
        String groupId = entity.getInlongGroupId();
        InlongGroupInfo groupInfo = groupService.get(groupId);
        Preconditions.checkNotNull(groupInfo, "inlong group not found for groupId=" + groupId);
        String mqResource = groupInfo.getMqResource();
        Preconditions.checkNotNull(mqResource, "mq resource cannot empty for groupId=" + groupId);
        PulsarClusterInfo pulsarClusterInfo = commonOperateService.getPulsarClusterInfo(entity.getMqType());
        try (PulsarAdmin pulsarAdmin = PulsarUtils.getPulsarAdmin(pulsarClusterInfo)) {
            PulsarTopicBean topicMessage = new PulsarTopicBean();
            String tenant = clusterBean.getDefaultTenant();
            topicMessage.setTenant(tenant);
            topicMessage.setNamespace(mqResource);

            String consumerGroup = entity.getConsumerGroup();
            List<String> topics = Arrays.asList(entity.getTopic().split(","));
            this.createPulsarSubscription(pulsarAdmin, consumerGroup, topicMessage, topics);
        } catch (Exception e) {
            log.error("create pulsar topic failed", e);
            throw new WorkflowListenerException("failed to create pulsar topic for groupId=" + groupId + ", reason: "
                    + e.getMessage());
        }
    }

    private void createPulsarSubscription(PulsarAdmin pulsarAdmin, String subscription, PulsarTopicBean topicBean,
            List<String> topics) {
        try {
            pulsarMqOptService.createSubscriptions(pulsarAdmin, subscription, topicBean, topics);
        } catch (Exception e) {
            log.error("create pulsar consumer group failed", e);
            throw new WorkflowListenerException("failed to create pulsar consumer group");
        }
    }

    /**
     * Create tube consumer group
     */
    private void createTubeConsumerGroup(ConsumptionEntity consumption) {
        AddTubeConsumeGroupRequest addTubeConsumeGroupRequest = new AddTubeConsumeGroupRequest();
        addTubeConsumeGroupRequest.setClusterId(1); // TODO is cluster id needed?
        addTubeConsumeGroupRequest.setCreateUser(consumption.getCreator());
        AddTubeConsumeGroupRequest.GroupNameJsonSetBean bean = new AddTubeConsumeGroupRequest.GroupNameJsonSetBean();
        bean.setTopicName(consumption.getTopic());
        bean.setGroupName(consumption.getConsumerGroup());
        addTubeConsumeGroupRequest.setGroupNameJsonSet(Collections.singletonList(bean));

        try {
            tubeMqOptService.createNewConsumerGroup(addTubeConsumeGroupRequest);
        } catch (Exception e) {
            throw new WorkflowListenerException("failed to create tube consumer group: " + addTubeConsumeGroupRequest);
        }
    }

    @Override
    public boolean async() {
        return false;
    }

}
