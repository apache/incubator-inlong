#! /bin/sh

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
# <p>
# http://www.apache.org/licenses/LICENSE-2.0
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#======================================================================
# Stop the project service shell script
# Find PID by project name
# Then kill -9 pid
#
#======================================================================

# Project name
APPLICATION="inlong-manager-openapi"

# Project startup jar package name
APPLICATION_JAR="manager-openapi.jar"

PID=$(ps -ef | grep "${APPLICATION_JAR}" | grep -v grep | awk '{ print $2 }')
if [[ -z "$PID" ]]; then
  echo ${APPLICATION} is already stopped
else
  echo kill ${PID}
  kill -9 ${PID}
  echo ${APPLICATION} stopped successfully
fi
