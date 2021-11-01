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
import { Divider } from 'antd';
import i18n from '@/i18n';
import { genDataFields } from '@/components/AccessHelper';

export const getFilterFormContent = (defaultValues = {} as any) => [
  {
    type: 'inputsearch',
    name: 'keyWord',
    initialValue: defaultValues.keyWord,
  },
];

export const genExtraContent = ({ editingId, record, onSave, onCancel, onEdit, onDelete }) => {
  return editingId === record.id || (editingId === true && !record.id)
    ? [
        {
          label: i18n.t('pages.AccessDetail.DataStream.Config.Save'),
          onRun: onSave,
          disabled: false,
        },
        {
          label: i18n.t('pages.AccessDetail.DataStream.Config.Cancel'),
          onRun: onCancel,
          disabled: false,
        },
      ]
    : [
        {
          label: i18n.t('basic.Edit'),
          onRun: onEdit,
          disabled: editingId,
        },
        {
          label: i18n.t('basic.Delete'),
          onRun: onDelete,
          disabled: editingId,
        },
      ];
};

export const genFormContent = (editingId, currentValues, inlongGroupId, readonly) => {
  const extraParams = {
    inlongGroupId,
    useDataSourcesActionRequest: !!currentValues?.id,
    useDataStorageActionRequest: !!currentValues?.id,
    fieldListEditing: editingId && !currentValues?.id,
    readonly,
  };

  return [
    {
      type: (
        <Divider orientation="left">{i18n.t('pages.AccessCreate.DataStream.config.Basic')}</Divider>
      ),
    },
    ...genDataFields(
      ['inlongStreamId', 'name', 'inCharges', 'description'],
      currentValues,
      extraParams,
    ),
    {
      type: (
        <Divider orientation="left">
          {i18n.t('pages.AccessCreate.DataStream.config.DataSources')}
        </Divider>
      ),
    },
    ...genDataFields(['dataSourceType', 'dataSourcesConfig'], currentValues, extraParams),
    {
      type: (
        <Divider orientation="left">
          {i18n.t('pages.AccessCreate.DataStream.config.DataInfo')}
        </Divider>
      ),
    },
    ...genDataFields(
      ['dataType', 'dataEncoding', 'fileDelimiter', 'rowTypeFields'],
      currentValues,
      extraParams,
    ),
    {
      type: (
        <Divider orientation="left">
          {i18n.t('pages.AccessCreate.DataStream.config.DataStorages')}
        </Divider>
      ),
    },
    ...genDataFields(['dataStorage', 'dataStorageHIVE'], currentValues, extraParams),
  ].map(item => {
    if (
      (editingId === true && currentValues?.id === undefined) ||
      (item.name === 'dataStorage' && !readonly)
    ) {
      return item;
    }

    const obj = { ...item };

    if (!editingId || editingId !== currentValues?.id) {
      // Nothing is being edited, or the current line is not being edited
      delete obj.extra;
      delete obj.rules;
      if (typeof obj.type === 'string') {
        obj.type = 'text';
        obj.props = { options: obj.props?.options };
      } else if (obj.name === 'inCharges') {
        obj.type = <span>{currentValues?.inCharges?.join(', ')}</span>;
      }

      if ((obj.suffix as any)?.type) {
        (obj.suffix as any).type = 'text';
      }
    } else {
      // Current edit line
      if (['inlongStreamId', 'dataSourceType', 'dataType'].includes(obj.name as string)) {
        obj.type = 'text';
      }
    }

    return obj;
  });
};
