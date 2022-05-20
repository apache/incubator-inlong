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

package org.apache.inlong.manager.client.api.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.common.auth.DefaultAuthentication;
import org.apache.inlong.manager.client.api.inner.InnerInlongManagerClient;
import org.apache.inlong.manager.common.pojo.stream.FullStreamResponse;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test class for get stream info list.
 */
@Slf4j
public class InnerInlongManagerClientTest {

    @Test(expected = RuntimeException.class)
    public void testListStreamInfo() {
        String serviceUrl = "127.0.0.1:8083";
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setAuthentication(new DefaultAuthentication("admin", "inlong"));
        InlongClientImpl inlongClient = new InlongClientImpl(serviceUrl, configuration);
        InnerInlongManagerClient innerInlongManagerClient = new InnerInlongManagerClient(
                inlongClient.getConfiguration());
        List<FullStreamResponse> fullStreamResponseList = innerInlongManagerClient.listStreamInfo("test");
        Assert.assertNull(fullStreamResponseList);
    }
}
