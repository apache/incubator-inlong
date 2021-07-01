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

package org.apache.inlong.manager.common.pojo.datastream;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.inlong.manager.common.pojo.datasource.SourceDbBasicInfo;
import org.apache.inlong.manager.common.pojo.datasource.SourceFileBasicInfo;

/**
 * Information of the data stream page,
 * <p/>including the basic information of the data flow and data source
 */
@Data
@ApiModel("Information of the data stream page")
public class FullPageUpdateInfo {

    @ApiModelProperty("Data stream info")
    private DataStreamInfo streamInfo;

    @ApiModelProperty("Basic file info")
    private SourceFileBasicInfo fileBasicInfo;

    @ApiModelProperty("Basic db info")
    private SourceDbBasicInfo dbBasicInfo;

}
