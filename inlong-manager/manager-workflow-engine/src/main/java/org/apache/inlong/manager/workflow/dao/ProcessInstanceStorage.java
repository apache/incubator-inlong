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

package org.apache.inlong.manager.workflow.dao;

import org.apache.inlong.manager.workflow.model.instance.ProcessInstance;
import org.apache.inlong.manager.workflow.model.view.CountByKey;
import org.apache.inlong.manager.workflow.model.view.ProcessQuery;
import org.apache.inlong.manager.workflow.model.view.ProcessSummaryQuery;

import java.util.List;

/**
 * Process instance DAO
 */
public interface ProcessInstanceStorage {

    /**
     * Insert process example record
     *
     * @param processInstance Process instance object
     * @return Process application ID
     */
    Integer insert(ProcessInstance processInstance);

    void update(ProcessInstance processInstance);

    ProcessInstance get(Integer id);

    List<ProcessInstance> listByQuery(ProcessQuery query);

    List<CountByKey> countByState(ProcessSummaryQuery query);
}
