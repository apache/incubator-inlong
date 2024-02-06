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

package org.apache.inlong.manager.service.audit;

import org.apache.inlong.manager.common.consts.AuditSourceType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.pojo.util.MySQLSensitiveUrlUtils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Audit source mysql operator.
 */
@Service
public class AuditSourceMysqlOperator extends AbstractAuditSourceOperator {

    private static final String MYSQL_JDBC_PREFIX = "jdbc:mysql://";

    @Override
    public String getType() {
        return AuditSourceType.MYSQL.name();
    }

    @Override
    public String convertTo(String url) {
        if (StringUtils.isNotBlank(url) && url.startsWith(MYSQL_JDBC_PREFIX)) {
            return MySQLSensitiveUrlUtils.filterSensitive(url);
        }

        throw new BusinessException(String.format(ErrorCodeEnum.AUDIT_SOURCE_URL_NOT_SUPPORTED.getMessage(), url));
    }
}
