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

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.pojo.user.UserDetail;
import org.apache.inlong.manager.common.util.LoginUserUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

@Slf4j
@ControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public Response<String> handleConstraintViolationException(HttpServletRequest request,
            ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        StringBuilder stringBuilder = new StringBuilder(64);
        for (ConstraintViolation<?> violation : violations) {
            stringBuilder.append(violation.getMessage()).append(".");
        }
        UserDetail userDetail = LoginUserUtils.getLoginUserDetail();
        log.error("Failed to handle request on path:" + request.getRequestURI()
                + (userDetail != null ? ", user:" + userDetail.getUserName() : ""), e);
        return Response.fail(stringBuilder.toString());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Response<String> handleMethodArgumentNotValidException(HttpServletRequest request,
            MethodArgumentNotValidException e) {
        UserDetail userDetail = LoginUserUtils.getLoginUserDetail();
        log.error("Failed to handle request on path:" + request.getRequestURI()
                + (userDetail != null ? ", user:" + userDetail.getUserName() : ""), e);
        return Response.fail(e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = IllegalArgumentException.class)
    public Response<String> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException e) {
        UserDetail userDetail = LoginUserUtils.getLoginUserDetail();
        log.error("Failed to handle request on path:" + request.getRequestURI()
                + (userDetail != null ? ", user:" + userDetail.getUserName() : ""), e);
        return Response.fail(e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = BindException.class)
    public Response<String> handleBindExceptionHandler(HttpServletRequest request, BindException e) {
        UserDetail userDetail = LoginUserUtils.getLoginUserDetail();
        log.error("Failed to handle request on path:" + request.getRequestURI()
                + (userDetail != null ? ", user:" + userDetail.getUserName() : ""), e);
        return Response.fail(e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = HttpMessageConversionException.class)
    public Response<String> handleHttpMessageConversionExceptionHandler(HttpServletRequest request,
            HttpMessageConversionException e) {
        UserDetail userDetail = LoginUserUtils.getLoginUserDetail();
        log.error("Failed to handle request on path:" + request.getRequestURI()
                + (userDetail != null ? ", user:" + userDetail.getUserName() : ""), e);
        return Response.fail("http message convert exception! pls check params");
    }

    @ResponseBody
    @ExceptionHandler(value = WorkflowException.class)
    public Response<String> handleWorkflowException(HttpServletRequest request, WorkflowException e) {
        UserDetail userDetail = LoginUserUtils.getLoginUserDetail();
        log.error("Failed to handle request on path:" + request.getRequestURI()
                + (userDetail != null ? ", user:" + userDetail.getUserName() : ""), e);
        return Response.fail(e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = BusinessException.class)
    public Response<String> handleBusinessExceptionHandler(HttpServletRequest request, BusinessException e) {
        UserDetail userDetail = LoginUserUtils.getLoginUserDetail();
        log.error("Failed to handle request on path:" + request.getRequestURI()
                + (userDetail != null ? ", user:" + userDetail.getUserName() : ""), e);
        return Response.fail(e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = AuthenticationException.class)
    public Response<String> handleAuthenticationException(HttpServletRequest request, AuthenticationException e) {
        log.error("Failed to handle request on path:" + request.getRequestURI(), e);
        return Response.fail("username or password is incorrect, or the account has expired");
    }

    @ResponseBody
    @ExceptionHandler(value = UnauthorizedException.class)
    public Response<String> handleUnauthorizedException(HttpServletRequest request, AuthorizationException e) {
        log.error("Failed to handle request on path:" + request.getRequestURI(), e);
        UserDetail userDetail = LoginUserUtils.getLoginUserDetail();
        return Response.fail("Current user [" + (userDetail != null ? userDetail.getUserName() : "")
                + "] has no permission to access URL: " + request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Response<String> handle(HttpServletRequest request, Exception e) {
        UserDetail userDetail = LoginUserUtils.getLoginUserDetail();
        log.error("Failed to handle request on path:" + request.getRequestURI()
                + (userDetail != null ? ", user:" + userDetail.getUserName() : ""), e);
        return Response.fail("There was an error in the service..."
                + "Please try again later! "
                + "If there are still problems, please contact the administrator");
    }
}
