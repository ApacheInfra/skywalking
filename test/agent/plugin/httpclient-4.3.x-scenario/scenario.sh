#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

PRG="$0"
AGENT_HOME=$2
TESTCASE_VERSION=$1
PRGDIR=`dirname "$PRG"`
[ -z "$TESTCASE_HOME" ] && TESTCASE_HOME=`cd "$PRGDIR" >/dev/null; pwd`

docker run -itd  \
        --name skywalking-agent-test-httpclient-4.3.x-${TESTCASE_VERSION}  \
        --env SCENARIO_NAME=httpclient-4.3.x    \
        --env SCENARIO_VERSION=${TESTCASE_VERSION}  \
        --env SCENARIO_SUPPORT_FRAMEWORK=httpclient \
        --env SCENARIO_ENTRY_SERVICE=http://localhost:8080/httpclient-4.3.x-scenario/case/httpclient  \
        --env SCENARIO_HEALTH_CHECK_URL=http://localhost:8080/httpclient-4.3.x-scenario/healthCheck \
        -v $AGENT_HOME:/usr/local/skywalking-agent-scenario/agent \
        -v $TESTCASE_HOME/data:/usr/local/skywalking-agent-scenario/data \
        -v $TESTCASE_HOME/packages:/usr/local/skywalking-agent-scenario/packages \
        skyapm/agent-test-tomcat:latest
