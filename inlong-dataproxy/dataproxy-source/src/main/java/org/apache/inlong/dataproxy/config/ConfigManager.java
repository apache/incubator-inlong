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

package org.apache.inlong.dataproxy.config;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.dataproxy.config.RemoteConfigJson.DataItem;
import org.apache.inlong.dataproxy.config.holder.BidPropertiesHolder;
import org.apache.inlong.dataproxy.config.holder.FileConfigHolder;
import org.apache.inlong.dataproxy.config.holder.MxPropertiesHolder;
import org.apache.inlong.dataproxy.config.holder.PropertiesConfigHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ConfigManager {

    public static final List<ConfigHolder> CONFIG_HOLDER_LIST = new ArrayList<ConfigHolder>();
    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);
    private static volatile boolean isInit = false;

    private static ConfigManager instance = null;

    private final PropertiesConfigHolder commonConfig =
            new PropertiesConfigHolder("common.properties");
    private final PropertiesConfigHolder topicConfig =
            new PropertiesConfigHolder("topics.properties");
    private final MxPropertiesHolder mxConfig = new MxPropertiesHolder("mx.properties");
    private final BidPropertiesHolder bidConfig =
            new BidPropertiesHolder("bid_mapping.properties");
    private final PropertiesConfigHolder dcConfig =
            new PropertiesConfigHolder("dc_mapping.properties");
    private final PropertiesConfigHolder transferConfig =
            new PropertiesConfigHolder("transfer.properties");
    private final PropertiesConfigHolder tubeSwitchConfig =
            new PropertiesConfigHolder("tube_switch.properties");
    private final PropertiesConfigHolder weightHolder =
            new PropertiesConfigHolder("weight.properties");
    private final FileConfigHolder blackListConfig =
            new FileConfigHolder("blacklist.properties");

    /**
     * get instance for manager
     * @return
     */
    public static ConfigManager getInstance() {

        if (isInit && instance != null) {
            return instance;
        }
        synchronized (ConfigManager.class) {
            if (!isInit) {
                instance = new ConfigManager();

                for (ConfigHolder holder : CONFIG_HOLDER_LIST) {

                    holder.loadFromFileToHolder();
                }
                ReloadConfigWorker reloadProperties = new ReloadConfigWorker(instance);
                reloadProperties.setDaemon(true);
                reloadProperties.start();
            }
            isInit = true;
        }
        return instance;
    }


    public Map<String, String> getWeightProperties() {
        return weightHolder.getHolder();
    }

    public Map<String, String> getTopicProperties() {
        return topicConfig.getHolder();
    }

    /**
     * update old maps, reload local files if changed.
     *
     * @param result        - map pending to be added
     * @param holder        - property holder
     * @param addElseRemove - if add(true) else remove(false)
     * @return true if changed else false.
     */
    private boolean updatePropertiesHolder(Map<String, String> result,
                                           PropertiesConfigHolder holder, boolean addElseRemove) {
        Map<String, String> tmpHolder = holder.forkHolder();
        boolean changed = false;
        for (Map.Entry<String, String> entry : result.entrySet()) {
            String oldValue = addElseRemove
                    ? tmpHolder.put(entry.getKey(), entry.getValue()) : tmpHolder.remove(entry.getKey());
            // if addElseRemove is false, that means removing item, changed is true.
            if (oldValue == null || !oldValue.equals(entry.getValue()) || !addElseRemove) {
                changed = true;
            }
        }

        if (changed) {
            return holder.loadFromHolderToFile(tmpHolder);
        } else {
            return false;
        }
    }

    public boolean addTopicProperties(Map<String, String> result) {
        return updatePropertiesHolder(result, topicConfig, true);
    }

    public boolean deleteTopicProperties(Map<String, String> result) {
        return updatePropertiesHolder(result, topicConfig, false);
    }

    public Map<String, String> getMxProperties() {
        return mxConfig.getHolder();
    }

    public boolean addMxProperties(Map<String, String> result) {
        return updatePropertiesHolder(result, mxConfig, true);
    }

    public boolean deleteMxProperties(Map<String, String> result) {
        return updatePropertiesHolder(result, mxConfig, false);
    }

    public Map<String, String> getDcMappingProperties() {
        return dcConfig.getHolder();
    }

    public Map<String, String> getTransferProperties() {
        return transferConfig.getHolder();
    }

    public Map<String, String> getTubeSwitchProperties() {
        return tubeSwitchConfig.getHolder();
    }

    public Map<String, Map<String, String>> getMxPropertiesMaps() {
        return mxConfig.getMxPropertiesMaps();
    }

    public Map<String, String> getBidMappingProperties() {
        return bidConfig.getBidMappingProperties();
    }

    public Map<String, Map<String, String>> getTidMappingProperties() {
        return bidConfig.getTidMappingProperties();
    }

    public Map<String, String> getBidEnableMappingProperties() {
        return bidConfig.getBidEnableMappingProperties();
    }

    public Map<String, String> getCommonProperties() {
        return commonConfig.getHolder();
    }

    public PropertiesConfigHolder getTopicConfig() {
        return topicConfig;
    }

    /**
     * load worker
     */
    private static class ReloadConfigWorker extends Thread {

        private static final Logger LOG = LoggerFactory.getLogger(ReloadConfigWorker.class);
        private final ConfigManager configManager;
        private final CloseableHttpClient httpClient;
        private final Gson gson = new Gson();
        private boolean isRunning = true;

        public ReloadConfigWorker(ConfigManager managerInstance) {
            this.configManager = managerInstance;
            this.httpClient = constructHttpClient();
        }

        private synchronized CloseableHttpClient constructHttpClient() {
            long timeoutInMs = TimeUnit.MILLISECONDS.toMillis(50000);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout((int) timeoutInMs)
                    .setSocketTimeout((int) timeoutInMs).build();
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            httpClientBuilder.setDefaultRequestConfig(requestConfig);
            return httpClientBuilder.build();
        }

        public int getRandom(int min, int max) {
            return (int) (Math.random() * (max + 1 - min)) + min;
        }

        private long getSleepTime() {
            String sleepTimeInMsStr =
                    configManager.getCommonProperties().get("configCheckInterval");
            long sleepTimeInMs = 10000;
            try {
                if (sleepTimeInMsStr != null) {
                    sleepTimeInMs = Long.parseLong(sleepTimeInMsStr);
                }
            } catch (Exception ignored) {
                LOG.info("ignored Exception ", ignored);
            }
            return sleepTimeInMs + getRandom(0, 5000);
        }

        public void close() {
            isRunning = false;
        }

        private void checkLocalFile() {


            for (ConfigHolder holder : CONFIG_HOLDER_LIST) {


                boolean isChanged = holder.checkAndUpdateHolder();
                if (isChanged) {
                    holder.executeCallbacks();
                }
            }
        }

        private boolean checkWithManager(String host) {
            HttpGet httpGet = null;
            try {
                String url = "http://" + host + "/openapi/inlong/manager/dataproxy/getConfig/"
                        + configManager.getCommonProperties().get("cluster_id");
                LOG.info("start to request {} to get config info", url);
                httpGet = new HttpGet(url);
                httpGet.addHeader(HttpHeaders.CONNECTION, "close");

                // request with post
                CloseableHttpResponse response = httpClient.execute(httpGet);
                String returnStr = EntityUtils.toString(response.getEntity());
                // get bid <-> topic and m value.

                RemoteConfigJson configJson = gson.fromJson(returnStr, RemoteConfigJson.class);
                Map<String, String> bidToTopic = new HashMap<String, String>();
                Map<String, String> bidToMValue = new HashMap<String, String>();

                if (configJson.getErrCode() == 0) {
                    for (DataItem item : configJson.getData()) {
                        bidToMValue.put(item.getBid(), item.getM());
                        bidToTopic.put(item.getBid(), item.getTopic());
                    }
                    configManager.addMxProperties(bidToMValue);
                    configManager.addTopicProperties(bidToTopic);
                }
            } catch (Exception ex) {
                LOG.error("exception caught", ex);
                return false;
            } finally {
                if (httpGet != null) {
                    httpGet.releaseConnection();
                }
            }
            return true;
        }

        private void checkRemoteConfig() {

            try {
                String managerHosts = configManager.getCommonProperties().get("manager_hosts");
                String[] hostList = StringUtils.split(managerHosts, ",");
                for (String host : hostList) {

                    if (checkWithManager(host)) {
                        break;
                    }
                }
            } catch (Exception ex) {
                LOG.error("exception caught", ex);
            }
        }

        @Override
        public void run() {
            long count = 0;
            while (isRunning) {

                long sleepTimeInMs = getSleepTime();
                count += 1;
                try {
                    checkLocalFile();
                    if (count % 30 == 0) {

                        checkRemoteConfig();
                        count = 0;
                    }
                    TimeUnit.MILLISECONDS.sleep(sleepTimeInMs);
                } catch (Exception ex) {
                    LOG.error("exception caught", ex);
                }
            }
        }
    }
}
