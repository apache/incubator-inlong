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

package org.apache.inlong.manager.web.config;

import org.apache.inlong.manager.web.SpringBaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RestTemplateConfigTest extends SpringBaseTest {

    @Autowired
    private RestTemplateConfig restTemplateConfig;

    private final int maxTotal                 = 5001;
    private final int defaultMaxPerRoute       = 2001;
    private final int validateAfterInactivity  = 5001;
    private final int connectionTimeout        = 3001;
    private final int readTimeout              = 10001;
    private final int connectionRequestTimeout = 3001;

    @Test
    public void configValue() {
        Assert.assertEquals(maxTotal, restTemplateConfig.getMaxTotal());
        Assert.assertEquals(defaultMaxPerRoute, restTemplateConfig.getDefaultMaxPerRoute());
        Assert.assertEquals(validateAfterInactivity, restTemplateConfig.getValidateAfterInactivity());
        Assert.assertEquals(connectionTimeout, restTemplateConfig.getConnectionTimeout());
        Assert.assertEquals(readTimeout, restTemplateConfig.getReadTimeout());
        Assert.assertEquals(connectionRequestTimeout, restTemplateConfig.getConnectionRequestTimeout());
    }

}