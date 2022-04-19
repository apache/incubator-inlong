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

package org.apache.inlong.sort.protocol;

import com.google.common.base.Preconditions;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;

import java.io.Serializable;
import java.util.Objects;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FieldInfo.class, name = "base"),
        @JsonSubTypes.Type(value = BuiltInFieldInfo.class, name = "builtin")
})
@Data
public class FieldInfo implements FunctionParam, Serializable {

    private static final long serialVersionUID = 5871970550803344673L;
    @JsonProperty("name")
    private final String name;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("nodeId")
    private String nodeId;
    @JsonIgnore
    private String tableNameAlias;
    @JsonProperty("formatInfo")
    private FormatInfo formatInfo;

    public FieldInfo(
            @JsonProperty("name") String name,
            @JsonProperty("formatInfo") FormatInfo formatInfo) {
        this.name = Preconditions.checkNotNull(name);
        this.formatInfo = Preconditions.checkNotNull(formatInfo);
    }

    @JsonCreator
    public FieldInfo(
            @JsonProperty("name") String name,
            @JsonProperty("nodeId") String nodeId,
            @JsonProperty("formatInfo") FormatInfo formatInfo) {
        this.name = Preconditions.checkNotNull(name);
        this.nodeId = nodeId;
        this.formatInfo = Preconditions.checkNotNull(formatInfo);
    }

    @Override
    public String format() {
        String formatName = name.trim();
        if (!formatName.startsWith("`")) {
            formatName = String.format("`%s", formatName);
        }
        if (!formatName.endsWith("`")) {
            formatName = String.format("%s`", formatName);
        }
        if (StringUtils.isNotBlank(tableNameAlias)) {
            return String.format("%s.%s", tableNameAlias, formatName);
        }
        return formatName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FieldInfo fieldInfo = (FieldInfo) o;
        return name.equals(fieldInfo.name) && formatInfo.equals(fieldInfo.formatInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, formatInfo);
    }

}
