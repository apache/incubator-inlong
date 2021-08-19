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

package org.apache.inlong.manager.workflow.model;

import org.apache.inlong.manager.workflow.core.event.task.TaskEvent;
import org.apache.inlong.manager.workflow.exception.WorkflowException;

/**
 * operate
 */
public enum Action {

    /**
     * Initiation process
     */
    START("initiation process"),
    /**
     * Agree
     */
    APPROVE("agree"),

    /**
     * Rejected
     */
    REJECT("reject"),

    /**
     * Applicant withdrawal
     */
    CANCEL("withdrawal"),

    /**
     * Turn to do
     */
    TRANSFER("Turn to do"),

    /**
     * Abandoned by the administrator
     */
    TERMINATE("abandoned"),

    /**
     * Automatic completion
     */
    COMPLETE("automatic completion");

    private final String displayName;

    Action(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get Action from TaskEvent
     */
    public static Action fromTaskEvent(TaskEvent taskEvent) {
        switch (taskEvent) {
            case CREATE:
                return null;
            case APPROVE:
                return Action.APPROVE;
            case REJECT:
                return Action.REJECT;
            case TRANSFER:
                return Action.TRANSFER;
            case CANCEL:
                return Action.CANCEL;
            case COMPLETE:
            case FAIL:
                return Action.COMPLETE;
            case TERMINATE:
                return Action.TERMINATE;
            default:
                throw new WorkflowException("unknown taskEvent " + taskEvent);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

}
