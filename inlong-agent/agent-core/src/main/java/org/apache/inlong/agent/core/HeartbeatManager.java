/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.core;

import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.apache.inlong.agent.common.AbstractDaemon;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.core.job.JobManager;
import org.apache.inlong.agent.core.job.JobWrapper;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.HttpManager;
import org.apache.inlong.common.pojo.agent.TaskSnapshotMessage;
import org.apache.inlong.common.pojo.agent.TaskSnapshotRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_REPORTSNAPSHOT_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_HOST;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_PORT;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_PREFIX_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_MANAGER_REPORTSNAPSHOT_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_MANAGER_VIP_HTTP_PREFIX_PATH;
import static org.apache.inlong.agent.core.task.TaskPositionManager.DEFAULT_FLUSH_TIMEOUT;

public class HeartbeatManager  extends AbstractDaemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatManager.class);

    private final AgentManager agentManager;
    private final JobManager jobmanager;
    private final AgentConfiguration conf;
    private final HttpManager httpManager;
    private final String baseManagerUrl;
    private final String reportSnapshotUrl;

    /**
     * Init heartbeat manager.
     */
    public HeartbeatManager(AgentManager agentManager) {
        this.agentManager = agentManager;
        jobmanager = agentManager.getJobManager();
        conf = AgentConfiguration.getAgentConf();
        httpManager = new HttpManager(conf);
        baseManagerUrl = buildBaseUrl();
        reportSnapshotUrl = builReportSnapShotUrl(baseManagerUrl);
    }

    /**
     * fetch heartbeat of job
     * @return
     */
    private TaskSnapshotRequest getHeartBeat() {
        Map<String, JobWrapper> jobWrapperMap = jobmanager.getJobs();

        List<TaskSnapshotMessage> taskSnapshotMessageList = new ArrayList<>();
        TaskSnapshotRequest taskSnapshotRequest = new TaskSnapshotRequest();

        Date date = new Date(System.currentTimeMillis());

        for (Map.Entry<String, JobWrapper> entry:jobWrapperMap.entrySet()) {
            if (StringUtils.isBlank(entry.getKey()) || entry.getValue() == null) {
                LOGGER.info(" key : {}, value : {} exits null",entry.getKey(),entry.getValue());
                continue;
            }
            String offset = entry.getValue().getSnapshot();
            String jobId = entry.getKey();
            TaskSnapshotMessage snapshotMessage = new TaskSnapshotMessage();
            snapshotMessage.setSnapshot(offset);
            snapshotMessage.setJobId(Integer.valueOf(jobId));
            taskSnapshotMessageList.add(snapshotMessage);
        }
        taskSnapshotRequest.setSnapshotList(taskSnapshotMessageList);
        taskSnapshotRequest.setReportTime(date);
        taskSnapshotRequest.setAgentIp(AgentUtils.fetchLocalIp());
        taskSnapshotRequest.setUuid(AgentUtils.fetchLocalUuid());
        return taskSnapshotRequest;
    }

    /**
     * build base url for manager according to config
     *
     * @example - http://127.0.0.1:8080/api/inlong/manager/openapi
     */
    private String buildBaseUrl() {
        return "http://" + conf.get(AGENT_MANAGER_VIP_HTTP_HOST)
                + ":" + conf.get(AGENT_MANAGER_VIP_HTTP_PORT)
                + conf.get(AGENT_MANAGER_VIP_HTTP_PREFIX_PATH, DEFAULT_AGENT_MANAGER_VIP_HTTP_PREFIX_PATH);
    }

    private String builReportSnapShotUrl(String baseUrl) {
        return baseUrl
                + conf.get(AGENT_MANAGER_REPORTSNAPSHOT_HTTP_PATH, DEFAULT_AGENT_MANAGER_REPORTSNAPSHOT_HTTP_PATH);
    }

    @Override
    public void start() throws Exception {
        submitWorker(heartBeatReportThread());
    }

    private Runnable heartBeatReportThread() {
        return () -> {
            while (isRunnable()) {
                try {
                    TaskSnapshotRequest taskSnapshotRequest = getHeartBeat();
                    httpManager.doSentPost(reportSnapshotUrl,taskSnapshotRequest);
                    LOGGER.info(" {} report to manager",taskSnapshotRequest);
                    TimeUnit.SECONDS.sleep(DEFAULT_FLUSH_TIMEOUT);
                } catch (Exception ex) {
                    LOGGER.error("error caught", ex);
                }
            }
        };
    }

    @Override
    public void stop() throws Exception {
        waitForTerminate();
    }

}