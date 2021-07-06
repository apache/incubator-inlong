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

package org.apache.inlong.agent.plugin.sinks;

import static org.apache.inlong.agent.constants.JobConstants.JOB_INSTANCE_ID;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.core.task.TaskPositionManager;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockSink implements Sink {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockSink.class);
    private final AtomicLong number = new AtomicLong(0);
    private TaskPositionManager taskPositionManager;
    private String sourceFileName;
    private String jobInstanceId;


    @Override
    public void write(Message message) {
        if (message != null) {
            number.incrementAndGet();
            taskPositionManager.updateFileSinkPosition(jobInstanceId, sourceFileName, 1);
        }
    }

    @Override
    public void setSourceFile(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    @Override
    public void init(JobProfile jobConf) {
        taskPositionManager = TaskPositionManager.getTaskPositionManager();
        jobInstanceId = jobConf.get(JOB_INSTANCE_ID);
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy mockSink, sink line number is : {}", number.get());
    }
}
