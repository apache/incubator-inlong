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

package org.apache.inlong.manager.common.pojo.sink.iceberg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.common.enums.SinkType;
import org.apache.inlong.manager.common.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonTypeDefine;

/**
 * Iceberg sink info
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Iceberg sink info")
@JsonTypeDefine(value = SinkType.SINK_ICEBERG)
public class IcebergSink extends StreamSink {

    @ApiModelProperty("Hive JDBC URL")
    private String jdbcUrl;

    @ApiModelProperty("Username for JDBC URL")
    private String username;

    @ApiModelProperty("User password")
    private String password;

    @ApiModelProperty("Target database name")
    private String dbName;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty("Data path, such as: hdfs://ip:port/user/hive/warehouse/test.db")
    private String dataPath;

    @ApiModelProperty("File format, support: Parquet, Orc, Avro")
    private String fileFormat;

    @ApiModelProperty("Data encoding type")
    private String dataEncoding;

    @ApiModelProperty("Data field separator")
    private String dataSeparator;

    @ApiModelProperty("Data consistency strategy, support: EXACTLY_ONCE(default), AT_LEAST_ONCE")
    private String dataConsistency;

    public IcebergSink() {
        this.setSinkType(SinkType.SINK_ICEBERG);
    }

    @Override
    public SinkRequest genSinkRequest() {
        return CommonBeanUtils.copyProperties(this, IcebergSinkRequest::new);
    }

}
