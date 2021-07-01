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

package org.apache.inlong.manager.common.pojo.datasource;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import lombok.Data;

/**
 * File data source details
 */
@Data
@ApiModel("File data source details")
public class SourceFileDetailInfo {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Business iIdentifier")
    private String businessIdentifier;

    @ApiModelProperty(value = "Data stream identifier")
    private String dataStreamIdentifier;

    @ApiModelProperty(value = "Name of the data source service")
    private String serverName;

    @ApiModelProperty(value = "Data source IP address")
    private String ip;

    @ApiModelProperty(value = "Data source port number")
    private Integer port;

    @ApiModelProperty(value = "Delivery method, with SSH, TCS")
    private String issueType;

    @ApiModelProperty(value = "Collection type, Agent, DataProxy Client, LoadProxy, file can only be Agent")
    private String accessType = "Agent";

    @ApiModelProperty(value = "User name of the data source IP host")
    private String username;

    @ApiModelProperty(value = "Login password of the data source host")
    private String password;

    @ApiModelProperty(value = "File path, supports regular matching")
    private String filePath;

    @ApiModelProperty(value = "Data source status")
    private Integer status;

    @ApiModelProperty(value = "Previous state")
    private Integer previousStatus;

    @ApiModelProperty(value = "is deleted? 0: deleted, 1: not deleted")
    private Integer isDeleted = 0;

    private String creator;

    private String modifier;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date modifyTime;

    @ApiModelProperty(value = "Temporary view, string in JSON format")
    private String tempView;

}