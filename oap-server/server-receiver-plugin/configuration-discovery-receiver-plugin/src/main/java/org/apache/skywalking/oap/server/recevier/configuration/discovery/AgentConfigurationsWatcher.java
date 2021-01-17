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
import org.apache.skywalking.oap.server.configuration.api.ConfigChangeWatcher;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;

/**
 * AgentConfigurationsWatcher used to handle dynamic configuration changes, and convert the configuration of the
 * String type to {@link AgentConfigurations}
 */
public class AgentConfigurationsWatcher extends ConfigChangeWatcher {
    private volatile String settingsString;
    private volatile AgentConfigurations activeAgentConfigurations;

    public AgentConfigurationsWatcher(AgentConfigurations agentConfigurations,
                                      ModuleProvider provider) {
        super(ConfigurationDiscoveryModule.NAME, provider, "agentConfigurations");
        this.settingsString = Const.EMPTY_STRING;
        this.activeAgentConfigurations = agentConfigurations;
    }

    @Override
    public void notify(ConfigChangeEvent value) {
        if (value.getEventType().equals(EventType.DELETE)) {
            settingsString = Const.EMPTY_STRING;
            this.activeAgentConfigurations = new AgentConfigurations();
        } else {
            settingsString = value.getNewValue();
            AgentConfigurationsReader agentConfigurationsReader =
                new AgentConfigurationsReader(new StringReader(value.getNewValue()));
            this.activeAgentConfigurations = agentConfigurationsReader.readAgentConfigurations();
        }
    }

    @Override
    public String value() {
        return settingsString;
    }

    public AgentConfigurations getActiveAgentConfigurations() {
        return activeAgentConfigurations;
    }
}
