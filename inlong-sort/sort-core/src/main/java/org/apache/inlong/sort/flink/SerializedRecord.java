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

package org.apache.inlong.sort.flink;

import java.io.Serializable;

/**
 * record after serialization
 */
public class SerializedRecord implements Serializable {

    private static final long serialVersionUID = 5418156417016358730L;

    private long dataFlowId;

    // Event time
    private long timestampMillis;

    private byte[] data;

    /**
     * Just satisfy requirement of Flink Pojo definition.
     */
    public SerializedRecord() {

    }

    public SerializedRecord(long dataFlowId, long timestampMillis, byte[] data) {
        this.dataFlowId = dataFlowId;
        this.timestampMillis = timestampMillis;
        this.data = data;
    }

    public void setDataFlowId(long dataFlowId) {
        this.dataFlowId = dataFlowId;
    }

    public void setTimestampMillis(long timestampMillis) {
        this.timestampMillis = timestampMillis;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getDataFlowId() {
        return dataFlowId;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public byte[] getData() {
        return data;
    }
}
