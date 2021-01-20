/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tubemq.manager.controller.group;



import static org.apache.tubemq.manager.service.TubeMQHttpConst.ADD;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.CLONE;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.DELETE;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.QUERY;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.REBALANCE_CONSUMER_GROUP;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.REBALANCE_CONSUMER;

import com.google.gson.Gson;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.tubemq.manager.controller.TubeMQResult;
import org.apache.tubemq.manager.controller.group.request.AddBlackGroupReq;
import org.apache.tubemq.manager.controller.group.request.DeleteBlackGroupReq;
import org.apache.tubemq.manager.controller.group.request.DeleteOffsetReq;
import org.apache.tubemq.manager.controller.group.request.QueryOffsetReq;
import org.apache.tubemq.manager.controller.node.request.CloneOffsetReq;
import org.apache.tubemq.manager.controller.topic.request.BatchAddGroupAuthReq;
import org.apache.tubemq.manager.controller.topic.request.DeleteGroupReq;
import org.apache.tubemq.manager.controller.topic.request.RebalanceConsumerReq;
import org.apache.tubemq.manager.controller.topic.request.RebalanceGroupReq;
import org.apache.tubemq.manager.service.interfaces.MasterService;
import org.apache.tubemq.manager.service.TopicServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/group")
@Slf4j
public class GroupController {


    private Gson gson = new Gson();

    @Autowired
    private MasterService masterService;

    @Autowired
    private TopicServiceImpl topicService;


    @PostMapping("")
    public @ResponseBody TubeMQResult groupMethodProxy(
        @RequestParam String method, @RequestBody String req) throws Exception {
        switch (method) {
            case ADD:
                return masterService.baseRequestMaster(gson.fromJson(req, BatchAddGroupAuthReq.class));
            case DELETE:
                return masterService.baseRequestMaster(gson.fromJson(req, DeleteGroupReq.class));
            case REBALANCE_CONSUMER_GROUP:
                return topicService.rebalanceGroup(gson.fromJson(req, RebalanceGroupReq.class));
            case REBALANCE_CONSUMER:
                return masterService.baseRequestMaster(gson.fromJson(req, RebalanceConsumerReq.class));
            default:
                return TubeMQResult.getErrorResult("no such method");
        }
    }

    /**
     * query the consumer group for certain topic
     * @param req
     * @return
     * @throws Exception
     */
    @GetMapping("/")
    public @ResponseBody String queryConsumer(
        @RequestParam Map<String, String> req) throws Exception {
        String url = masterService.getQueryUrl(req);
        return masterService.queryMaster(url);
    }


    @PostMapping("/offset")
    public @ResponseBody TubeMQResult offsetProxy(
        @RequestParam String method, @RequestBody String req) {
        switch (method) {
            case CLONE:
                return topicService.cloneOffsetToOtherGroups(gson.fromJson(req, CloneOffsetReq.class));
            case DELETE:
                return topicService.deleteOffset(gson.fromJson(req, DeleteOffsetReq.class));
            case QUERY:
                return topicService.queryOffset(gson.fromJson(req, QueryOffsetReq.class));
            default:
                return TubeMQResult.getErrorResult("no such method");
        }
    }


    @PostMapping("/blackGroup")
    public @ResponseBody TubeMQResult blackGroupProxy(
        @RequestParam String method, @RequestBody String req) {
        switch (method) {
            case ADD:
                return masterService.baseRequestMaster(gson.fromJson(req, AddBlackGroupReq.class));
            case DELETE:
                return masterService.baseRequestMaster(gson.fromJson(req, DeleteBlackGroupReq.class));
            default:
                return TubeMQResult.getErrorResult("no such method");
        }
    }


    /**
     * query the black list for certain topic
     * @param req
     * @return
     * @throws Exception
     */
    @GetMapping("/blackGroup")
    public @ResponseBody String queryBlackGroup(
        @RequestParam Map<String, String> req) throws Exception {
        String url = masterService.getQueryUrl(req);
        return masterService.queryMaster(url);
    }




}
