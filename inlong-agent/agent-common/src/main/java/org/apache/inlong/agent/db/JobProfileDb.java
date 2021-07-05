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

import java.util.ArrayList;
import java.util.List;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constants.JobConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for job conf persistence.
 */
public class JobProfileDb {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobProfileDb.class);
    private final Db db;

    public JobProfileDb(Db db) {
        this.db = db;
    }

    /**
     * get job which in accepted state
     * @return null or job conf
     */
    public JobProfile getAcceptedJob() {
        return getJob(StateSearchKey.ACCEPTED);
    }

    public List<JobProfile> getAcceptedJobs() {
        return getJobs(StateSearchKey.ACCEPTED);
    }

    /**
     * update job state and search it by key name
     * @param jobInstanceId - job key name
     * @param stateSearchKey - job state
     */
    public void updateJobState(String jobInstanceId, StateSearchKey stateSearchKey) {
        KeyValueEntity entity = db.get(jobInstanceId);
        if (entity != null) {
            entity.setStateSearchKey(stateSearchKey);
            db.put(entity);
        }
    }

    /**
     * store job profile
     * @param jobProfile - job profile
     */
    public void storeJobFirstTime(JobProfile jobProfile) {
        if (jobProfile.allRequiredKeyExist()) {
            String keyName = jobProfile.get(JobConstants.JOB_INSTANCE_ID);
            jobProfile.setLong(JobConstants.JOB_STORE_TIME, System.currentTimeMillis());
            KeyValueEntity entity = new KeyValueEntity(keyName,
                jobProfile.toJsonStr(), jobProfile.get(JobConstants.JOB_DIR_FILTER_PATTERN));
            entity.setStateSearchKey(StateSearchKey.ACCEPTED);
            db.put(entity);
        }
    }

    /**
     * update job profile
     * @param jobProfile
     */
    public void updateJobProfile(JobProfile jobProfile) {
        String instanceId = jobProfile.getInstanceId();
        KeyValueEntity entity = db.get(instanceId);
        if (entity == null) {
            LOGGER.warn("job profile {} doesn't exist, update job profile fail {}", instanceId, jobProfile.toJsonStr());
            return;
        }
        entity.setJsonValue(jobProfile.toJsonStr());
        db.put(entity);
    }

    /**
     * check whether job is finished, note that non-exist job is regarded as finished.
     * @param jobProfile
     * @return
     */
    public boolean checkJobfinished(JobProfile jobProfile) {
        KeyValueEntity entity = db.get(jobProfile.getInstanceId());
        if (entity == null) {
            LOGGER.info("job profile {} doesn't exist", jobProfile.getInstanceId());
            return true;
        }
        return entity.checkFinished();
    }

    public void deleteJob(String keyName) {
        db.remove(keyName);
    }

    public JobProfile getJobProfile(String jobId) {
        KeyValueEntity keyValueEntity = db.get(jobId);
        if (keyValueEntity != null) {
            return keyValueEntity.getAsJobProfile();
        }
        return null;
    }

    public void removeExpireJobs(long expireTime) {
        // remove finished tasks
        List<KeyValueEntity> successEntityList = db.search(StateSearchKey.SUCCESS);
        List<KeyValueEntity> failedEntityList = db.search(StateSearchKey.FAILED);
        List<KeyValueEntity> entityList = new ArrayList<>(successEntityList);
        entityList.addAll(failedEntityList);
        for (KeyValueEntity entity : entityList) {
            if (entity.getKey().startsWith(JobConstants.JOB_ID_PREFIX)) {
                JobProfile profile = entity.getAsJobProfile();
                long storeTime = profile.getLong(JobConstants.JOB_STORE_TIME, 0);
                long currentTime = System.currentTimeMillis();
                if (storeTime == 0 || currentTime - storeTime > expireTime) {
                    LOGGER.info("delete job {} because of timeout store time: {}, expire time: {}",
                        entity.getKey(), storeTime, expireTime);
                    deleteJob(entity.getKey());
                }
            }
        }
    }

    /**
     * get job conf by state
     * @param stateSearchKey - state index for searching.
     * @return
     */
    public JobProfile getJob(StateSearchKey stateSearchKey) {
        KeyValueEntity entity = db.searchOne(stateSearchKey);
        if (entity != null && entity.getKey().startsWith(JobConstants.JOB_ID_PREFIX)) {
            return entity.getAsJobProfile();
        }
        return null;
    }


    /**
     * get job reading specific file
     * @param fileName
     * @return
     */
    public JobProfile getJob(String fileName) {
        KeyValueEntity entity = db.searchOne(fileName);
        if (entity != null && entity.getKey().startsWith(JobConstants.JOB_ID_PREFIX)) {
            return entity.getAsJobProfile();
        }
        return null;
    }

    /**
     * get list of job profiles.
     * @param stateSearchKey - state search key.
     * @return - list of job profile.
     */
    public List<JobProfile> getJobs(StateSearchKey stateSearchKey) {
        List<KeyValueEntity> entityList = db.search(stateSearchKey);
        List<JobProfile> profileList = new ArrayList<>();
        for (KeyValueEntity entity : entityList) {
            if (entity.getKey().startsWith(JobConstants.JOB_ID_PREFIX)) {
                profileList.add(entity.getAsJobProfile());
            }
        }
        return profileList;
    }
}
