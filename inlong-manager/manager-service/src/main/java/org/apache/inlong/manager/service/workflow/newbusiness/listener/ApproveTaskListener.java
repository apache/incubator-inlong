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

package org.apache.inlong.manager.service.workflow.newbusiness.listener;

import org.apache.inlong.manager.common.pojo.business.BusinessApproveInfo;
import org.apache.inlong.manager.common.pojo.datastream.DataStreamApproveInfo;
import org.apache.inlong.manager.service.core.BusinessService;
import org.apache.inlong.manager.service.core.DataStreamService;
import org.apache.inlong.manager.service.workflow.newbusiness.NewBusinessApproveForm;
import org.apache.inlong.manager.workflow.core.event.ListenerResult;
import org.apache.inlong.manager.workflow.core.event.task.TaskEvent;
import org.apache.inlong.manager.workflow.core.event.task.TaskEventListener;
import org.apache.inlong.manager.workflow.exception.WorkflowListenerException;
import org.apache.inlong.manager.workflow.model.WorkflowContext;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * New Service Access-System Administrator Approval Task Event Listener
 *
 */
@Slf4j
@Component
public class ApproveTaskListener implements TaskEventListener {

    @Autowired
    private BusinessService businessService;
    @Autowired
    private DataStreamService dataStreamService;

    @Override
    public TaskEvent event() {
        return TaskEvent.APPROVE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        // Save the data format selected at the time of approval and the cluster information of the data stream
        NewBusinessApproveForm approveForm = (NewBusinessApproveForm) context.getActionContext().getForm();

        // Save the business information after approval
        BusinessApproveInfo approveInfo = approveForm.getBusinessApproveInfo();
        businessService.updateAfterApprove(approveInfo, context.getApplicant());

        // Save data stream information after approval
        List<DataStreamApproveInfo> streamApproveInfoList = approveForm.getStreamApproveInfoList();
        dataStreamService.updateAfterApprove(streamApproveInfoList, context.getApplicant());
        return ListenerResult.success();
    }

    @Override
    public boolean async() {
        return false;
    }
}
