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

package org.apache.inlong.manager.workflow.core.event.task;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.inlong.manager.workflow.core.WorkflowDataAccessor;
import org.apache.inlong.manager.workflow.core.event.EventListenerManager;
import org.apache.inlong.manager.workflow.dao.EventLogStorage;
import org.apache.inlong.manager.workflow.exception.WorkflowListenerException;

import java.util.List;
import java.util.Map;

/**
 * Internal default task listener management
 */
public class TaskEventListenerManager implements EventListenerManager<TaskEvent, TaskEventListener> {

    private Map<TaskEvent, List<TaskEventListener>> syncTaskEventListeners = Maps.newHashMap();
    private Map<TaskEvent, List<TaskEventListener>> asyncTaskEventListeners = Maps.newHashMap();
    private Map<String, TaskEventListener> taskEventListeners = Maps.newHashMap();

    private WorkflowDataAccessor workflowDataAccessor;

    public TaskEventListenerManager(WorkflowDataAccessor workflowDataAccessor) {
        this.workflowDataAccessor = workflowDataAccessor;
    }

    @Override
    public void register(TaskEventListener listener) {
        if (taskEventListeners.containsKey(listener.name())) {
            throw new WorkflowListenerException("duplicate listener:" + listener.name());
        }
        taskEventListeners.put(listener.name(), listener);

        if (listener.async()) {
            this.asyncTaskEventListeners.computeIfAbsent(listener.event(), a -> Lists.newArrayList())
                    .add(enhanceListener(listener));
            return;
        }

        this.syncTaskEventListeners.computeIfAbsent(listener.event(), a -> Lists.newArrayList())
                .add(enhanceListener(listener));
    }

    private TaskEventListener enhanceListener(TaskEventListener taskEventListener) {
        EventLogStorage eventLogStorage = workflowDataAccessor.eventLogStorage();
        if (eventLogStorage == null) {
            return taskEventListener;
        }

        return new LogableTaskEventListener(taskEventListener, eventLogStorage);
    }

    @Override
    public List<TaskEventListener> asyncListeners(TaskEvent event) {
        return asyncTaskEventListeners.getOrDefault(event, TaskEventListener.EMPTY_LIST);
    }

    @Override
    public List<TaskEventListener> syncListeners(TaskEvent event) {
        return syncTaskEventListeners.getOrDefault(event, TaskEventListener.EMPTY_LIST);
    }

    @Override
    public TaskEventListener listener(String listenerName) {
        return taskEventListeners.get(listenerName);
    }

    @Override
    public EventLogStorage eventLogStorage() {
        return workflowDataAccessor.eventLogStorage();
    }

}
