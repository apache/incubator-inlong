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

package org.apache.inlong.manager.service.core.operation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupMode;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupResetRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.ProcessQuery;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.common.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.process.LightGroupResourceProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.process.NewGroupProcessForm;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.workflow.ProcessName;
import org.apache.inlong.manager.service.workflow.WorkflowService;
import org.apache.inlong.manager.workflow.core.WorkflowQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

/**
 * Operation related to inlong group process
 */
@Service
public class InlongGroupProcessOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongGroupProcessOperation.class);

    private final ExecutorService executorService = new ThreadPoolExecutor(
            20,
            40,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("inlong-group-process-%s").build(),
            new CallerRunsPolicy());

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private WorkflowQueryService workflowQueryService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private InlongStreamService streamService;

    /**
     * Allocate resource application groups for access services and initiate an approval process
     *
     * @param groupId Inlong group id
     * @param operator Operator name
     * @return Workflow result
     */
    public WorkflowResult startProcess(String groupId, String operator) {
        LOGGER.info("begin to start approve process, groupId = {}, operator = {}", groupId, operator);
        groupService.updateStatus(groupId, GroupStatus.TO_BE_APPROVAL.getCode(), operator);
        // Initiate the approval process
        NewGroupProcessForm form = genNewGroupProcessForm(groupId);
        return workflowService.start(ProcessName.NEW_GROUP_PROCESS, operator, form);
    }

    /**
     * Suspend resource application group in an asynchronous way,
     * stop source and sort task related to application group asynchronously,
     * persist the application status if necessary.
     *
     * @return groupId
     */
    public String suspendProcessAsync(String groupId, String operator) {
        LOGGER.info("begin to suspend process asynchronously, groupId = {}, operator = {}", groupId, operator);
        groupService.updateStatus(groupId, GroupStatus.SUSPENDING.getCode(), operator);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupMode mode = GroupMode.parseGroupMode(groupInfo);
        switch (mode) {
            case NORMAL:
                GroupResourceProcessForm form = genGroupProcessForm(groupInfo, GroupOperateType.SUSPEND);
                executorService.execute(() -> workflowService.start(ProcessName.SUSPEND_GROUP_PROCESS, operator, form));
                break;
            case LIGHTWEIGHT:
                LightGroupResourceProcessForm lightForm = genLightGroupProcessForm(groupInfo, GroupOperateType.SUSPEND);
                executorService.execute(
                        () -> workflowService.start(ProcessName.SUSPEND_LIGHT_GROUP_PROCESS, operator, lightForm));
                break;
            default:
                throw new WorkflowListenerException(ErrorCodeEnum.GROUP_MODE_UNSUPPORTED.getMessage());
        }
        return groupId;
    }

    /**
     * Suspend resource application group which is started up successfully,
     * stop source and sort task related to application group asynchronously,
     * persist the application status if necessary.
     *
     * @return Workflow result
     */
    public WorkflowResult suspendProcess(String groupId, String operator) {
        LOGGER.info("begin to suspend process, groupId = {}, operator = {}", groupId, operator);
        groupService.updateStatus(groupId, GroupStatus.SUSPENDING.getCode(), operator);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupMode mode = GroupMode.parseGroupMode(groupInfo);
        WorkflowResult result;
        switch (mode) {
            case NORMAL:
                GroupResourceProcessForm form = genGroupProcessForm(groupInfo, GroupOperateType.SUSPEND);
                result = workflowService.start(ProcessName.SUSPEND_GROUP_PROCESS, operator, form);
                break;
            case LIGHTWEIGHT:
                LightGroupResourceProcessForm lightForm = genLightGroupProcessForm(groupInfo, GroupOperateType.SUSPEND);
                result = workflowService.start(ProcessName.SUSPEND_LIGHT_GROUP_PROCESS, operator, lightForm);
                break;
            default:
                throw new WorkflowListenerException(ErrorCodeEnum.GROUP_MODE_UNSUPPORTED.getMessage());
        }
        return result;
    }

    /**
     * Restart resource application group in an asynchronous way,
     * starting from the last persist snapshot.
     *
     * @return Workflow result
     */
    public String restartProcessAsync(String groupId, String operator) {
        LOGGER.info("begin to restart process asynchronously, groupId = {}, operator = {}", groupId, operator);
        groupService.updateStatus(groupId, GroupStatus.RESTARTING.getCode(), operator);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupMode mode = GroupMode.parseGroupMode(groupInfo);
        switch (mode) {
            case NORMAL:
                GroupResourceProcessForm form = genGroupProcessForm(groupInfo, GroupOperateType.RESTART);
                executorService.execute(() -> workflowService.start(ProcessName.RESTART_GROUP_PROCESS, operator, form));
                break;
            case LIGHTWEIGHT:
                LightGroupResourceProcessForm lightForm = genLightGroupProcessForm(groupInfo, GroupOperateType.RESTART);
                executorService.execute(
                        () -> workflowService.start(ProcessName.RESTART_LIGHT_GROUP_PROCESS, operator, lightForm));
                break;
            default:
                throw new WorkflowListenerException(ErrorCodeEnum.GROUP_MODE_UNSUPPORTED.getMessage());
        }
        return groupId;
    }

    /**
     * Restart resource application group which is suspended successfully,
     * starting from the last persist snapshot.
     *
     * @return Workflow result
     */
    public WorkflowResult restartProcess(String groupId, String operator) {
        LOGGER.info("begin to restart process, groupId = {}, operator = {}", groupId, operator);
        groupService.updateStatus(groupId, GroupStatus.RESTARTING.getCode(), operator);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupMode mode = GroupMode.parseGroupMode(groupInfo);
        WorkflowResult result;
        switch (mode) {
            case NORMAL:
                GroupResourceProcessForm form = genGroupProcessForm(groupInfo, GroupOperateType.RESTART);
                result = workflowService.start(ProcessName.RESTART_GROUP_PROCESS, operator, form);
                break;
            case LIGHTWEIGHT:
                LightGroupResourceProcessForm lightForm = genLightGroupProcessForm(groupInfo, GroupOperateType.RESTART);
                result = workflowService.start(ProcessName.RESTART_LIGHT_GROUP_PROCESS, operator, lightForm);
                break;
            default:
                throw new WorkflowListenerException(ErrorCodeEnum.GROUP_MODE_UNSUPPORTED.getMessage());
        }
        return result;
    }

    /**
     * Delete resource application group logically and delete related resource in an
     */
    public String deleteProcessAsync(String groupId, String operator) {
        LOGGER.info("begin to delete process asynchronously, groupId = {}, operator = {}", groupId, operator);
        executorService.execute(() -> {
            try {
                invokeDeleteProcess(groupId, operator);
            } catch (Exception ex) {
                LOGGER.error("exception while delete process, groupId = {}, operator = {}", groupId, operator, ex);
                throw ex;
            }
            groupService.delete(groupId, operator);
        });
        return groupId;
    }

    /**
     * Delete resource application group logically and delete related resource in an asynchronous way
     */
    public boolean deleteProcess(String groupId, String operator) {
        LOGGER.info("begin to delete process, groupId = {}, operator = {}", groupId, operator);
        try {
            invokeDeleteProcess(groupId, operator);
        } catch (Exception ex) {
            LOGGER.error("exception while delete process, groupId = {}, operator = {}", groupId, operator, ex);
            throw ex;
        }
        return groupService.delete(groupId, operator);
    }

    /**
     * Reset group status when group is staying CONFIG_ING|SUSPENDING|RESTARTING|DELETING for a long time
     * This api is side effect, must be used carefully.
     *
     * @param request
     * @param operator
     * @return
     */
    public boolean resetGroupStatus(InlongGroupResetRequest request, String operator) {
        LOGGER.info("begin to reset group status, request = {}, operator = {}", request, operator);
        final String groupId = request.getInlongGroupId();
        InlongGroupInfo groupInfo = groupService.get(groupId);
        Preconditions.checkNotNull(groupInfo, ErrorCodeEnum.GROUP_NOT_FOUND.getMessage());
        GroupStatus status = GroupStatus.forCode(groupInfo.getStatus());
        final int rerunProcess = request.getRerunProcess();
        final int resetFinalStatus = request.getResetFinalStatus();
        switch (status) {
            case CONFIG_ING:
            case SUSPENDING:
            case RESTARTING:
            case DELETING:
                return dealWithPendingGroup(groupInfo, operator, status, rerunProcess, resetFinalStatus);
            default:
                throw new IllegalStateException(
                        String.format("Unsupported status to reset for group = %s and status = %s",
                                request.getInlongGroupId(), status));
        }
    }

    private boolean dealWithPendingGroup(InlongGroupInfo groupInfo, String operator, GroupStatus status,
            int rerunProcess, int resetFinalStatus) {
        final String groupId = groupInfo.getInlongGroupId();
        if (rerunProcess == 1) {
            ProcessQuery processQuery = new ProcessQuery();
            processQuery.setInlongGroupId(groupId);
            List<WorkflowProcessEntity> entities = workflowQueryService.listProcessEntity(processQuery);
            Collections.sort(entities, (process1, process2) -> process1.getId() - process2.getId());
            WorkflowProcessEntity lastProcess = entities.get(entities.size() - 1);
            executorService.execute(() -> {
                workflowService.continueProcess(lastProcess.getId(), operator, "Reset group status");
            });
            return true;
        }
        if (resetFinalStatus == 1) {
            GroupStatus finalStatus = getFinalStatus(status);
            return groupService.updateStatus(groupId, finalStatus.getCode(), operator);
        } else {
            return groupService.updateStatus(groupId, GroupStatus.CONFIG_FAILED.getCode(), operator);
        }
    }

    private GroupStatus getFinalStatus(GroupStatus pendingStatus) {
        switch (pendingStatus) {
            case CONFIG_ING:
                return GroupStatus.CONFIG_SUCCESSFUL;
            case SUSPENDING:
                return GroupStatus.SUSPENDED;
            case RESTARTING:
                return GroupStatus.RESTARTED;
            default:
                return GroupStatus.DELETED;
        }
    }

    private void invokeDeleteProcess(String groupId, String operator) {
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupMode mode = GroupMode.parseGroupMode(groupInfo);
        switch (mode) {
            case NORMAL:
                GroupResourceProcessForm form = genGroupProcessForm(groupInfo, GroupOperateType.DELETE);
                workflowService.start(ProcessName.DELETE_GROUP_PROCESS, operator, form);
                break;
            case LIGHTWEIGHT:
                LightGroupResourceProcessForm lightForm = genLightGroupProcessForm(groupInfo,
                        GroupOperateType.DELETE);
                workflowService.start(ProcessName.DELETE_LIGHT_GROUP_PROCESS, operator, lightForm);
                break;
            default:
                throw new WorkflowListenerException(ErrorCodeEnum.GROUP_MODE_UNSUPPORTED.getMessage());
        }
    }

    /**
     * Generate the form of [New Group Workflow]
     */
    private NewGroupProcessForm genNewGroupProcessForm(String groupId) {
        NewGroupProcessForm form = new NewGroupProcessForm();
        InlongGroupInfo groupInfo = groupService.get(groupId);
        form.setGroupInfo(groupInfo);
        List<InlongStreamBriefInfo> infoList = streamService.getBriefList(groupInfo.getInlongGroupId());
        form.setStreamInfoList(infoList);
        return form;
    }

    private GroupResourceProcessForm genGroupProcessForm(InlongGroupInfo groupInfo, GroupOperateType operateType) {
        GroupResourceProcessForm form = new GroupResourceProcessForm();
        String groupId = groupInfo.getInlongGroupId();
        if (GroupOperateType.RESTART == operateType) {
            List<InlongStreamInfo> streamList = streamService.list(groupId);
            form.setStreamInfos(streamList);
        }
        form.setGroupInfo(groupInfo);
        form.setGroupOperateType(operateType);
        return form;
    }

    private LightGroupResourceProcessForm genLightGroupProcessForm(InlongGroupInfo groupInfo,
            GroupOperateType operateType) {
        LightGroupResourceProcessForm form = new LightGroupResourceProcessForm();
        form.setGroupInfo(groupInfo);
        String groupId = groupInfo.getInlongGroupId();
        List<InlongStreamInfo> streamList = streamService.list(groupId);
        form.setStreamInfos(streamList);
        form.setGroupOperateType(operateType);
        return form;
    }

}
