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

package org.apache.inlong.manager.workflow.core.impl;

import com.google.common.collect.Maps;

import org.apache.inlong.manager.workflow.core.ProcessDefinitionStorage;
import org.apache.inlong.manager.workflow.exception.WorkflowException;
import org.apache.inlong.manager.workflow.model.definition.Process;
import org.apache.inlong.manager.common.util.Preconditions;

import java.util.Map;

/**
 * Memory process memory
 */
public class MemoryProcessDefinitionStorage implements ProcessDefinitionStorage {

    private static final Map<String, Process> PROCESS_BY_NAME_STORAGE = Maps.newConcurrentMap();

    @Override
    public Process get(String name) {
        if (!PROCESS_BY_NAME_STORAGE.containsKey(name)) {
            return null;
        }
        try {
            return PROCESS_BY_NAME_STORAGE.get(name).clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new WorkflowException();
        }
    }

    @Override
    public void add(Process process) {
        Preconditions.checkNotEmpty(process.getName(), "process name cannot be null");
        if (PROCESS_BY_NAME_STORAGE.containsKey(process.getName())) {
            throw new WorkflowException("process already exist with the same name " + process.getName());
        }

        PROCESS_BY_NAME_STORAGE.put(process.getName(), process);
    }

    @Override
    public void delete(String name) {
        if (!PROCESS_BY_NAME_STORAGE.containsKey(name)) {
            throw new WorkflowException("process definition not found for name " + name);
        }

        PROCESS_BY_NAME_STORAGE.remove(name);
    }
}
