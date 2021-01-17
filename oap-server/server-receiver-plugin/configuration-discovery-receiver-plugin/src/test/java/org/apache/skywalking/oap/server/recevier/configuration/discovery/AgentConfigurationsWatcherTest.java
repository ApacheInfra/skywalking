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

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import org.apache.skywalking.oap.server.configuration.api.ConfigChangeWatcher;
import org.apache.skywalking.oap.server.library.util.ResourceUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

public class AgentConfigurationsWatcherTest {
    @Spy
    private AgentConfigurationsWatcher agentConfigurationsWatcher = new AgentConfigurationsWatcher(
        new AgentConfigurations(), null);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConfigModifyEvent() throws IOException {
        assertTrue(agentConfigurationsWatcher.getActiveAgentConfigurations().getConfigurationMap().isEmpty());

        Reader reader = ResourceUtils.read("agent-dynamic-configuration.yml");
        char[] chars = new char[1024 * 1024];
        int length = reader.read(chars);

        agentConfigurationsWatcher.notify(new ConfigChangeWatcher.ConfigChangeEvent(
            new String(chars, 0, length),
            ConfigChangeWatcher.EventType.MODIFY
        ));

        AgentConfigurations agentConfigurations = agentConfigurationsWatcher.getActiveAgentConfigurations();
        Map<String, ServiceConfiguration> configurationMap = agentConfigurations.getConfigurationMap();
        Assert.assertEquals(2, configurationMap.size());
        ServiceConfiguration serviceConfigurationProvider = configurationMap.get("serviceA");
        Assert.assertEquals("serviceA", serviceConfigurationProvider.getService());
        Assert.assertEquals(2, serviceConfigurationProvider.getConfiguration().size());
        Assert.assertEquals("1000", serviceConfigurationProvider.getConfiguration().get("trace.sample_rate"));
        Assert.assertEquals(
            "/api/seller/seller/*", serviceConfigurationProvider.getConfiguration().get("trace.ignore_path"));

        ServiceConfiguration serviceConfigurationConsumer = configurationMap.get("serviceB");
        Assert.assertEquals("serviceB", serviceConfigurationConsumer.getService());
        Assert.assertEquals(2, serviceConfigurationConsumer.getConfiguration().size());
        Assert.assertEquals("1000", serviceConfigurationConsumer.getConfiguration().get("trace.sample_rate"));
        Assert.assertEquals(
            "/api/seller/seller/*", serviceConfigurationConsumer.getConfiguration().get("trace.ignore_path"));
    }

    @Test
    public void testConfigDeleteEvent() throws IOException {
        Reader reader = ResourceUtils.read("agent-dynamic-configuration.yml");
        AgentConfigurations configurations = new AgentConfigurationsReader(reader).readAgentConfigurations();

        agentConfigurationsWatcher = spy(new AgentConfigurationsWatcher(configurations, null));

        agentConfigurationsWatcher.notify(
            new ConfigChangeWatcher.ConfigChangeEvent("whatever", ConfigChangeWatcher.EventType.DELETE));

        AgentConfigurations agentConfigurations = agentConfigurationsWatcher.getActiveAgentConfigurations();
        Map<String, ServiceConfiguration> configurationMap = agentConfigurations.getConfigurationMap();
        Assert.assertEquals(0, configurationMap.size());
        ServiceConfiguration serviceConfigurationProvider = configurationMap.get("serviceA");
        ServiceConfiguration serviceConfigurationConsumer = configurationMap.get("serviceB");

        Assert.assertNull(null, serviceConfigurationProvider);
        Assert.assertNull(null, serviceConfigurationConsumer);
    }
}