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

package org.apache.inlong.manager.web.auth;

import javax.annotation.Resource;
import org.apache.inlong.manager.common.auth.InlongShiro;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShiroConfig {

    @Resource
    private InlongShiro inLongShiro;

    @Bean
    public AuthorizingRealm shiroRealm(HashedCredentialsMatcher matcher) {
        AuthorizingRealm authorizingRealm = inLongShiro.getShiroRealm();
        authorizingRealm.setCredentialsMatcher(matcher);
        return authorizingRealm;
    }

    @Bean
    public WebSecurityManager securityManager(@Qualifier("hashedCredentialsMatcher")
            HashedCredentialsMatcher matcher) {
        DefaultWebSecurityManager securityManager = (DefaultWebSecurityManager) inLongShiro.getWebSecurityManager();
        securityManager.setRealm(shiroRealm(matcher));
        return securityManager;
    }

    @Bean
    public DefaultWebSessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = (DefaultWebSessionManager) inLongShiro.getWebSessionManager();
        sessionManager.setGlobalSessionTimeout(1000 * 60 * 60);
        return sessionManager;
    }

    @Bean(name = "hashedCredentialsMatcher")
    public HashedCredentialsMatcher hashedCredentialsMatcher() {
        HashedCredentialsMatcher hashedCredentialsMatcher = (HashedCredentialsMatcher) inLongShiro
                .getCredentialsMatcher();
        return hashedCredentialsMatcher;
    }

    /**
     * Filter for annon / authc
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = inLongShiro.getShiroFilter(securityManager);
        return shiroFilterFactoryBean;
    }

    /**
     * Enable permission verification annotation
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor() {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager(hashedCredentialsMatcher()));
        return advisor;
    }
}
