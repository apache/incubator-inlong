/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.inlong.sort.protocol.node.load;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.constant.IcebergConstant.CatalogType;
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

@JsonTypeName("hudiLoad")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HudiLoadNode extends LoadNode implements InlongMetric, Serializable {

    private static final long serialVersionUID = -1L;

    @JsonProperty("tableName")
    @Nonnull
    private String tableName;

    @JsonProperty("dbName")
    @Nonnull
    private String dbName;

    @JsonProperty("primaryKey")
    private String primaryKey;

    @JsonProperty("catalogType")
    private CatalogType catalogType;

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("path")
    private String path;

    @JsonProperty("warehouse")
    private String warehouse;

    @JsonCreator
    public HudiLoadNode(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
            @Nullable @JsonProperty("sinkParallelism") Integer sinkParallelism,
            @JsonProperty("properties") Map<String, String> properties,
            @Nonnull @JsonProperty("dbName") String dbName,
            @Nonnull @JsonProperty("tableName") String tableName,
            @JsonProperty("primaryKey") String primaryKey,
            @JsonProperty("catalogType") CatalogType catalogType,
            @JsonProperty("uri") String uri,
            @JsonProperty("warehouse") String warehouse) {
        super(id, name, fields, fieldRelations, filters, filterStrategy, sinkParallelism, properties);
        this.tableName = Preconditions.checkNotNull(tableName, "table name is null");
        this.dbName = Preconditions.checkNotNull(dbName, "db name is null");
        this.primaryKey = primaryKey;
        this.catalogType = catalogType == null ? CatalogType.HIVE : catalogType;
        this.uri = uri;
        this.warehouse = warehouse;
        this.path = properties.get("path");
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();
        options.put("connector", "hudi-inlong");

        // options.put("catalog-type", catalogType.name());
        // options.put("catalog-name", catalogType.name());

        options.put("path", path);
        options.put("hoodie.database.name", dbName);
        options.put("hoodie.table.name", tableName);
        options.put("hoodie.datasource.write.recordkey.field", primaryKey);

        options.put("hive_sync.enable", "true");
        options.put("hive_sync.mode", "hms");
        options.put("hive_sync.db", dbName);
        options.put("hive_sync.table", tableName);
        options.put("hive_sync.metastore.uris", uri);

        String partitionPathField = primaryKey;
        List<FieldInfo> partitionFields = getPartitionFields();
        if (partitionFields != null && !partitionFields.isEmpty()) {
            partitionPathField = partitionFields.stream()
                    .map(FieldInfo::getName)
                    .collect(Collectors.joining(","));
        }
        options.put("hoodie.datasource.write.partitionpath.field", partitionPathField);

        return options;
    }

    @Override
    public String genTableName() {
        return tableName;
    }

    @Override
    public String getPrimaryKey() {
        return primaryKey;
    }

    @Override
    public List<FieldInfo> getPartitionFields() {
        return super.getPartitionFields();
    }

}
