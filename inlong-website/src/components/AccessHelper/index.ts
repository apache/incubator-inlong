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

// Field configuration list
export { default as genBussinessFields } from './FieldsConfig/bussinessFields';
export { default as genDataFields } from './FieldsConfig/dataFields';

// Data source creation/selector
export { default as DataSourcesCreateModal } from './DataSourcesEditor/CreateModal';

// Columns configuration related to all data sources
export {
  tableColumns as dataSourcesDbColumns,
  getCreateFormContent as getDataSourcesDbCreateFormContent,
} from './DataSourcesEditor/DbConfig';

export {
  tableColumns as dataSourcesFileColumns,
  getCreateFormContent as getDataSourcesFileCreateFormContent,
} from './DataSourcesEditor/FileConfig';

// Data source configuration editor
export { default as DataSourcesEditor } from './DataSourcesEditor';

// All flow-related columns configuration
export { hiveTableColumns as dataStorageHiveColumns } from './DataStorageEditor/hiveConfig';

// Flow editor
export { default as DataStorageEditor } from './DataStorageEditor/Editor';
export { default as DataStorageDetailModal } from './DataStorageEditor/DetailModal';
