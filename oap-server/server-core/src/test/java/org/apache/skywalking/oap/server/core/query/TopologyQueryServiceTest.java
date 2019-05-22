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

import com.google.common.collect.Lists;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.query.entity.Call;
import org.apache.skywalking.oap.server.core.query.entity.Node;
import org.apache.skywalking.oap.server.core.query.entity.Step;
import org.apache.skywalking.oap.server.core.query.entity.Topology;
import org.apache.skywalking.oap.server.core.storage.query.ITopologyQueryDAO;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by dengming in 2019-05-19
 */
public class TopologyQueryServiceTest extends AbstractTopologyTest {

    private TopologyQueryService queryService = new TopologyQueryService(moduleManager);

    private ITopologyQueryDAO queryDAO = mock(ITopologyQueryDAO.class);

    @Before
    public void setUp() throws Exception {
        when(moduleServiceHolder.getService(ITopologyQueryDAO.class)).thenReturn(queryDAO);
    }

    @Test
    public void getGlobalTopology() throws Exception {
        List<Call.CallDetail> serverCalls = mockServerCall();
        List<Call.CallDetail> clientCalls = mockClientCall();

        when(queryDAO.loadServerSideServiceRelations(Step.MONTH, START_TB, END_TB)).thenReturn(serverCalls);
        when(queryDAO.loadClientSideServiceRelations(Step.MONTH, START_TB, END_TB)).thenReturn(clientCalls);
        Topology topology = queryService.getGlobalTopology(Step.MONTH, START_TB, END_TB, 123, 234);
        assertNotNull(topology);
        assertTopology(topology);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getServiceTopology() throws Exception {

        List<Call.CallDetail> serverCalls = mockServerCall();
        List<Call.CallDetail> clientCalls = mockClientCall();

        List<Call.CallDetail> sourceCalls = mockSourceServiceCall();

        when(queryDAO.loadSpecifiedServerSideServiceRelations(any(Step.class), anyLong(),
                anyLong(), anyListOf(Integer.class))).thenReturn(serverCalls, sourceCalls);
        when(queryDAO.loadSpecifiedClientSideServiceRelations(any(Step.class), anyLong(),
                anyLong(), anyListOf(Integer.class))).thenReturn(clientCalls);

        Topology topology = queryService.getServiceTopology(Step.MONTH, START_TB, END_TB, 12);
        assertTopology(topology);
        assertTrue(topology.getNodes().stream()
                .anyMatch(node -> "component-name-3".equals(node.getType()) && node.getId() == 33));
    }

    @Test
    public void getEndpointTopology() throws Exception {
        List<Call.CallDetail> serverCalls = mockServerCall();
        when(queryDAO.loadSpecifiedDestOfServerSideEndpointRelations(any(Step.class), anyLong(),
                anyLong(), anyInt())).thenReturn(serverCalls);
        Topology topology = queryService.getEndpointTopology(Step.MONTH, START_TB, END_TB, 1);

        assertNotNull(topology);

        assertEquals(4, topology.getCalls().size());

        for (int i = 0; i < 4; i++) {
            Call.CallDetail serverDetail = serverCalls.get(i);
            Call topologyCall = topology.getCalls().get(i);
            assertEquals(serverDetail.getSource(), topologyCall.getSource());
            assertEquals(serverDetail.getTarget(), topologyCall.getTarget());
        }

        assertEquals(8, topology.getNodes().size());
        assertNode(0, topology.getNodes().get(0));
        assertNode(100, topology.getNodes().get(1));
        assertNode(101, topology.getNodes().get(2));
        assertNode(111, topology.getNodes().get(3));
        assertNode(102, topology.getNodes().get(4));
        assertNode(122, topology.getNodes().get(5));
        assertNode(103, topology.getNodes().get(6));
        assertNode(133, topology.getNodes().get(7));

    }


    private void assertNode(int expectedId, Node node) {
        assertEquals(expectedId, node.getId());
        assertEquals(Const.EMPTY_STRING, node.getType());
        assertEquals(ENDPOINT_INVENTORY_NAME, node.getName());
    }

    private List<Call.CallDetail> mockSourceServiceCall() {
        Call.CallDetail call = new Call.CallDetail();
        call.setTarget(33);
        call.setComponentId(3);
        return Lists.newArrayList(call);
    }
}