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

package org.apache.skywalking.e2e.metrics;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.skywalking.e2e.AbstractQuery;

@Data
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class MetricsQuery extends AbstractQuery<MetricsQuery> {
    public static String SERVICE_SLA = "service_sla";
    public static String SERVICE_CPM = "service_cpm";
    public static String SERVICE_RESP_TIME = "service_resp_time";
    public static String SERVICE_APDEX = "service_apdex";
    public static String[] ALL_SERVICE_METRICS = {
        SERVICE_SLA,
        SERVICE_CPM,
        SERVICE_RESP_TIME,
        SERVICE_APDEX
    };
    public static String SERVICE_PERCENTILE = "service_percentile";
    public static String[] ALL_SERVICE_MULTIPLE_LINEAR_METRICS = {
        SERVICE_PERCENTILE
    };

    public static String ENDPOINT_CPM = "endpoint_cpm";
    public static String ENDPOINT_AVG = "endpoint_avg";
    public static String ENDPOINT_SLA = "endpoint_sla";
    public static String[] ALL_ENDPOINT_METRICS = {
        ENDPOINT_CPM,
        ENDPOINT_AVG,
        ENDPOINT_SLA,
        };
    public static String ENDPOINT_PERCENTILE = "endpoint_percentile";
    public static String[] ALL_ENDPOINT_MULTIPLE_LINEAR_METRICS = {
        ENDPOINT_PERCENTILE
    };

    public static String SERVICE_INSTANCE_RESP_TIME = "service_instance_resp_time";
    public static String SERVICE_INSTANCE_CPM = "service_instance_cpm";
    public static String SERVICE_INSTANCE_SLA = "service_instance_sla";
    public static String[] ALL_INSTANCE_METRICS = {
        SERVICE_INSTANCE_RESP_TIME,
        SERVICE_INSTANCE_CPM,
        SERVICE_INSTANCE_SLA
    };

    public static String INSTANCE_JVM_THREAD_LIVE_COUNT = "instance_jvm_thread_live_count";
    public static String INSTANCE_JVM_THREAD_DAEMON_COUNT = "instance_jvm_thread_daemon_count";
    public static String INSTANCE_JVM_THREAD_PEAK_COUNT = "instance_jvm_thread_peak_count";
    public static String [] ALL_INSTANCE_JVM_METRICS = {
        INSTANCE_JVM_THREAD_DAEMON_COUNT,
        INSTANCE_JVM_THREAD_DAEMON_COUNT,
        INSTANCE_JVM_THREAD_PEAK_COUNT
    };

    public static String SERVICE_RELATION_CLIENT_CPM = "service_relation_client_cpm";
    public static String SERVICE_RELATION_SERVER_CPM = "service_relation_server_cpm";
    public static String SERVICE_RELATION_CLIENT_CALL_SLA = "service_relation_client_call_sla";
    public static String SERVICE_RELATION_SERVER_CALL_SLA = "service_relation_server_call_sla";
    public static String SERVICE_RELATION_CLIENT_RESP_TIME = "service_relation_client_resp_time";
    public static String SERVICE_RELATION_SERVER_RESP_TIME = "service_relation_server_resp_time";
    public static String SERVICE_RELATION_CLIENT_P99 = "service_relation_client_p99";
    public static String SERVICE_RELATION_SERVER_P99 = "service_relation_server_p99";
    public static String[] ALL_SERVICE_RELATION_CLIENT_METRICS = {
        SERVICE_RELATION_CLIENT_CPM
    };

    public static String[] ALL_SERVICE_RELATION_SERVER_METRICS = {
        SERVICE_RELATION_SERVER_CPM
    };

    public static String SERVICE_INSTANCE_RELATION_CLIENT_CPM = "service_instance_relation_client_cpm";
    public static String SERVICE_INSTANCE_RELATION_SERVER_CPM = "service_instance_relation_server_cpm";
    public static String SERVICE_INSTANCE_RELATION_CLIENT_CALL_SLA = "service_instance_relation_client_call_sla";
    public static String SERVICE_INSTANCE_RELATION_SERVER_CALL_SLA = "service_instance_relation_server_call_sla";
    public static String SERVICE_INSTANCE_RELATION_CLIENT_RESP_TIME = "service_instance_relation_client_resp_time";
    public static String SERVICE_INSTANCE_RELATION_SERVER_RESP_TIME = "service_instance_relation_server_resp_time";
    public static String SERVICE_INSTANCE_RELATION_CLIENT_P99 = "service_instance_relation_client_p99";
    public static String SERVICE_INSTANCE_RELATION_SERVER_P99 = "service_instance_relation_server_p99";
    public static String[] ALL_SERVICE_INSTANCE_RELATION_CLIENT_METRICS = {
        SERVICE_INSTANCE_RELATION_CLIENT_CPM
    };

    public static String[] ALL_SERVICE_INSTANCE_RELATION_SERVER_METRICS = {
        SERVICE_INSTANCE_RELATION_SERVER_CPM
    };

    public static String METER_INSTANCE_CPU_PERCENTAGE = "meter_instance_cpu_percentage";
    public static String METER_INSTANCE_JVM_MEMORY_BYTES_USED = "meter_instance_jvm_memory_bytes_used";
    public static String METER_INSTANCE_TRACE_COUNT = "meter_instance_trace_count";
    public static String METER_INSTANCE_METRICS_FIRST_AGGREGATION = "meter_instance_metrics_first_aggregation";
    public static String METER_INSTANCE_PERSISTENCE_PREPARE_COUNT = "meter_instance_persistence_prepare_count";
    public static String METER_INSTANCE_PERSISTENCE_EXECUTE_COUNT = "meter_instance_persistence_execute_count";

    public static String[] ALL_SO11Y_LINER_METRICS = {
        METER_INSTANCE_CPU_PERCENTAGE,
        METER_INSTANCE_JVM_MEMORY_BYTES_USED,
        METER_INSTANCE_TRACE_COUNT,
        METER_INSTANCE_METRICS_FIRST_AGGREGATION,
        METER_INSTANCE_PERSISTENCE_PREPARE_COUNT,
        METER_INSTANCE_PERSISTENCE_EXECUTE_COUNT 
    };

    public static String METER_INSTANCE_PERSISTENCE_EXECUTE_PERCENTILE = "meter_instance_persistence_execute_percentile";

    public static String[] ALL_SO11Y_LABELED_METRICS = {
        METER_INSTANCE_PERSISTENCE_EXECUTE_PERCENTILE
    };
    private String id;
    private String metricsName;

}
