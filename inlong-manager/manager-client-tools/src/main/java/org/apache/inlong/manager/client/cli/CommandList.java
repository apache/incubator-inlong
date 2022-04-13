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

package org.apache.inlong.manager.client.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.client.api.inner.InnerInlongManagerClient;
import org.apache.inlong.manager.common.pojo.group.InlongGroupListResponse;
import org.apache.inlong.manager.common.pojo.sink.SinkListResponse;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.pojo.stream.FullStreamResponse;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;

import java.util.List;

@Parameters(commandDescription = "Displays main information for one or more resources")
public class CommandList extends CommandBase {

    @Parameter()
    private java.util.List<String> params;

    public CommandList() {
        super("list");
        jcommander.addCommand("stream", new CommandList.ListStream());
        jcommander.addCommand("group", new CommandList.ListGroup());
        jcommander.addCommand("sink", new CommandList.ListSink());
        jcommander.addCommand("source", new CommandList.ListSource());
    }

    @Parameters(commandDescription = "Get stream main information")
    private class ListStream extends CommandUtil {

        @Parameter()
        private java.util.List<String> params;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String groupId;

        @Override
        void run() {
            InnerInlongManagerClient managerClient = new InnerInlongManagerClient(connect().getConfiguration());
            try {
                List<FullStreamResponse> fullStreamResponseList = managerClient.listStreamInfo(groupId);
                fullStreamResponseList.forEach(fullStreamResponse -> {
                    print(fullStreamResponse.getStreamInfo(), InlongStreamInfo.class);
                });
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get group details")
    private class ListGroup extends CommandUtil {

        @Parameter()
        private java.util.List<String> params;

        @Parameter(names = {"-s", "--status"})
        private int status;

        @Parameter(names = {"-g", "--group"}, description = "inlong group id")
        private String group;

        @Parameter(names = {"-n", "--num"}, description = "the number displayed")
        private int pageSize = 10;

        @Override
        void run() {
            InnerInlongManagerClient managerClient = new InnerInlongManagerClient(connect().getConfiguration());
            try {
                PageInfo<InlongGroupListResponse> groupPageInfo = managerClient.listGroups(group, status, 1, pageSize);
                print(groupPageInfo.getList(), InlongGroupListResponse.class);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get sink details")
    private class ListSink extends CommandUtil {

        @Parameter()
        private java.util.List<String> params;

        @Parameter(names = {"-s", "--stream"}, required = true, description = "stream id")
        private String stream;

        @Parameter(names = {"-g", "--group"}, required = true, description = "group id")
        private String group;

        @Override
        void run() {
            InnerInlongManagerClient managerClient = new InnerInlongManagerClient(connect().getConfiguration());
            try {
                List<SinkListResponse> sinkListResponses = managerClient.listSinks(group, stream);
                print(sinkListResponses, SinkListResponse.class);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get source details")
    private class ListSource extends CommandUtil {

        @Parameter()
        private java.util.List<String> params;

        @Parameter(names = {"-s", "--stream"}, required = true, description = "inlong stream id")
        private String stream;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String group;

        @Parameter(names = {"-t", "--type"}, description = "sink type")
        private String type;

        @Override
        void run() {
            InnerInlongManagerClient managerClient = new InnerInlongManagerClient(connect().getConfiguration());
            try {
                List<SourceListResponse> sourceListResponses = managerClient.listSources(group, stream, type);
                print(sourceListResponses, SourceListResponse.class);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
