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

package org.apache.inlong.sort.cdc.debezium.internal;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.source.SourceRecord;

/**
 * Copied from Debezium project. Make it public to be accessible from DebeziumChangeFetcher.
 */
public class EmbeddedEngineChangeEvent<K, V> implements ChangeEvent<K, V>, RecordChangeEvent<V> {

    private final K key;
    private final V value;
    private final SourceRecord sourceRecord;

    public EmbeddedEngineChangeEvent(K key, V value, SourceRecord sourceRecord) {
        this.key = key;
        this.value = value;
        this.sourceRecord = sourceRecord;
    }

    @Override
    public K key() {
        return key;
    }

    @Override
    public V value() {
        return value;
    }

    @Override
    public V record() {
        return value;
    }

    @Override
    public String destination() {
        return sourceRecord.topic();
    }

    public SourceRecord sourceRecord() {
        return sourceRecord;
    }

    @Override
    public String toString() {
        return "EmbeddedEngineChangeEvent [key=" + key + ", value=" + value + ", sourceRecord=" + sourceRecord + "]";
    }
}
