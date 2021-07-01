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

package org.apache.inlong.manager.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.pojo.business.DataSchemaInfo;
import org.apache.inlong.manager.common.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.common.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.service.core.ClusterInfoService;
import org.apache.inlong.manager.service.core.DataSchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Control layer for basic information query
 * <p/>Basic information, such as data_schema, cluster info, etc.
 */
@RestController
@RequestMapping("/basic")
@Api(tags = "Basic Config")
public class BasicInfoController {

    @Autowired
    private ClusterInfoService clusterInfoService;
    @Autowired
    private DataSchemaService schemaService;

    @RequestMapping(value = "/cluster/list", method = RequestMethod.GET)
    @ApiOperation(value = "Query the cluster list based on conditions")
    public Response<List<ClusterInfo>> list(ClusterRequest request) {
        return Response.success(clusterInfoService.list(request));
    }

    @ApiOperation(value = "Query data format list")
    @RequestMapping(value = "/schema/listAll", method = RequestMethod.GET)
    public Response<List<DataSchemaInfo>> dataSchemaList() {
        return Response.success(schemaService.listAllDataSchema());
    }

}
