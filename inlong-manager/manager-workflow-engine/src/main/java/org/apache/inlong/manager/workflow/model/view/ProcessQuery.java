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

import org.apache.inlong.manager.workflow.model.ProcessState;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * Process query conditions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Application form query conditions")
public class ProcessQuery extends PageQuery {

    @ApiModelProperty("Process from ID")
    private Integer id;

    @ApiModelProperty("process form list of ID")
    private List<Integer> ids;

    @ApiModelProperty("process name")
    private String name;

    @ApiModelProperty("process display name")
    private String displayName;

    @ApiModelProperty("applicant")
    private String applicant;

    @ApiModelProperty("status")
    private ProcessState state;

    @ApiModelProperty("business ID")
    private String businessId;

    @ApiModelProperty("start time-lower limit: yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTimeBegin;

    @ApiModelProperty("start time-upper limit:yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTimeEnd;

    @ApiModelProperty("end time-lower limit:yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTimeBegin;

    @ApiModelProperty("end time-upper limit:yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTimeEnd;

    @ApiModelProperty("whether to hide")
    private Boolean hidden = false;

    @ApiModelProperty("whether to include the current to-do task")
    private boolean includeCurrentTask = false;

    @ApiModelProperty("whether to include the form information displayed in the list")
    private boolean includeShowInList = true;
}
