/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.singletenant.flink.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.common.TimestampFormat;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.TableSchema.Builder;
import org.apache.flink.table.data.util.DataFormatConverters;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.inlong.sort.configuration.Constants;
import org.apache.inlong.sort.formats.base.TableFormatUtils;
import org.apache.inlong.sort.formats.common.DateFormatInfo;
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.formats.common.MapFormatInfo;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.common.TimeFormatInfo;
import org.apache.inlong.sort.formats.common.TimestampFormatInfo;
import org.apache.inlong.sort.formats.common.TypeInfo;
import org.apache.inlong.sort.protocol.BuiltInFieldInfo;
import org.apache.inlong.sort.protocol.DataFlowInfo;
import org.apache.inlong.sort.protocol.FieldInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.apache.inlong.sort.protocol.deserialization.DeserializationInfo;
import org.apache.inlong.sort.protocol.deserialization.InLongMsgDeserializationInfo;

import static org.apache.flink.formats.avro.typeutils.AvroSchemaConverter.convertToSchema;
import static org.apache.flink.table.types.utils.LogicalTypeDataTypeConverter.toDataType;
import static org.apache.inlong.sort.formats.base.TableFormatUtils.deriveLogicalType;
import static org.apache.inlong.sort.formats.common.Constants.DATE_AND_TIME_STANDARD_ISO_8601;
import static org.apache.inlong.sort.formats.common.Constants.DATE_AND_TIME_STANDARD_SQL;

public class CommonUtils {

    /**
     * Get table schema.
     */
    public static TableSchema getTableSchema(FieldInfo[] fieldInfos) {
        TableSchema.Builder builder = new Builder();

        for (FieldInfo fieldInfo : fieldInfos) {
            builder.field(
                    fieldInfo.getName(),
                    TableFormatUtils.getType(fieldInfo.getFormatInfo().getTypeInfo()));
        }

        return builder.build();
    }

    /**
     * Convert information of field to Row type.
     */
    public static org.apache.flink.api.java.typeutils.RowTypeInfo convertFieldInfosToRowTypeInfo(
            FieldInfo[] fieldInfos
    ) {
        int length = fieldInfos.length;
        TypeInformation<?>[] typeInformationArray = new TypeInformation[length];
        String[] fieldNames = new String[length];
        for (int i = 0; i < length; i++) {
            FieldInfo fieldInfo = fieldInfos[i];
            fieldNames[i] = fieldInfo.getName();

            TypeInfo typeInfo = fieldInfo.getFormatInfo().getTypeInfo();
            typeInformationArray[i] = TableFormatUtils.getType(typeInfo);
        }

        return new org.apache.flink.api.java.typeutils.RowTypeInfo(typeInformationArray, fieldNames);
    }

    /**
     * Convert information of field to logical type.
     */
    public static LogicalType convertFieldInfosToLogicalType(FieldInfo[] fieldInfos) {
        int fieldLength = fieldInfos.length;
        String[] fieldNames = new String[fieldLength];
        FormatInfo[] fieldFormatInfos = new FormatInfo[fieldLength];
        for (int i = 0; i < fieldLength; i++) {
            fieldNames[i] = fieldInfos[i].getName();
            fieldFormatInfos[i] = fieldInfos[i].getFormatInfo();
        }

        RowFormatInfo rowFormatInfo = new RowFormatInfo(fieldNames, fieldFormatInfos);
        return deriveLogicalType(rowFormatInfo);
    }

    /**
     * Create Avro schema in json.
     */
    public static String buildAvroRecordSchemaInJson(FieldInfo[] fieldInfos) {
        LogicalType logicalType = convertFieldInfosToLogicalType(fieldInfos);
        Schema schema = convertToSchema(logicalType);

        if (schema.isUnion()) {
            return schema.getTypes().get(1).toString();
        }
        return schema.toString();
    }

    /**
     * Convert information of field to Data type.
     */
    public static DataType convertFieldInfosToDataType(FieldInfo[] fieldInfos) {
        LogicalType logicalType = convertFieldInfosToLogicalType(fieldInfos);
        return toDataType(logicalType);
    }

    /**
     * Create converter.
     */
    public static DataFormatConverters.RowConverter createRowConverter(FieldInfo[] fieldInfos) {
        DataType[] fieldDataTypes = getTableSchema(fieldInfos).getFieldDataTypes();
        return new DataFormatConverters.RowConverter(fieldDataTypes);
    }

    /**
     * Convert information of field to Row type.
     */
    public static RowType convertFieldInfosToRowType(FieldInfo[] fieldInfos) {
        int fieldLength = fieldInfos.length;
        String[] fieldNames = new String[fieldLength];
        LogicalType[] fieldLogicalTypes = new LogicalType[fieldLength];
        for (int i = 0; i < fieldLength; i++) {
            fieldNames[i] = fieldInfos[i].getName();
            fieldLogicalTypes[i] = TableFormatUtils.deriveLogicalType(fieldInfos[i].getFormatInfo());
        }

        return RowType.of(fieldLogicalTypes, fieldNames);
    }

