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

package org.apache.skywalking.apm.plugin.cassandra.v3;

import jnr.ffi.annotations.In;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.util.StringUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * @author stone.wlg
 */
public class ClusterConstructorWithStateListenerArgInterceptor implements InstanceConstructorInterceptor {
    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        List<InetSocketAddress> inetSocketAddresses = (List<InetSocketAddress>) allArguments[1];
        List<String> hosts = new ArrayList<String>();
        for (InetSocketAddress inetSocketAddress : inetSocketAddresses) {
            hosts.add(inetSocketAddress.getHostName() + ":" + inetSocketAddress.getPort());
        }
        String contactPoints = StringUtil.join(',', hosts.toArray(new String[0]));

        objInst.setSkyWalkingDynamicField(
            new ConnectionInfo(contactPoints)
        );
    }
}
