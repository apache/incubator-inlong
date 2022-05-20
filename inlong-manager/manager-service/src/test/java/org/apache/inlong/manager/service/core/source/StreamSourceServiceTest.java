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

package org.apache.inlong.manager.service.core.source;

import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.pojo.source.SourceResponse;
import org.apache.inlong.manager.common.pojo.source.binlog.BinlogSourceRequest;
import org.apache.inlong.manager.common.pojo.source.binlog.BinlogSourceResponse;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Stream source service test
 */
public class StreamSourceServiceTest extends ServiceBaseTest {

    private final String sourceName = "stream_source_service_test";

    @Autowired
    private StreamSourceService sourceService;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;

    /**
     * Save source info.
     */
    public Integer saveSource() {
        streamServiceTest.saveInlongStream(GLOBAL_GROUP_ID, GLOBAL_STREAM_ID, GLOBAL_OPERATOR);

        BinlogSourceRequest sourceInfo = new BinlogSourceRequest();
        sourceInfo.setInlongGroupId(GLOBAL_GROUP_ID);
        sourceInfo.setInlongStreamId(GLOBAL_STREAM_ID);
        sourceInfo.setSourceName(sourceName);
        sourceInfo.setSourceType(SourceType.BINLOG.getType());
        return sourceService.save(sourceInfo, GLOBAL_OPERATOR);
    }

    @Test
    public void testSaveAndDelete() {
        Integer id = this.saveSource();
        Assert.assertNotNull(id);

        boolean result = sourceService.delete(id, GLOBAL_OPERATOR);
        Assert.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer id = this.saveSource();

        SourceResponse source = sourceService.get(id);
        Assert.assertEquals(GLOBAL_GROUP_ID, source.getInlongGroupId());

        sourceService.delete(id, GLOBAL_OPERATOR);
    }

    @Test
    public void testGetAndUpdate() {
        Integer id = this.saveSource();
        SourceResponse response = sourceService.get(id);
        Assert.assertEquals(GLOBAL_GROUP_ID, response.getInlongGroupId());

        BinlogSourceResponse binlogResponse = (BinlogSourceResponse) response;

        BinlogSourceRequest request = CommonBeanUtils.copyProperties(binlogResponse, BinlogSourceRequest::new);
        boolean result = sourceService.update(request, GLOBAL_OPERATOR);
        Assert.assertTrue(result);

        sourceService.delete(id, GLOBAL_OPERATOR);
    }

}
