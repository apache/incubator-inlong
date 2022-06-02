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

package org.apache.inlong.manager.common.pojo.group;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.SmallTools;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Inlong group request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Inlong group create request")
@JsonTypeInfo(use = Id.NAME, visible = true, property = "mqType")
public class InlongGroupRequest {

    @ApiModelProperty(value = "Inlong group id", required = true)
    private String inlongGroupId;

    @ApiModelProperty(value = "Inlong group name", required = true)
    private String name;

    @ApiModelProperty(value = "Inlong group description")
    private String description;

    @NotNull
    @ApiModelProperty(value = "MQ type, high throughput: TUBE, high consistency: PULSAR")
    private String mqType;

    @ApiModelProperty(value = "MQ resource, in inlong group",
            notes = "Tube corresponds to Topic, Pulsar corresponds to Namespace")
    private String mqResource;

    @ApiModelProperty(value = "Tube master URL")
    private String tubeMaster;

    @ApiModelProperty(value = "Whether to enable zookeeper? 0: disable, 1: enable")
    @Builder.Default
    private Integer enableZookeeper = 0;

    @ApiModelProperty(value = "Whether to enable zookeeper? 0: disable, 1: enable")
    @Builder.Default
    private Integer enableCreateResource = 1;

    @ApiModelProperty(value = "Whether to use lightweight mode, 0: false, 1: true")
    @Builder.Default
    private Integer lightweight = 0;

    @ApiModelProperty(value = "Inlong cluster tag, which links to inlong_cluster table")
    private String inlongClusterTag;

    @ApiModelProperty(value = "Number of access items per day, unit: 10,000 items per day")
    private Integer dailyRecords;

    @ApiModelProperty(value = "Access size per day, unit: GB per day")
    private Integer dailyStorage;

    @ApiModelProperty(value = "peak access per second, unit: bars per second")
    private Integer peakRecords;

    @ApiModelProperty(value = "The maximum length of a single piece of data, unit: Byte")
    private Integer maxLength;

    @ApiModelProperty(value = "Name of responsible person, separated by commas")
    private String inCharges;

    @ApiModelProperty(value = "Name of followers, separated by commas")
    private String followers;

    @ApiModelProperty(value = "Name of creator")
    private String creator;

    @ApiModelProperty(value = "Inlong group Extension properties")
    private List<InlongGroupExtInfo> extList;

    /**
     * Check the validation of request params
     */
    public void checkParams() {
        if (StringUtils.isBlank(inlongGroupId)) {
            throw new BusinessException("inlongGroupId cannot be null");
        }

        if (inlongGroupId.length() < 4 || inlongGroupId.length() > 200) {
            throw new BusinessException("characters for inlongGroupId must be more than 4 and less than 200");
        }

        if (!SmallTools.isLowerOrNum(inlongGroupId)) {
            throw new BusinessException("inlongGroupId must starts with a lowercase letter "
                    + "and contains only lowercase letters, digits, `-` or `_`");
        }

        if (StringUtils.isBlank(mqType)) {
            throw new BusinessException("mqType cannot be null");
        }

        if (StringUtils.isBlank(inCharges)) {
            throw new BusinessException("inCharges cannot be null");
        }
    }

}
