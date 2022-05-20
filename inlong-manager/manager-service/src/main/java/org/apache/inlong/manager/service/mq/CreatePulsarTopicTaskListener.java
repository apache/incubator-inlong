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

package org.apache.inlong.manager.service.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.common.pojo.dataproxy.PulsarClusterInfo;
import org.apache.inlong.manager.common.beans.ClusterBean;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.common.pojo.pulsar.PulsarTopicBean;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.StreamResourceProcessForm;
import org.apache.inlong.manager.service.CommonOperateService;
import org.apache.inlong.manager.service.mq.util.PulsarOptService;
import org.apache.inlong.manager.service.mq.util.PulsarUtils;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.QueueOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Create task listener for Pulsar Topic
 */
@Slf4j
@Component
public class CreatePulsarTopicTaskListener implements QueueOperateListener {

    @Autowired
    private CommonOperateService commonOperateService;
    @Autowired
    private ClusterBean clusterBean;
    @Autowired
    private PulsarOptService pulsarOptService;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        StreamResourceProcessForm form = (StreamResourceProcessForm) context.getProcessForm();
        InlongGroupInfo groupInfo = form.getGroupInfo();
        InlongStreamInfo streamInfo = form.getStreamInfo();
        final String groupId = streamInfo.getInlongGroupId();
        final String streamId = streamInfo.getInlongStreamId();
        log.info("begin to create pulsar topic for groupId={}, streamId={}", groupId, streamId);

        InlongPulsarInfo pulsarInfo = (InlongPulsarInfo) groupInfo;
        PulsarClusterInfo globalCluster = commonOperateService.getPulsarClusterInfo(pulsarInfo.getMqType());
        try (PulsarAdmin globalPulsarAdmin = PulsarUtils.getPulsarAdmin(globalCluster)) {
            List<String> pulsarClusters = PulsarUtils.getPulsarClusters(globalPulsarAdmin);
            for (String cluster : pulsarClusters) {
                String serviceUrl = PulsarUtils.getServiceUrl(globalPulsarAdmin, cluster);
                PulsarClusterInfo pulsarClusterInfo = PulsarClusterInfo.builder()
                        .token(globalCluster.getToken()).adminUrl(serviceUrl).build();
                String pulsarTopic = streamInfo.getMqResource();
                this.createTopic(pulsarInfo, pulsarTopic, pulsarClusterInfo);
            }
        } catch (Exception e) {
            log.error("create pulsar topic error for groupId={}, streamId={}", groupId, streamId, e);
            throw new WorkflowListenerException(
                    "create pulsar topic error for groupId=" + groupId + ", streamId=" + streamId);
        }

        log.info("success to create pulsar topic for groupId={}, streamId={}", groupId, streamId);
        return ListenerResult.success();
    }

    private void createTopic(InlongPulsarInfo groupInfo, String pulsarTopic, PulsarClusterInfo pulsarClusterInfo)
            throws Exception {
        Integer partitionNum = groupInfo.getPartitionNum();
        int partition = 0;
        if (partitionNum != null && partitionNum > 0) {
            partition = partitionNum;
        }

        try (PulsarAdmin pulsarAdmin = PulsarUtils.getPulsarAdmin(pulsarClusterInfo)) {
            PulsarTopicBean topicBean = PulsarTopicBean.builder()
                    .tenant(clusterBean.getDefaultTenant())
                    .namespace(groupInfo.getMqResource())
                    .topicName(pulsarTopic)
                    .numPartitions(partition)
                    .queueModule(groupInfo.getQueueModule())
                    .build();
            pulsarOptService.createTopic(pulsarAdmin, topicBean);
        }
    }

    @Override
    public boolean async() {
        return false;
    }

}
