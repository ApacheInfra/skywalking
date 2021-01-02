/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.core.query;

import java.io.IOException;
import java.util.List;
import org.apache.skywalking.apm.util.StringUtil;
import org.apache.skywalking.oap.server.core.analysis.IDManager;
import org.apache.skywalking.oap.server.core.analysis.manual.searchtag.Tag;
import org.apache.skywalking.oap.server.core.query.input.TraceScopeCondition;
import org.apache.skywalking.oap.server.core.query.type.LogState;
import org.apache.skywalking.oap.server.core.query.type.Logs;
import org.apache.skywalking.oap.server.core.query.type.Pagination;
import org.apache.skywalking.oap.server.core.storage.StorageModule;
import org.apache.skywalking.oap.server.core.storage.query.ILogQueryDAO;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.module.Service;

public class LogQueryService implements Service {

    private final ModuleManager moduleManager;
    private ILogQueryDAO logQueryDAO;

    public LogQueryService(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private ILogQueryDAO getLogQueryDAO() {
        if (logQueryDAO == null) {
            this.logQueryDAO = moduleManager.find(StorageModule.NAME).provider().getService(ILogQueryDAO.class);
        }
        return logQueryDAO;
    }

    public Logs queryLogs(String metricName,
                          String serviceId,
                          String serviceInstanceId,
                          String endpointId,
                          String endpointName,
                          TraceScopeCondition relatedTrace,
                          LogState state,
                          Pagination paging,
                          final long startTB,
                          final long endTB,
                          final List<Tag> tags) throws IOException {
        PaginationUtils.Page page = PaginationUtils.INSTANCE.exchange(paging);

        Logs logs = getLogQueryDAO().queryLogs(metricName,
                                               serviceId,
                                               serviceInstanceId,
                                               endpointId,
                                               endpointName,
                                               relatedTrace,
                                               state,
                                               paging,
                                               page.getFrom(), page.getLimit(),
                                               startTB, endTB, tags
        );
        logs.getLogs().forEach(log -> {
            if (StringUtil.isNotEmpty(log.getServiceId())) {
                final IDManager.ServiceID.ServiceIDDefinition serviceIDDefinition = IDManager.ServiceID.analysisId(
                    log.getServiceId());
                log.setServiceName(serviceIDDefinition.getName());
            }
            if (StringUtil.isNotEmpty(log.getServiceInstanceId())) {
                final IDManager.ServiceInstanceID.InstanceIDDefinition instanceIDDefinition = IDManager.ServiceInstanceID
                    .analysisId(log.getServiceInstanceId());
                log.setServiceInstanceName(instanceIDDefinition.getName());
            }
        });
        return logs;
    }
}
