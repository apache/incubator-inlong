/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.broker.web;

import static org.apache.inlong.tubemq.server.common.webbase.WebMethodMapper.getRegisteredWebMethod;
import static org.apache.inlong.tubemq.server.common.webbase.WebMethodMapper.getWebApiRegInfo;
import static org.apache.inlong.tubemq.server.common.webbase.WebMethodMapper.registerWebMethod;
import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.inlong.tubemq.server.broker.TubeBroker;
import org.apache.inlong.tubemq.server.common.webbase.WebMethodMapper.WebApiRegInfo;



public abstract class AbstractWebHandler extends HttpServlet {

    protected final TubeBroker broker;

    public AbstractWebHandler(TubeBroker broker) {
        this.broker = broker;
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

    public Set<String> getSupportedMethod() {
        return getRegisteredWebMethod();
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp) throws IOException {
        StringBuilder strBuffer = new StringBuilder(1024);

        try {
            String method = req.getParameter("method");
            if (method == null) {
                strBuffer.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                        .append("Please take with method parameter! \"}");
            } else {
                WebApiRegInfo webApiRegInfo = getWebApiRegInfo(method);
                if (webApiRegInfo == null) {
                    strBuffer.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                            .append("Unsupported method ").append(method).append("\"}");
                } else {
                    webApiRegInfo.method.invoke(webApiRegInfo.webHandler, req, strBuffer);
                }
            }
        } catch (Throwable e) {
            strBuffer.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append("Bad request from server: ")
                    .append(e.getMessage())
                    .append("\"}");
        }
        resp.getWriter().write(strBuffer.toString());
        resp.setCharacterEncoding(req.getCharacterEncoding());
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.flushBuffer();
    }

    public abstract void registerWebApiMethod();

    protected void innRegisterWebMethod(String webMethodName,
                                        String clsMethodName,
                                        boolean needAuthToken) {
        registerWebMethod(webMethodName, clsMethodName,
                false, needAuthToken, this);
    }

}
