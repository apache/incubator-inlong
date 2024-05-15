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

package org.apache.inlong.sort.standalone.source.sortsdk.v2;

import org.apache.inlong.sdk.commons.admin.AdminServiceRegister;
import org.apache.inlong.sdk.sort.api.QueryConsumeConfig;
import org.apache.inlong.sdk.sort.api.SortClient;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.api.SortClientFactory;
import org.apache.inlong.sort.standalone.admin.ConsumerServiceMBean;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.apache.inlong.sort.standalone.config.holder.ManagerUrlHandler;
import org.apache.inlong.sort.standalone.config.holder.SortClusterConfigType;
import org.apache.inlong.sort.standalone.config.holder.SortSourceConfigType;
import org.apache.inlong.sort.standalone.config.holder.v2.SortConfigHolder;
import org.apache.inlong.sort.standalone.config.loader.ClassResourceQueryConsumeConfig;
import org.apache.inlong.sort.standalone.source.sortsdk.DefaultTopicChangeListener;
import org.apache.inlong.sort.standalone.source.sortsdk.FetchCallback;
import org.apache.inlong.sort.standalone.source.sortsdk.SortSdkSourceContext;
import org.apache.inlong.sort.standalone.utils.v2.FlumeConfigGenerator;

import org.apache.commons.lang3.ClassUtils;
import org.apache.flume.Context;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default Source implementation of InLong.
 *
 * <p>
 * SortSdkSource acquired msg from different upstream data store by register {@link SortClient} for each sort task. The
 * only things SortSdkSource should do is to get one client by the sort task id, or remove one client when the task is
 * finished or schedule to other source instance.
 * </p>
 *
 * <p>
 * The Default Manager of InLong will schedule the partition and topic automatically.
 * </p>
 *
 * <p>
 * Because all sources should implement {@link Configurable}, the SortSdkSource should have default constructor
 * <b>WITHOUT</b> any arguments, and parameters will be configured by {@link Configurable#configure(Context)}.
 * </p>
 */
public final class SortSdkSource extends AbstractSource
        implements
            Configurable,
            Runnable,
            EventDrivenSource,
            ConsumerServiceMBean {

    private static final Logger LOG = LoggerFactory.getLogger(SortSdkSource.class);
    public static final String SORT_SDK_PREFIX = "sortsdk.";
    private static final int CORE_POOL_SIZE = 1;
    private static final SortClientConfig.ConsumeStrategy defaultStrategy = SortClientConfig.ConsumeStrategy.lastest;
    private static final String KEY_SORT_SDK_CLIENT_NUM = "sortSdkClientNum";
    private static final int DEFAULT_SORT_SDK_CLIENT_NUM = 1;
    private String taskName;
    private SortSdkSourceContext context;
    private String sortClusterName;
    private long reloadInterval;
    private ScheduledExecutorService pool;

    private List<SortClient> sortClients = new ArrayList<>();

    @Override
    public synchronized void start() {
        int sortSdkClientNum = CommonPropertiesHolder.getInteger(KEY_SORT_SDK_CLIENT_NUM, DEFAULT_SORT_SDK_CLIENT_NUM);
        LOG.info("start SortSdkSource:{}, client num is {}", taskName, sortSdkClientNum);
        for (int i = 0; i < sortSdkClientNum; i++) {
            SortClient client = this.newClient(taskName);
            if (client != null) {
                this.sortClients.add(client);
            }
        }
    }

    @Override
    public void stop() {
        pool.shutdownNow();
        LOG.info("close sort client {}.", taskName);
        for (SortClient sortClient : sortClients) {
            sortClient.getConfig().setStopConsume(true);
            sortClient.close();
        }
    }

    @Override
    public void run() {
        LOG.info("start to reload SortSdkSource:{}", taskName);
        for (SortClient sortClient : sortClients) {
            sortClient.getConfig().setManagerApiUrl(ManagerUrlHandler.getSortSourceConfigUrl());
        }
    }

    @Override
    public void configure(Context context) {
        this.taskName = context.getString(FlumeConfigGenerator.KEY_TASK_NAME);
        this.context = new SortSdkSourceContext(getName(), context);
        this.sortClusterName = SortConfigHolder.getSortConfig().getSortClusterName();
        this.reloadInterval = this.context.getReloadInterval();
        this.initReloadExecutor();
        // register
        AdminServiceRegister.register(ConsumerServiceMBean.MBEAN_TYPE, taskName, this);
    }

    private void initReloadExecutor() {
        this.pool = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
        pool.scheduleAtFixedRate(this, reloadInterval, reloadInterval, TimeUnit.SECONDS);
    }

    private SortClient newClient(final String sortTaskName) {
        LOG.info("start a new sort client for task: {}", sortTaskName);
        try {
            final SortClientConfig clientConfig = new SortClientConfig(sortTaskName, this.sortClusterName,
                    new DefaultTopicChangeListener(),
                    SortSdkSource.defaultStrategy, InetAddress.getLocalHost().getHostAddress());
            final FetchCallback callback = FetchCallback.Factory.create(sortTaskName, getChannelProcessor(), context);
            clientConfig.setCallback(callback);
            Map<String, String> sortSdkParams = this.getSortClientConfigParameters();
            clientConfig.setParameters(sortSdkParams);

            // create SortClient
            String configType = CommonPropertiesHolder
                    .getString(SortSourceConfigType.KEY_TYPE, SortSourceConfigType.MANAGER.name());
            SortClient client = null;
            if (SortClusterConfigType.FILE.name().equalsIgnoreCase(configType)) {
                LOG.info("create sort sdk client in file way:{}", configType);
                ClassResourceQueryConsumeConfig queryConfig = new ClassResourceQueryConsumeConfig();
                client = SortClientFactory.createSortClient(clientConfig, queryConfig);
            } else if (SortClusterConfigType.MANAGER.name().equalsIgnoreCase(configType)) {
                LOG.info("create sort sdk client in manager way:{}", configType);
                clientConfig.setManagerApiUrl(ManagerUrlHandler.getSortSourceConfigUrl());
                client = SortClientFactory.createSortClient(clientConfig);
            } else {
                LOG.info("create sort sdk client in custom way:{}", configType);
                Class<?> loaderClass = ClassUtils.getClass(configType);
                Object loaderObject = loaderClass.getDeclaredConstructor().newInstance();
                if (loaderObject instanceof Configurable) {
                    ((Configurable) loaderObject).configure(new Context(CommonPropertiesHolder.get()));
                }
                if (!(loaderObject instanceof QueryConsumeConfig)) {
                    LOG.error("got exception when create QueryConsumeConfig instance, config key:{},config class:{}",
                            SortSourceConfigType.KEY_TYPE, configType);
                    return null;
                }
                // if it specifies the type of QueryConsumeConfig.
                client = SortClientFactory.createSortClient(clientConfig, (QueryConsumeConfig) loaderObject);
            }

            client.init();
            callback.setClient(client);
            return client;
        } catch (Throwable th) {
            LOG.error("got one throwable when init client of id:{}", sortTaskName, th);
        }
        return null;
    }

    private Map<String, String> getSortClientConfigParameters() {
        Map<String, String> commonParams = CommonPropertiesHolder.getContext().getSubProperties(SORT_SDK_PREFIX);
        return new HashMap<>(commonParams);
    }

    @Override
    public void stopConsumer() {
        for (SortClient sortClient : sortClients) {
            sortClient.getConfig().setStopConsume(true);
        }
    }

    @Override
    public void recoverConsumer() {
        for (SortClient sortClient : sortClients) {
            sortClient.getConfig().setStopConsume(false);
        }
    }
}
