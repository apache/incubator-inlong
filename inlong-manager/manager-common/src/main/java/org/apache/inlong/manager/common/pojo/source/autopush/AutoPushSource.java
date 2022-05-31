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

package org.apache.inlong.manager.common.pojo.source.autopush;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.pojo.source.SourceRequest;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.util.CommonBeanUtils;

/**
 * Auto push info by DataProxy SDK
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Auto push source info")
public class AutoPushSource extends StreamSource {

    @ApiModelProperty(value = "DataProxy group name, used when the user enables local configuration")
    private String dataProxyGroup;

    public AutoPushSource() {
        this.setSourceType(SourceType.AUTO_PUSH.name());
    }

    @Override
    public SourceRequest genSourceRequest() {
        return CommonBeanUtils.copyProperties(this, AutoPushSourceRequest::new);
    }

}
