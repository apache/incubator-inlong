/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React, { useState, useMemo } from 'react';
import { Tabs, Card } from 'antd';
import { parse } from 'qs';
import { PageContainer, Container } from '@/components/PageContainer';
import { DashTotalRevert, DashPending, DashRejected, DashCancelled } from '@/components/Icons';
import { DashboardCardList } from '@/components/DashboardCard';
import { useRequest, useHistory, useLocation } from '@/hooks';
import Applies, { activedName as AppliesActivedName } from './Applies';
import Approvals, { activedName as ApprovalsActivedName } from './Approvals';

const list = [
  {
    label: '我的申请',
    value: AppliesActivedName,
    content: <Applies />,
  },
  {
    label: '我的审批',
    value: ApprovalsActivedName,
    content: <Approvals />,
  },
];

const dashCardList = [
  {
    desc: '审批通过',
    dataIndex: 'totalApproveCount',
    icon: <DashTotalRevert />,
  },
  {
    desc: '已驳回',
    dataIndex: 'totalRejectCount',
    icon: <DashRejected />,
  },
  {
    desc: '待审批',
    dataIndex: 'totalProcessingCount',
    icon: <DashPending />,
  },
  {
    desc: '已取消',
    dataIndex: 'totalCancelCount',
    icon: <DashCancelled />,
  },
];

const Comp: React.FC = () => {
  const history = useHistory();
  const location = useLocation();

  const [actived, setActived] = useState(parse(location.search.slice(1))?.actived || list[0].value);

  const { data: summary = {} } = useRequest({
    url: '/workflow/processSummary',
  });

  const onTabsChange = value => {
    setActived(value);
    history.push({
      search: `?actived=${value}`,
    });
  };

  const dashboardList = useMemo(
    () =>
      dashCardList.map(item => ({
        ...item,
        title: summary[item.dataIndex] || 0,
      })),
    [summary],
  );

  return (
    <PageContainer useDefaultBreadcrumb={false} useDefaultContainer={false}>
      <Container>
        <DashboardCardList dataSource={dashboardList} />
      </Container>

      <Container>
        <Card>
          <Tabs activeKey={actived} onChange={val => onTabsChange(val)}>
            {list.map(item => (
              <Tabs.TabPane tab={item.label} key={item.value}>
                {item.content}
              </Tabs.TabPane>
            ))}
          </Tabs>
        </Card>
      </Container>
    </PageContainer>
  );
};

export default Comp;
