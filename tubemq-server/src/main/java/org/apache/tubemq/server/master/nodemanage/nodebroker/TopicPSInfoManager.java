/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tubemq.server.master.nodemanage.nodebroker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.tubemq.corebase.utils.ConcurrentHashSet;
import org.apache.tubemq.server.master.TMaster;
import org.apache.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfoHolder;

/**
 * Topic Publication/Subscription info management
 */
public class TopicPSInfoManager {

    private final TMaster master;
    private final ConcurrentHashMap<String/* topic */,
            ConcurrentHashSet<String/* producerId */>> topicPubInfoMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/* topic */,
            ConcurrentHashSet<String/* group */>> topicSubInfoMap =
            new ConcurrentHashMap<>();

    public TopicPSInfoManager(TMaster master) {
        this.master = master;
    }

    /**
     * Get groups according to topic
     *
     * @param topic
     * @return
     */
    public ConcurrentHashSet<String> getTopicSubInfo(String topic) {
        return topicSubInfoMap.get(topic);
    }

    /**
     * Set groups for a topic
     *
     * @param topic
     * @param groupSet
     */
    public void setTopicSubInfo(String topic,
                                ConcurrentHashSet<String> groupSet) {
        topicSubInfoMap.put(topic, groupSet);
    }

    /**
     * Remove a group from the group set for a specific topic
     *
     * @param topic
     * @param group
     * @return
     */
    public boolean removeTopicSubInfo(String topic,
                                      String group) {
        ConcurrentHashSet<String> groupSet = getTopicSubInfo(topic);
        if (groupSet != null) {
            return groupSet.remove(group);
        }
        return true;
    }

    /**
     * Get producer IDs for a topic
     *
     * @param topic
     * @return
     */
    public ConcurrentHashSet<String> getTopicPubInfo(String topic) {
        return topicPubInfoMap.get(topic);
    }

    /**
     * Set producer IDs for a topic
     *
     * @param topic
     * @param producerIdSet
     * @return
     */
    public ConcurrentHashSet<String> setTopicPubInfo(String topic,
                                                     ConcurrentHashSet<String> producerIdSet) {
        return topicPubInfoMap.putIfAbsent(topic, producerIdSet);
    }

    public void addProducerTopicPubInfo(final String producerId,
                                        final Set<String> topicList) {
        for (String topic : topicList) {
            ConcurrentHashSet<String> producerIdSet =
                    topicPubInfoMap.get(topic);
            if (producerIdSet == null) {
                ConcurrentHashSet<String> tmpProducerIdSet =
                        new ConcurrentHashSet<>();
                producerIdSet =
                        topicPubInfoMap.putIfAbsent(topic, tmpProducerIdSet);
                if (producerIdSet == null) {
                    producerIdSet = tmpProducerIdSet;
                }
            }
            if (!producerIdSet.contains(producerId)) {
                producerIdSet.add(producerId);
            }
        }
    }

    public void rmvProducerTopicPubInfo(final String producerId,
                                        final Set<String> topicList) {
        if (topicList != null) {
            for (String topic : topicList) {
                if (topic != null) {
                    ConcurrentHashSet<String> producerIdSet =
                            topicPubInfoMap.get(topic);
                    if (producerIdSet != null) {
                        producerIdSet.remove(producerId);
                    }
                }
            }
        }
    }

    public void clear() {
        topicPubInfoMap.clear();
        topicSubInfoMap.clear();
    }

    /**
     * Get the set of online groups subscribed to the specified topic.
     * If the specified query consumer group is empty, then the full amount of
     * online consumer groups will be taken; if the specified subscription topic
     * is empty, then all online consumer groups will be taken.
     *
     * @param qryGroupSet
     * @param subTopicSet
     * @return online groups
     */
    public Set<String> getGroupSetWithSubTopic(Set<String> qryGroupSet,
                                               Set<String> subTopicSet) {
        Set<String> resultSet = new HashSet<>();
        if (subTopicSet.isEmpty()) {
            // get all online group
            ConsumerInfoHolder consumerHolder = master.getConsumerHolder();
            List<String> onlineGroups = consumerHolder.getAllGroup();
            if (!onlineGroups.isEmpty()) {
                if (qryGroupSet.isEmpty()) {
                    resultSet.addAll(onlineGroups);
                } else {
                    for (String group : qryGroupSet) {
                        if (onlineGroups.contains(group)) {
                            resultSet.add(group);
                        }
                    }
                }
            }
        } else {
            // filter subscribed online group
            Set<String> tmpGroupSet;
            if (qryGroupSet.isEmpty()) {
                for (String topic : subTopicSet) {
                    tmpGroupSet = topicSubInfoMap.get(topic);
                    if (tmpGroupSet != null && !tmpGroupSet.isEmpty()) {
                        resultSet.addAll(tmpGroupSet);
                    }
                }
            } else {
                for (String topic : subTopicSet) {
                    tmpGroupSet = topicSubInfoMap.get(topic);
                    if (tmpGroupSet == null || tmpGroupSet.isEmpty()) {
                        continue;
                    }
                    for (String group : qryGroupSet) {
                        if (tmpGroupSet.contains(group)) {
                            resultSet.add(group);
                        }
                    }
                    qryGroupSet.removeAll(resultSet);
                    if (qryGroupSet.isEmpty()) {
                        break;
                    }
                }
            }
        }
        return resultSet;
    }
}
