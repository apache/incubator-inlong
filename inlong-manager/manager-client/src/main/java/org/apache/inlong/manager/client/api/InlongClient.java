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

package org.apache.inlong.manager.client.api;

import org.apache.inlong.manager.client.api.impl.InlongClientImpl;

/**
 * An interface to manipulate Inlong Cluster
 * <p/>
 * Example:
 * <p/>
 *
 * <pre>
 * <code>
 * ClientConfiguration configuration = ..
 * InlongClient client = InlongClient.create(${serviceUrl}, configuration);
 * DataStreamGroupConf groupConf = ..
 * DataStreamGroup group = client.createStreamGroup(groupConf);
 * DataStreamConf streamConf = ..
 * DataStreamBuilder builder = group.createDataStream(streamConf);
 * StreamSource source = ..
 * StreamSink sink = ..
 * List<StreamField> fields = ..
 * DataStream stream = builder.source(source).sink(sink).fields(fields).init();
 * group.init();
 * </code>
 * </pre>
 */
public interface InlongClient {

    static InlongClient create(String serviceUrl, ClientConfiguration configuration) {
        return new InlongClientImpl(serviceUrl, configuration);
    }

    /**
     * Create stream group by conf
     *
     * @param groupConf
     * @return streamGroupId
     * @throws Exception
     */
    DataStreamGroup createStreamGroup(DataStreamGroupConf groupConf) throws Exception;

}
