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

import React, { useState, useMemo, useEffect } from 'react';
import { Button, Table, Modal, message } from 'antd';
import request from '@/utils/request';
import isEqual from 'lodash/isEqual';
import DetailModal from './DetailModal';
import { hiveTableColumns } from './hiveConfig';

export interface Props {
  value?: Record<string, any>[];
  onChange?: Function;
  readonly?: boolean;
  // datastorage type
  type: string;
  // defaultRowTypeFields, can be used to automatically fill in the form default values
  defaultRowTypeFields?: Record<string, unknown>[];
  dataType?: string;
  // Whether to use real operations (for example, to call the background interface when deleting/newing, etc.)
  useActionRequest?: boolean;
  businessIdentifier?: string;
  // Data stream ID, required for real operation
  dataStreamIdentifier?: string;
}

const removeIdFromValues = values =>
  values.map(item => {
    const obj = { ...item };
    delete obj._etid;
    return obj;
  });

const addIdToValues = values =>
  values?.map(item => {
    const obj = { ...item };
    obj._etid = Math.random().toString();
    return obj;
  });

const Comp = ({
  value,
  onChange,
  readonly,
  type = 'HIVE',
  defaultRowTypeFields,
  dataType,
  useActionRequest,
  businessIdentifier,
  dataStreamIdentifier,
}: Props) => {
  const [data, setData] = useState(addIdToValues(value) || []);

  useEffect(() => {
    if (value && !isEqual(value, removeIdFromValues(data))) {
      setData(addIdToValues(value));
    }
    // eslint-disable-next-line
  }, [value]);

  const [detailModal, setDetailModal] = useState({
    visible: false,
    _etid: '',
    id: '',
    record: {},
  }) as any;

  const triggerChange = newData => {
    if (onChange) {
      onChange(removeIdFromValues(newData));
    }
  };

  const onSaveRequest = async values => {
    const isUpdate = detailModal.id;
    const submitData = {
      ...values,
      storageType: type,
      businessIdentifier,
      dataStreamIdentifier,
    };
    if (isUpdate) submitData.id = detailModal.id;
    const newId = await request({
      url: `/storage/${isUpdate ? 'update' : 'save'}`,
      method: 'POST',
      data: submitData,
    });
    return isUpdate ? detailModal.id : newId;
  };

  const onAddRow = rowValues => {
    const newData = data.concat(addIdToValues([rowValues]));
    setData(newData);
    triggerChange(newData);
  };

  const onDeleteRequest = id => {
    return new Promise(resolve => {
      Modal.confirm({
        title: '确认删除吗',
        onOk: async () => {
          await request({
            url: `/storage/delete/${id}`,
            method: 'DELETE',
            params: {
              storageType: type,
            },
          });
          resolve(true);
          message.success('删除成功');
        },
      });
    });
  };

  const onDeleteRow = async record => {
    const { _etid, id } = record;
    if (useActionRequest) {
      await onDeleteRequest(id);
    }
    const newData = [...data];
    const index = newData.findIndex(item => item._etid === _etid);
    newData.splice(index, 1);
    setData(newData);
    triggerChange(newData);
  };

  const onEditRow = record => {
    setDetailModal({
      visible: true,
      id: useActionRequest ? record?.id : true,
      _etid: record._etid,
      record,
    });
  };

  const onUpdateRow = (_etid, rowValues) => {
    const newData = data.map(item => {
      if (item._etid === _etid) {
        return {
          ...item,
          ...rowValues,
        };
      }
      return item;
    });

    setData(newData);
    triggerChange(newData);
  };

  const tableColumns = useMemo(() => {
    return {
      HIVE: hiveTableColumns,
    }[type];
  }, [type]) as any;

  const columns = tableColumns.concat(
    readonly
      ? []
      : [
          {
            title: '操作',
            dataIndex: 'actions',
            render: (text, record) => (
              <>
                <Button type="link" onClick={() => onEditRow(record)}>
                  编辑
                </Button>
                <Button type="link" onClick={() => onDeleteRow(record)}>
                  删除
                </Button>
              </>
            ),
          },
        ],
  );

  return (
    <>
      <div>
        {`${type}流向配置`}
        {!readonly && (
          <Button
            type="link"
            onClick={() => setDetailModal({ visible: true })}
            disabled={data.length}
          >
            添加
          </Button>
        )}
      </div>

      <Table pagination={false} size="small" dataSource={data} columns={columns} rowKey="_etid" />

      <DetailModal
        {...detailModal}
        bid={businessIdentifier}
        id={detailModal.id !== true && detailModal.id}
        dataType={dataType}
        defaultRowTypeFields={defaultRowTypeFields}
        storageType={type}
        onOk={async values => {
          const isUpdate = detailModal.id;
          const id = useActionRequest ? await onSaveRequest(values) : '';
          const result = id ? { id, ...values } : { ...detailModal.record, ...values };
          isUpdate ? onUpdateRow(detailModal._etid, result) : onAddRow(result);
          setDetailModal({ visible: false });
        }}
        onCancel={() => setDetailModal({ visible: false })}
      />
    </>
  );
};

export default Comp;