    /**
     * Get standard timestamp.
     */
    public static TimestampFormat getTimestampFormatStandard(String input) {
        if (DATE_AND_TIME_STANDARD_SQL.equalsIgnoreCase(input)) {
            return TimestampFormat.SQL;
        } else if (DATE_AND_TIME_STANDARD_ISO_8601.equalsIgnoreCase(input)) {
            return TimestampFormat.ISO_8601;
        }

        throw new IllegalArgumentException("Unsupported timestamp format standard: " + input);
    }

    /**
     * Deep copy.
     */
    public static Object deepCopy(Serializable input) throws IOException, ClassNotFoundException {
        byte[] bytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(input);
            bytes = baos.toByteArray();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }

    // TODO: support map and array
    public static FieldInfo[] convertDateToStringFormatInfo(FieldInfo[] inputInfos)
            throws IOException, ClassNotFoundException {
        FieldInfo[] copiedInfos = (FieldInfo[]) deepCopy(inputInfos);
        for (FieldInfo copiedInfo : copiedInfos) {
            FormatInfo formatInfo = copiedInfo.getFormatInfo();
            if (formatInfo instanceof DateFormatInfo
                    || formatInfo instanceof TimeFormatInfo
                    || formatInfo instanceof TimestampFormatInfo) {
                if (!isStandardTimestampFormat(formatInfo)) {
                    copiedInfo.setFormatInfo(StringFormatInfo.INSTANCE);
                }
            }
        }

        return copiedInfos;
    }

    /**
     * Judge standard timestamp.
     */
    public static boolean isStandardTimestampFormat(FormatInfo formatInfo) {
        if (formatInfo instanceof DateFormatInfo) {
            String format = ((DateFormatInfo) formatInfo).getFormat();
            return DATE_AND_TIME_STANDARD_SQL.equals(format) || DATE_AND_TIME_STANDARD_ISO_8601.equals(format);
        } else if (formatInfo instanceof TimeFormatInfo) {
            String format = ((TimeFormatInfo) formatInfo).getFormat();
            return DATE_AND_TIME_STANDARD_SQL.equals(format) || DATE_AND_TIME_STANDARD_ISO_8601.equals(format);
        } else if (formatInfo instanceof TimestampFormatInfo) {
            String format = ((TimestampFormatInfo) formatInfo).getFormat();
            return DATE_AND_TIME_STANDARD_SQL.equals(format) || DATE_AND_TIME_STANDARD_ISO_8601.equals(format);
        }

        return false;
    }

    /**
     * Extract formatting information.
     */
    public static FormatInfo[] extractFormatInfos(FieldInfo[] fieldInfos) {
        int length = fieldInfos.length;
        FormatInfo[] output = new FormatInfo[length];
        for (int i = 0; i < length; i++) {
            output[i] = fieldInfos[i].getFormatInfo();
        }

        return output;
    }

    /**
     * Extract information of field.
     */
    public static FieldInfo[] extractNonBuiltInFieldInfos(FieldInfo[] fieldInfos, boolean includeData) {
        return Arrays.stream(fieldInfos)
                .filter(fieldInfo -> !(fieldInfo instanceof BuiltInFieldInfo)
                        || (includeData && ((BuiltInFieldInfo) fieldInfo).getBuiltInField()
                        == BuiltInFieldInfo.BuiltInField.MYSQL_METADATA_DATA))
                .toArray(FieldInfo[]::new);
    }

    /**
     * Check information of field.
     */
    public static boolean checkWhetherMigrateAll(FieldInfo[] fieldInfos) {
        for (FieldInfo fieldInfo : fieldInfos) {
            if (fieldInfo instanceof BuiltInFieldInfo && ((BuiltInFieldInfo) fieldInfo).getBuiltInField()
                    == BuiltInFieldInfo.BuiltInField.MYSQL_METADATA_DATA) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get field information of producer.
     */
    public static FieldInfo[] getProducedFieldInfos(FieldInfo[] physicalFieldInfos) {
        List<FieldInfo> results = new ArrayList<>();
        results.add(new FieldInfo(
                "metadata",
                new MapFormatInfo(StringFormatInfo.INSTANCE, StringFormatInfo.INSTANCE)));
        results.addAll(Arrays.asList(physicalFieldInfos));
        return results.toArray(new FieldInfo[0]);
    }

    /**
     * Get ID of group and stream.
     */
    public static Pair<String, String> getInLongGroupIdAndStreamId(DataFlowInfo dataFlowInfo) {
        String groupId = "";
        String streamId = "";

        if (dataFlowInfo != null) {
            // Get group id
            Map<String, Object> properties = dataFlowInfo.getProperties();
            if (properties != null) {
                groupId = properties.getOrDefault(Constants.INLONG_GROUP_ID, "").toString();
            }

            // Get stream id
            final DeserializationInfo deserializationInfo = dataFlowInfo.getSourceInfo().getDeserializationInfo();
            if (deserializationInfo instanceof InLongMsgDeserializationInfo) {
                streamId = ((InLongMsgDeserializationInfo) deserializationInfo).getTid();
            }

        }

        return Pair.of(groupId, streamId);
    }

}
