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

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.pojo.datastream.DataStreamInfo;
import org.apache.inlong.manager.common.pojo.datastream.DataStreamListVO;
import org.apache.inlong.manager.common.pojo.datastream.DataStreamPageRequest;
import org.apache.inlong.manager.common.pojo.datastream.DataStreamSummaryInfo;
import org.apache.inlong.manager.common.pojo.datastream.FullDataStreamPageRequest;
import org.apache.inlong.manager.common.pojo.datastream.FullPageInfo;
import org.apache.inlong.manager.common.pojo.datastream.FullPageUpdateInfo;
import org.apache.inlong.manager.common.util.LoginUserUtil;
import org.apache.inlong.manager.service.core.DataStreamService;
import org.apache.inlong.manager.service.core.operationlog.OperationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data stream control layer
 */
@RestController
@RequestMapping("/datastream")
@Api(tags = "Data Stream")
public class DataStreamController {

    @Autowired
    private DataStreamService dataStreamService;

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save data stream information")
    public Response<Integer> save(@RequestBody DataStreamInfo dataStreamInfo) {
        int result = dataStreamService.save(dataStreamInfo, LoginUserUtil.getLoginUserDetail().getUserName());
        return Response.success(result);
    }

    @RequestMapping(value = "/saveAll", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save data stream page information ,including source and storage")
    public Response<Boolean> saveAll(@RequestBody FullPageInfo pageInfo) {
        return Response.success(dataStreamService.saveAll(pageInfo, LoginUserUtil.getLoginUserDetail().getUserName()));
    }

    @RequestMapping(value = "/batchSaveAll", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Batch save data stream page information ,including source and storage")
    public Response<Boolean> batchSaveAll(@RequestBody List<FullPageInfo> infoList) {
        boolean result = dataStreamService.batchSaveAll(infoList, LoginUserUtil.getLoginUserDetail().getUserName());
        return Response.success(result);
    }

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ApiOperation(value = "Query data stream information")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "bid", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "dsid", dataTypeClass = String.class, required = true)
    })
    public Response<DataStreamInfo> get(@RequestParam String bid, @RequestParam String dsid) {
        return Response.success(dataStreamService.get(bid, dsid));
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ApiOperation(value = "Paging query data stream list")
    public Response<PageInfo<DataStreamListVO>> listByCondition(DataStreamPageRequest request) {
        request.setCurrentUser(LoginUserUtil.getLoginUserDetail().getUserName());
        return Response.success(dataStreamService.listByCondition(request));
    }

    @RequestMapping(value = "/listAll", method = RequestMethod.GET)
    @ApiOperation(value = "Paging query all data of the data stream page under the specified bid")
    public Response<PageInfo<FullPageInfo>> listAllWithBid(FullDataStreamPageRequest request) {
        request.setCurrentUser(LoginUserUtil.getLoginUserDetail().getUserName());
        return Response.success(dataStreamService.listAllWithBid(request));
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Modify data stream information")
    public Response<Boolean> update(@RequestBody DataStreamInfo dataStreamInfo) {
        return Response
                .success(dataStreamService.update(dataStreamInfo, LoginUserUtil.getLoginUserDetail().getUserName()));
    }

    @RequestMapping(value = "/updateAll", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Modify data stream page information,including basic data source information")
    public Response<Boolean> updateAll(@RequestBody FullPageUpdateInfo updateInfo) {
        boolean result = dataStreamService.updateAll(updateInfo, LoginUserUtil.getLoginUserDetail().getUserName());
        return Response.success(result);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    @OperationLog(operation = OperationType.DELETE)
    @ApiOperation(value = "Delete data stream information")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "bid", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "dsid", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> delete(@RequestParam String bid, @RequestParam String dsid) {
        return Response.success(dataStreamService.delete(bid, dsid, LoginUserUtil.getLoginUserDetail().getUserName()));
    }

    @RequestMapping(value = "/getSummaryList/{bid}", method = RequestMethod.GET)
    @ApiOperation(value = "Obtain the flow of data stream according to bid")
    @ApiImplicitParam(name = "bid", value = "Business identifier", dataTypeClass = String.class, required = true)
    public Response<List<DataStreamSummaryInfo>> getSummaryList(@PathVariable String bid) {
        return Response.success(dataStreamService.getSummaryList(bid));
    }

}
