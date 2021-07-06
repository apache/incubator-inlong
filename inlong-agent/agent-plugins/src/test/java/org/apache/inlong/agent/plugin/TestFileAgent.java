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

package org.apache.inlong.agent.plugin;

import static org.apache.inlong.agent.constants.JobConstants.JOB_DIR_FILTER_PATTERN;
import static org.apache.inlong.agent.constants.JobConstants.JOB_FILE_MAX_WAIT;
import static org.awaitility.Awaitility.await;

import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.IOUtils;
import org.apache.inlong.agent.plugin.AgentBaseTestsHelper;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.core.job.JobWrapper;
import org.apache.inlong.agent.core.trigger.TriggerManager;
import org.apache.inlong.agent.db.StateSearchKey;
import org.apache.inlong.agent.plugin.utils.TestUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFileAgent {

    private static final ClassLoader LOADER = TestFileAgent.class.getClassLoader();
    private static final String RECORD = "This is the test line for huge file\n";
    private static Path testRootDir;
    private static MiniAgent agent;
    private static AgentBaseTestsHelper helper;

    @BeforeClass
    public static void setup() throws Exception {
        helper = new AgentBaseTestsHelper(
            TestFileAgent.class.getName()).setupAgentHome();
        agent = new MiniAgent();
        agent.start();
        testRootDir = helper.getTestRootDir();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (agent != null) {
            agent.stop();
        }
        helper.teardownAgentHome();
    }

    private void createHugeFiles(String fileName) throws Exception {
        final Path hugeFile = Paths.get(testRootDir.toString(), fileName);
        FileWriter writer = new FileWriter(hugeFile.toFile());
        for (int i = 0; i < 100; i++) {
            writer.write(RECORD);
        }
        writer.flush();
        writer.close();
    }

    @Test
    public void testFileAgent() throws Exception {
        for (int i = 0; i < 10; i++) {
            createHugeFiles(String.format("hugeFile.%s.txt", i));
        }
        try (InputStream stream = LOADER.getResourceAsStream("fileAgentJob.json")) {
            if (stream != null) {
                String jobJson = IOUtils.toString(stream, StandardCharsets.UTF_8);
                JobProfile profile = JobProfile.parseJsonStr(jobJson);
                profile.set(JOB_DIR_FILTER_PATTERN, Paths.get(testRootDir.toString(),
                    "hugeFile.[0-9].txt").toString());
                agent.submitJob(profile);
            }
        }
        await().atMost(5, TimeUnit.MINUTES).until(() -> {
            JobProfile jobConf = agent.getManager().getJobManager()
                    .getJobConfDb().getJob(StateSearchKey.SUCCESS);
            return jobConf != null;
        });

        JobProfile jobConf = agent.getManager().getJobManager()
                .getJobConfDb().getJob(StateSearchKey.SUCCESS);
        Assert.assertEquals(1, jobConf.getInt("job.id"));
    }

    @Test
    public void testOneJobOnly() throws Exception {
        TriggerProfile triggerProfile = TriggerProfile.parseJsonStr(TestUtils.getTestTriggerProfile());
        triggerProfile.set(JOB_DIR_FILTER_PATTERN, helper.getParentPath() + triggerProfile.get(JOB_DIR_FILTER_PATTERN));
        triggerProfile.set(JOB_DIR_FILTER_PATTERN, Paths.get(testRootDir.toString(),
            "test[0-9].dat").toString());
        triggerProfile.set(JOB_FILE_MAX_WAIT, "-1");
        TriggerManager triggerManager = agent.getManager().getTriggerManager();
        triggerManager.addTrigger(triggerProfile);
        TestUtils.createHugeFiles("test0.dat", testRootDir.toString(), RECORD);
        TestUtils.createHugeFiles("test1.dat", testRootDir.toString(), RECORD);
        await().atMost(2, TimeUnit.MINUTES).until(this::checkOnlyOneJob);
        Assert.assertTrue(checkOnlyOneJob());
    }

    private boolean checkOnlyOneJob() {
        Map<String, JobWrapper> jobs = agent.getManager().getJobManager().getJobs();
        AtomicBoolean result = new AtomicBoolean(false);
        if (jobs.size() == 1) {
            jobs.forEach(
                (s, jobWrapper) -> result.set(jobWrapper.getJob().getJobConf()
                        .get(JOB_DIR_FILTER_PATTERN).equals(testRootDir + "/test1.dat"))
            );
        }
        return result.get();
    }


}
