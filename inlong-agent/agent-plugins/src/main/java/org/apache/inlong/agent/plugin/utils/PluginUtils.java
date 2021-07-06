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

package org.apache.inlong.agent.plugin.utils;

import static org.apache.inlong.agent.constants.CommonConstants.AGENT_COLON;
import static org.apache.inlong.agent.constants.CommonConstants.AGENT_NIX_OS;
import static org.apache.inlong.agent.constants.CommonConstants.AGENT_NUX_OS;
import static org.apache.inlong.agent.constants.CommonConstants.AGENT_OS_NAME;
import static org.apache.inlong.agent.constants.CommonConstants.DEFAULT_FILE_MAX_NUM;
import static org.apache.inlong.agent.constants.CommonConstants.FILE_MAX_NUM;
import static org.apache.inlong.agent.constants.JobConstants.JOB_DIR_FILTER_PATTERN;
import static org.apache.inlong.agent.constants.JobConstants.JOB_RETRY_TIME;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constants.JobConstants;
import org.apache.inlong.agent.plugin.trigger.PathPattern;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.pulsar.client.api.CompressionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils for plugin package.
 */
@Slf4j
public class PluginUtils {


    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtils.class);


    /**
     * convert string compress type to enum compress type
     * @param type
     * @return
     */
    public static CompressionType convertType(String type) {
        switch (type) {
            case "lz4": return CompressionType.LZ4;
            case "zlib": return CompressionType.ZLIB;
            case "zstd": return CompressionType.ZSTD;
            case "snappy": return CompressionType.SNAPPY;
            case "none":
            default:
                return CompressionType.NONE;
        }
    }

    public static String toJsonStr(Object obj) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();
        return gson.toJson(obj);
    }


    public static Collection<File> findSuitFiles(JobProfile jobConf) {
        String dirPattern = jobConf.get(JOB_DIR_FILTER_PATTERN);
        LOGGER.info("find files with dir pattern {}", dirPattern);
        PathPattern pattern = new PathPattern(dirPattern);
        updateRetryTime(jobConf, pattern);
        int maxFileNum = jobConf.getInt(FILE_MAX_NUM, DEFAULT_FILE_MAX_NUM);
        LOGGER.info("dir pattern {}, max file num {}", dirPattern, maxFileNum);
        Collection<File> allFiles = new ArrayList<>();
        try {
            pattern.walkAllSuitableFiles(allFiles, maxFileNum);
        } catch (IOException ex) {
            LOGGER.warn("cannot get all files from {}", dirPattern, ex);
        }
        return allFiles;
    }

    /**
     * if the job is retry job, the date is determined
     * @param jobConf
     * @param pattern
     */
    public static void updateRetryTime(JobProfile jobConf, PathPattern pattern) {
        if (jobConf.hasKey(JOB_RETRY_TIME)) {
            LOGGER.info("job {} is retry job with specific time, update file time to {}"
                + "", jobConf.toJsonStr(), jobConf.get(JOB_RETRY_TIME));
            pattern.updateDateFormatRegex(jobConf.get(JOB_RETRY_TIME));
        }
    }

    public static JobProfile copyJobProfile(TriggerProfile triggerProfile, String dataTime,
        File pendingFile) {
        JobProfile copiedProfile = TriggerProfile.parseJsonStr(triggerProfile.toJsonStr());
        String md5 = AgentUtils.getFileMd5(pendingFile);
        copiedProfile.set(pendingFile.getAbsolutePath() + ".md5", md5);
        copiedProfile.set(JobConstants.JOB_DIR_FILTER_PATTERN, pendingFile.getAbsolutePath());
        // the time suit for file name is just the data time
        copiedProfile.set(JobConstants.JOB_DATA_TIME, dataTime);
        return copiedProfile;
    }

    public static List<String> getLocalIpList() {
        List<String> allIps = new ArrayList<>();
        try {
            String os = System.getProperty(AGENT_OS_NAME).toLowerCase();
            if (os.contains(AGENT_NIX_OS) || os.contains(AGENT_NUX_OS)) {
                /* Deal with linux platform. */
                Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                while (nis.hasMoreElements()) {
                    NetworkInterface ni = nis.nextElement();
                    addIp(allIps, ni);
                }
            } else {
                /* Deal with windows platform. */
                allIps.add(InetAddress.getLocalHost().getHostAddress());
            }
        } catch (Exception e) {
            LOGGER.error("get local ip list fail with ex {} ", e);
        }
        return allIps;
    }


    private static void addIp(List<String> allIps, NetworkInterface ni) {
        Enumeration<InetAddress> ias = ni.getInetAddresses();
        while (ias.hasMoreElements()) {
            InetAddress ia = ias.nextElement();
            if (!ia.isLoopbackAddress() && ia.getHostAddress().contains(AGENT_COLON)) {
                allIps.add(ia.getHostAddress());
            }
        }
    }

}
