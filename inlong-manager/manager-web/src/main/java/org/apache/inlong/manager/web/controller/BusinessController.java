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
import io.swagger.annotations.ApiOperation;
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.pojo.business.BusinessCountVO;
import org.apache.inlong.manager.common.pojo.business.BusinessInfo;
import org.apache.inlong.manager.common.pojo.business.BusinessListVO;
import org.apache.inlong.manager.common.pojo.business.BusinessPageRequest;
import org.apache.inlong.manager.common.pojo.business.BusinessTopicVO;
import org.apache.inlong.manager.common.util.LoginUserUtil;
import org.apache.inlong.manager.service.core.BusinessService;
import org.apache.inlong.manager.service.core.impl.BusinessProcessOperation;
import org.apache.inlong.manager.service.core.operationlog.OperationLog;
import org.apache.inlong.manager.service.workflow.WorkflowResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Business access control layer
 */
@RestController
@RequestMapping("/business")
@Api(tags = "Business Config")
public class BusinessController {

    @Autowired
    private BusinessService businessService;
    @Autowired
    private BusinessProcessOperation bizProcessOperation;

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save business information")
    public Response<String> save(@RequestBody BusinessInfo businessInfo) {
        return Response.success(businessService.save(businessInfo, LoginUserUtil.getLoginUserDetail().getUserName()));
    }

    @RequestMapping(value = "/get/{bid}", method = RequestMethod.GET)
    @ApiOperation(value = "Query business information")
    @ApiImplicitParam(name = "bid", value = "Business identifier", dataTypeClass = String.class, required = true)
    public Response<BusinessInfo> get(@PathVariable String bid) {
        return Response.success(businessService.get(bid));
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ApiOperation(value = "Query business list according to conditions")
    public Response<PageInfo<BusinessListVO>> listByCondition(BusinessPageRequest request) {
        request.setCurrentUser(LoginUserUtil.getLoginUserDetail().getUserName());
        return Response.success(businessService.listByCondition(request));
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Modify business information")
    public Response<String> update(@RequestBody BusinessInfo businessInfo) {
        return Response.success(businessService.update(businessInfo, LoginUserUtil.getLoginUserDetail().getUserName()));
    }

    @RequestMapping(value = "/delete/{bid}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete business information")
    @OperationLog(operation = OperationType.DELETE)
    @ApiImplicitParam(name = "bid", value = "Business identifier", dataTypeClass = String.class, required = true)
    public Response<Boolean> delete(@PathVariable String bid) {
        return Response.success(businessService.delete(bid, LoginUserUtil.getLoginUserDetail().getUserName()));
    }

    @RequestMapping(value = "/exist/{bid}", method = RequestMethod.GET)
    @ApiOperation(value = "Query whether the business identifier exists")
    @ApiImplicitParam(name = "bid", value = "Business identifier", dataTypeClass = String.class, required = true)
    public Response<Boolean> exist(@PathVariable String bid) {
        return Response.success(businessService.exist(bid));
    }

    @RequestMapping(value = "/countByStatus", method = RequestMethod.GET)
    @ApiOperation(value = "Statistics of current user's business status")
    public Response<BusinessCountVO> countCurrentUserBusiness() {
        return Response.success(businessService.countBusinessByUser(LoginUserUtil.getLoginUserDetail().getUserName()));
    }

    @RequestMapping(value = "startProcess/{bid}", method = RequestMethod.POST)
    @ApiOperation(value = "Start approval process")
    @ApiImplicitParam(name = "bid", value = "Business identifier", dataTypeClass = String.class)
    public Response<WorkflowResult> startProcess(@PathVariable String bid) {
        String username = LoginUserUtil.getLoginUserDetail().getUserName();
        return Response.success(bizProcessOperation.startProcess(bid, username));
    }

    @RequestMapping(value = "getTopic/{bid}", method = RequestMethod.GET)
    @ApiOperation(value = "Get Topic via the business")
    public Response<BusinessTopicVO> getTopic(@PathVariable String bid) {
        return Response.success(businessService.getTopic(bid));
    }

}