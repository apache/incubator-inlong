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

package org.apache.inlong.manager.workflow.model.view;

import org.apache.inlong.manager.workflow.model.TaskState;
import org.apache.inlong.manager.workflow.model.instance.TaskInstance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Task list view
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Task list view")
public class TaskListView {

    /**
     * Task ID
     */
    @ApiModelProperty(value = "Approval task ID")
    private Integer id;

    /**
     * Task type
     */
    @ApiModelProperty(value = "task type")
    private String type;

    /**
     * Application form ID
     */
    @ApiModelProperty(value = "application form ID")
    private Integer processInstId;

    @ApiModelProperty(value = "process name")
    private String processName;

    @ApiModelProperty(value = "process display name")
    private String processDisplayName;

    /**
     * Task name
     */
    @ApiModelProperty(value = "task name-english key")
    private String name;

    /**
     * Chinese name of the task
     */
    @ApiModelProperty(value = "Task display name")
    private String displayName;

    @ApiModelProperty(value = "applicant")
    private String applicant;
    /**
     * Approver
     */
    @ApiModelProperty(value = "set approver")
    private List<String> approvers;

    /**
     * Task operator
     */
    @ApiModelProperty(value = "actual operation approver")
    private String operator;

    /**
     * Task status
     */
    @ApiModelProperty(value = "task status")
    private TaskState state;

    /**
     * Remarks information
     */
    @ApiModelProperty(value = "remarks information")
    private String remark;

    /**
     * Start time
     */
    @ApiModelProperty(value = "start time")
    private Date startTime;

    /**
     * End time
     */
    @ApiModelProperty(value = "end time")
    private Date endTime;

    @ApiModelProperty(value = "extra information shown in the list")
    private Map<String, Object> showInList;


    public static TaskListView fromTaskInstance(TaskInstance taskInstance) {
        return TaskListView.builder()
                .id(taskInstance.getId())
                .type(taskInstance.getType())
                .processInstId(taskInstance.getProcessInstId())
                .processName(taskInstance.getProcessName())
                .processDisplayName(taskInstance.getProcessDisplayName())
                .name(taskInstance.getName())
                .displayName(taskInstance.getDisplayName())
                .applicant(taskInstance.getApplicant())
                .approvers(Arrays.asList(taskInstance.getApprovers().split(TaskInstance.APPROVERS_DELIMITER)))
                .operator(taskInstance.getOperator())
                .state(TaskState.valueOf(taskInstance.getState()))
                .remark(taskInstance.getRemark())
                .startTime(taskInstance.getStartTime())
                .endTime(taskInstance.getEndTime())
                .build();
    }
}
