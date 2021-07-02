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
import {
  Input,
  DatePicker,
  TimePicker,
  Switch,
  Checkbox,
  Radio,
  InputNumber,
  Cascader,
  TreeSelect,
  Slider,
  AutoComplete,
  Transfer,
} from 'antd';
import HighSelect from '@/components/HighSelect';

const text: React.FC<Record<string, any>> = ({ value, options }) => {
  if (!Array.isArray(options)) return <span>{value}</span>;

  return (
    (options &&
      (Array.isArray(value)
        ? options
            .filter(item => value.includes(item.value))
            .map(item => item.label)
            .join(', ')
        : options.find(item => item.value === value)?.label)) || <span>{value}</span>
  );
};

const select = props => (
  <HighSelect
    placeholder={props.placeholder || '请选择'}
    style={{ minWidth: 100, ...props?.style }}
    {...props}
  />
);

const input = props => <Input placeholder={props.placeholder || '请输入'} {...props} />;

const inputsearch = props => (
  <Input.Search placeholder={props.placeholder || '请输入'} {...props} />
);

const password = props => <Input.Password {...props} />;

const inputnumber = props => <InputNumber {...props} />;

const textarea = props => <Input.TextArea {...props} />;

const datepicker = props => <DatePicker {...props} />;

const timepicker = props => <TimePicker {...props} />;

const switchbox = props => <Switch {...props} />;

const checkbox = props => <Checkbox {...props} />;

const checkboxgroup = props => <Checkbox.Group {...props} />;

const radio = props => <Radio.Group {...props} />;

const radiobutton = ({ options, ...rest }) => (
  <Radio.Group {...rest}>
    {options &&
      options.map(
        (v: any): React.ReactNode => (
          <Radio.Button key={v.key || v.value.toString()} value={v.value} disabled={!!v.disabled}>
            {v.label}
          </Radio.Button>
        ),
      )}
  </Radio.Group>
);

const rangepicker = props => <DatePicker.RangePicker {...props} />;

const cascader = props => <Cascader {...props} />;

const treeselect = props => <TreeSelect {...props} />;

const slider = props => <Slider {...props} />;

const autocomplete = props => <AutoComplete {...props} />;

const transfer = props => <Transfer {...props} />;

export default {
  text,
  input,
  inputsearch,
  password,
  select,
  textarea,
  datepicker,
  timepicker,
  switchbox,
  checkbox,
  checkboxgroup,
  radio,
  radiobutton,
  inputnumber,
  rangepicker,
  cascader,
  treeselect,
  slider,
  autocomplete,
  transfer,
};
