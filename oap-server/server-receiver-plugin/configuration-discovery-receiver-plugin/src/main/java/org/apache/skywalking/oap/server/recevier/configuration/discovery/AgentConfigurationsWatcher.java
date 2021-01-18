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

package org.apache.skywalking.oap.server.recevier.configuration.discovery;

import java.io.StringReader;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.skywalking.oap.server.configuration.api.ConfigChangeWatcher;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;

/**
 * AgentConfigurationsWatcher used to handle dynamic configuration changes.
 */
public class AgentConfigurationsWatcher extends ConfigChangeWatcher {
    private volatile String settingsString;
    private volatile AgentConfigurationsTable agentConfigurationsTable;
    /**
     * The uuid is based on calculation by md5.
     */
    private volatile String uuid;

    public AgentConfigurationsWatcher(ModuleProvider provider) {
        super(ConfigurationDiscoveryModule.NAME, provider, "agentConfigurations");
        this.settingsString = Const.EMPTY_STRING;
        this.agentConfigurationsTable = new AgentConfigurationsTable();
    }

    /**
     * For testing use only. manually load configuration items.
     */
    public AgentConfigurationsWatcher(AgentConfigurationsTable agentConfigurationsTable, ModuleProvider provider) {
        super(ConfigurationDiscoveryModule.NAME, provider, "agentConfigurations");
        this.settingsString = Const.EMPTY_STRING;
        this.agentConfigurationsTable = agentConfigurationsTable;
    }

    @Override
    public void notify(ConfigChangeEvent value) {
        if (value.getEventType().equals(EventType.DELETE)) {
            settingsString = Const.EMPTY_STRING;
            this.agentConfigurationsTable = new AgentConfigurationsTable();
        } else {
            settingsString = value.getNewValue();
            AgentConfigurationsReader agentConfigurationsReader =
                new AgentConfigurationsReader(new StringReader(value.getNewValue()));
            this.agentConfigurationsTable = agentConfigurationsReader.readAgentConfigurationsTable();
        }
        uuid = DigestUtils.md5Hex(settingsString);
    }

    @Override
    public String value() {
        return settingsString;
    }

    /**
     * For testing use only.
     */
    public AgentConfigurationsTable getAgentConfigurationsTable() {
        return agentConfigurationsTable;
    }

    public AgentConfigurations getAgentConfigurations(String service) {
        return agentConfigurationsTable.getAgentConfigurationsCache().get(service);
    }

    public String getLatestUUID() {
        return uuid;
    }
}
