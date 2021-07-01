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

package org.apache.inlong.manager.service.core;

import org.apache.inlong.manager.common.beans.PageResult;
import org.apache.inlong.manager.workflow.core.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.core.event.task.TaskEvent;
import org.apache.inlong.manager.workflow.model.view.EventLogQuery;
import org.apache.inlong.manager.workflow.model.view.EventLogView;

/**
 * Workflow event related services
 *
 */
public interface WorkflowEventService {

    /**
     * Get event log based on ID
     *
     * @param id
     * @return
     */
    EventLogView get(Integer id);

    /**
     * Query event logs based on conditions
     *
     * @param query Query conditions
     * @return Log list
     */
    PageResult<EventLogView> list(EventLogQuery query);

    /**
     * Execute the listener based on the log ID
     *
     * @param eventLogId Log record ID
     */
    void executeEventListener(Integer eventLogId);

    /**
     * Re-execute the specified listener according to the process ID
     *
     * @param processInstId Process ID
     * @param listenerName  Listener name
     */
    void executeProcessEventListener(Integer processInstId, String listenerName);

    /**
     * Re-execute the specified listener based on the task ID
     *
     * @param taskInstId   Task ID
     * @param listenerName Listener name
     */
    void executeTaskEventListener(Integer taskInstId, String listenerName);

    /**
     * Re-trigger the process event based on the process ID
     *
     * @param processInstId Process ID
     * @param processEvent  Process event
     */
    void triggerProcessEvent(Integer processInstId, ProcessEvent processEvent);

    /**
     * Re-trigger task events based on task ID
     *
     * @param taskInstId Task ID
     * @param taskEvent  Task event
     */
    void triggerTaskEvent(Integer taskInstId, TaskEvent taskEvent);
}
