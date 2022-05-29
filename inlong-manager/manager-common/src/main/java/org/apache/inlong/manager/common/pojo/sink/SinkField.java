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

package org.apache.inlong.manager.common.pojo.sink;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.inlong.manager.common.pojo.stream.StreamField;

/**
 * Sink field info.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("Sink field configuration")
public class SinkField extends StreamField {

    @ApiModelProperty("Source field name")
    private String sourceFieldName;

    @ApiModelProperty("Source field type")
    private String sourceFieldType;

    public SinkField() {

    }

    public SinkField(int index, String fieldType, String fieldName, String sourceFieldType, String sourceFieldName) {
        this(index, fieldType, fieldName, null, null, sourceFieldName, sourceFieldType, 0, null);
    }

    public SinkField(int index, String fieldType, String fieldName, String fieldComment,
            String fieldValue, String sourceFieldName, String sourceFieldType,
            Integer isMetaField, String fieldFormat) {
        super(index, fieldType, fieldName, fieldComment, fieldValue, isMetaField, fieldFormat);
        this.sourceFieldName = sourceFieldName;
        this.sourceFieldType = sourceFieldType;
    }
}
