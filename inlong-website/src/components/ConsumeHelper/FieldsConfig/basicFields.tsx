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

import React from 'react';
import { FormItemProps } from '@/components/FormGenerator';
import { pickObjectArray } from '@/utils';
import StaffSelect from '@/components/StaffSelect';
import BussinessSelect from '../BussinessSelect';

export default (
  names: string[],
  bussinessDetail: Record<'middlewareType', string> = { middlewareType: '' },
  currentValues: Record<string, any> = {},
): FormItemProps[] => {
  const fields: FormItemProps[] = [
    {
      type: 'input',
      label: '消费组名称',
      name: 'consumerGroupName',
      initialValue: currentValues.consumerGroupName,
      extra: '仅限小写字⺟、数字和下划线',
      rules: [
        { required: true },
        { pattern: /^[a-z_\d]+$/, message: '仅限小写字⺟、数字和下划线' },
      ],
      props: {
        placeholder: '请输入',
      },
    },
    {
      type: <StaffSelect mode="multiple" currentUserClosable={false} />,
      label: '消费责任人',
      name: 'inCharges',
      initialValue: currentValues.inCharges,
      extra: '至少2人，责任人可查看、修改消费信息',
      rules: [{ required: true, type: 'array', min: 2, message: '请填写至少2个责任人' }],
    },
    {
      type: BussinessSelect,
      label: '消费目标业务ID',
      name: 'businessIdentifier',
      initialValue: currentValues.businessIdentifier,
      rules: [{ required: true }],
      props: {
        style: { width: 500 },
        onChange: () => ({
          topic: '',
        }),
      },
    },
    {
      type: 'select',
      label: 'Topic',
      name: 'topic',
      initialValue: currentValues.topic,
      rules: [{ required: true }],
      props: {
        options: {
          requestService: `/business/getTopic/${currentValues.businessIdentifier}`,
          requestParams: {
            formatResult: result => [
              {
                label: result.topicName,
                value: result.topicName,
              },
            ],
          },
        },
      },
      visible: values => !!values.businessIdentifier,
    },
    {
      type: 'radio',
      label: '是否过滤消费',
      name: 'filterEnabled',
      initialValue: currentValues.filterEnabled ?? 0,
      props: {
        options: [
          {
            label: '是',
            value: 1,
          },
          {
            label: '否',
            value: 0,
          },
        ],
      },
      rules: [{ required: true }],
      visible: !!bussinessDetail.middlewareType,
    },
    {
      type: 'input',
      label: '消费数据流ID',
      name: 'dataStreamIdentifier',
      initialValue: currentValues.dataStreamIdentifier,
      extra: '多个数据流ID之间用分号；隔开',
      rules: [{ required: true }],
      visible: values => bussinessDetail.middlewareType && values.filterEnabled,
    },
    {
      type: 'text',
      label: 'Master地址',
      name: 'masterUrl',
      initialValue: currentValues.masterUrl,
    },
  ] as FormItemProps[];

  return pickObjectArray(names, fields);
};
