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

package org.apache.inlong.agent.db;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.conf.TriggerProfile;

/**
 * key value entity. key is string and value is a json
 */
@Entity(version = 1)
public class KeyValueEntity {

    @PrimaryKey
    private String key;

    @SecondaryKey(relate = Relationship.MANY_TO_ONE)
    private StateSearchKey stateSearchKey;

    /**
     * stores the file name that the jsonValue refers
     */
    @SecondaryKey(relate = Relationship.MANY_TO_ONE)
    private String fileName;

    private String jsonValue;

    private KeyValueEntity() {

    }

    public KeyValueEntity(String key, String jsonValue, String fileName) {
        this.key = key;
        this.jsonValue = jsonValue;
        this.stateSearchKey = StateSearchKey.ACCEPTED;
        this.fileName = fileName;
    }

    public String getKey() {
        return key;
    }

    public StateSearchKey getStateSearchKey() {
        return stateSearchKey;
    }

    public KeyValueEntity setStateSearchKey(StateSearchKey stateSearchKey) {
        this.stateSearchKey = stateSearchKey;
        return this;
    }

    public String getJsonValue() {
        return jsonValue;
    }

    public KeyValueEntity setJsonValue(String jsonValue) {
        this.jsonValue = jsonValue;
        return this;
    }

    /**
     * convert keyValue to job profile
     * @return JobConfiguration
     */
    public JobProfile getAsJobProfile() {
        // convert jsonValue to jobConfiguration
        return JobProfile.parseJsonStr(getJsonValue());
    }

    /**
     * convert keyValue to trigger profile
     * @return
     */
    public TriggerProfile getAsTriggerProfile() {
        return TriggerProfile.parseJsonStr(getJsonValue());
    }

    /**
     * check whether the entity is finished
     * @return
     */
    public boolean checkFinished() {
        return stateSearchKey.equals(StateSearchKey.SUCCESS)
                || stateSearchKey.equals(StateSearchKey.FAILED);
    }
}
