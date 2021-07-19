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

package org.apache.inlong.sort.flink.clickhouse;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.inlong.sort.configuration.Configuration;
import org.apache.inlong.sort.configuration.Constants;
import org.apache.inlong.sort.flink.SerializedRecord;
import org.apache.inlong.sort.flink.transformation.RecordTransformer;
import org.apache.inlong.sort.meta.MetaManager;
import org.apache.inlong.sort.meta.MetaManager.DataFlowInfoListener;
import org.apache.inlong.sort.protocol.DataFlowInfo;
import org.apache.inlong.sort.protocol.sink.ClickHouseSinkInfo;
import org.apache.inlong.sort.protocol.sink.SinkInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClickHouseMultiSinkFunction extends ProcessFunction<SerializedRecord, Void>
        implements CheckpointedFunction {

    private static final Logger LOG = LoggerFactory.getLogger(ClickHouseMultiSinkFunction.class);

    private final Configuration configuration;

    private transient Object lock;

    private transient Map<Long, ClickHouseSinkFunction> clickHouseSinkFunctionMap;

    private transient RecordTransformer recordTransformer;

    public ClickHouseMultiSinkFunction(Configuration configuration) {
        this.configuration = Preconditions.checkNotNull(configuration);
    }

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {
        lock = new Object();
        clickHouseSinkFunctionMap = new HashMap<>();
        recordTransformer = new RecordTransformer(
                configuration.getInteger(Constants.ETL_RECORD_SERIALIZATION_BUFFER_SIZE));
        MetaManager metaManager = MetaManager.getInstance(configuration);
        metaManager.registerDataFlowInfoListener(new DataFlowInfoListenerImpl());
    }

    @Override
    public void close() throws Exception {
        MetaManager.release();

        synchronized (lock) {
            if (clickHouseSinkFunctionMap != null) {
                for (ClickHouseSinkFunction clickHouseSinkFunction : clickHouseSinkFunctionMap.values()) {
                    clickHouseSinkFunction.close();
                }
                clickHouseSinkFunctionMap.clear();
                clickHouseSinkFunctionMap = null;
            }
        }
    }

    @Override
    public void snapshotState(FunctionSnapshotContext functionSnapshotContext) throws Exception {
        synchronized (lock) {
            for (ClickHouseSinkFunction clickHouseSinkFunction : clickHouseSinkFunctionMap.values()) {
                clickHouseSinkFunction.snapshotState(functionSnapshotContext);
            }
        }
    }

    @Override
    public void initializeState(FunctionInitializationContext functionInitializationContext) {
    }

    @Override
    public void processElement(SerializedRecord serializedRecord, Context context, Collector<Void> collector)
            throws Exception {
        final long dataFlowId = serializedRecord.getDataFlowId();

        synchronized (lock) {
            ClickHouseSinkFunction clickHouseSinkFunction = clickHouseSinkFunctionMap.get(dataFlowId);
            if (clickHouseSinkFunction == null) {
                LOG.warn("Cannot get DataFlowInfo with id {}", dataFlowId);
                return;
            }

            clickHouseSinkFunction.invoke(new Tuple2<>(false, recordTransformer.toRecord(serializedRecord).getRow()));
        }
    }

    private class DataFlowInfoListenerImpl implements DataFlowInfoListener {

        @Override
        public void addDataFlow(DataFlowInfo dataFlowInfo) throws Exception {
            synchronized (lock) {
                addOrUpdateClickHouseSinkFunction(dataFlowInfo);
                recordTransformer.addDataFlow(dataFlowInfo);
            }
        }

        @Override
        public void updateDataFlow(DataFlowInfo dataFlowInfo) throws Exception {
            synchronized (lock) {
                addOrUpdateClickHouseSinkFunction(dataFlowInfo);
                recordTransformer.updateDataFlow(dataFlowInfo);
            }
        }

        @Override
        public void removeDataFlow(DataFlowInfo dataFlowInfo) throws Exception {
            synchronized (lock) {
                // Close the old one
                ClickHouseSinkFunction oldClickHouseSinkFunction
                        = clickHouseSinkFunctionMap.remove(dataFlowInfo.getId());
                if (oldClickHouseSinkFunction != null) {
                    oldClickHouseSinkFunction.close();
                }

                recordTransformer.removeDataFlow(dataFlowInfo);
            }
        }

        private void addOrUpdateClickHouseSinkFunction(DataFlowInfo dataFlowInfo) throws Exception {
            long dataFlowId = dataFlowInfo.getId();
            SinkInfo sinkInfo = dataFlowInfo.getSinkInfo();
            if (!(sinkInfo instanceof ClickHouseSinkInfo)) {
                LOG.error("SinkInfo type {} of dataFlow {} doesn't match application sink type 'clickhouse'!",
                        sinkInfo.getClass(), dataFlowId);
                return;
            }
            ClickHouseSinkInfo clickHouseSinkInfo = (ClickHouseSinkInfo) sinkInfo;
            ClickHouseSinkFunction clickHouseSinkFunction = new ClickHouseSinkFunction(clickHouseSinkInfo);
            clickHouseSinkFunction.setRuntimeContext(getRuntimeContext());
            clickHouseSinkFunction.open(new org.apache.flink.configuration.Configuration());

            // Close the old one if exist
            ClickHouseSinkFunction oldClickHouseSinkFunction
                    = clickHouseSinkFunctionMap.put(dataFlowId, clickHouseSinkFunction);
            if (oldClickHouseSinkFunction != null) {
                oldClickHouseSinkFunction.close();
            }
        }
    }
}
