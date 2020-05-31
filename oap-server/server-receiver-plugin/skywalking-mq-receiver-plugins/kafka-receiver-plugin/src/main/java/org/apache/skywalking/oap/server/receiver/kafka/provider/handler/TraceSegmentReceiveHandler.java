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

package org.apache.skywalking.oap.server.receiver.kafka.provider.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.utils.Bytes;
import org.apache.skywalking.apm.network.language.agent.v3.SegmentObject;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.receiver.kafka.module.KafkaReceiverConfig;
import org.apache.skywalking.oap.server.receiver.trace.module.TraceModule;
import org.apache.skywalking.oap.server.receiver.trace.provider.TraceModuleProvider;
import org.apache.skywalking.oap.server.receiver.trace.provider.TraceServiceModuleConfig;
import org.apache.skywalking.oap.server.receiver.trace.provider.parser.SegmentParserListenerManager;
import org.apache.skywalking.oap.server.receiver.trace.provider.parser.TraceAnalyzer;
import org.apache.skywalking.oap.server.telemetry.TelemetryModule;
import org.apache.skywalking.oap.server.telemetry.api.CounterMetrics;
import org.apache.skywalking.oap.server.telemetry.api.HistogramMetrics;
import org.apache.skywalking.oap.server.telemetry.api.MetricsCreator;
import org.apache.skywalking.oap.server.telemetry.api.MetricsTag;

@Slf4j
public class TraceSegmentReceiveHandler implements KafkaReceiveHandler {
    public static final String TOPIC_NAME = "skywalking-segment";

    private final SegmentParserListenerManager listenerManager;
    private final ModuleManager moduleManager;

    private final TraceServiceModuleConfig traceModuleConfig;
    private final KafkaReceiverConfig config;

    private HistogramMetrics histogram;
    private CounterMetrics errorCounter;

    public TraceSegmentReceiveHandler(ModuleManager moduleManager,
                                      SegmentParserListenerManager listenerManager,
                                      KafkaReceiverConfig config) {
        this.moduleManager = moduleManager;
        this.listenerManager = listenerManager;
        this.config = config;
        traceModuleConfig = (TraceServiceModuleConfig) ((TraceModuleProvider) moduleManager.find(TraceModule.NAME)
                                                                                           .provider())
            .createConfigBeanIfAbsent();
        MetricsCreator metricsCreator = moduleManager.find(TelemetryModule.NAME)
                                                     .provider()
                                                     .getService(MetricsCreator.class);
        histogram = metricsCreator.createHistogramMetric(
            "trace_in_latency", "The process latency of trace data",
            new MetricsTag.Keys("protocol"), new MetricsTag.Values("grpc")
        );
        errorCounter = metricsCreator.createCounter(
            "trace_analysis_error_count",
            "The error number of trace analysis",
            new MetricsTag.Keys("protocol"),
            new MetricsTag.Values("grpc")
        );
    }

    @Override
    public void handle(final ConsumerRecord<String, Bytes> record) {
        try {
            SegmentObject segment = SegmentObject.parseFrom(record.value().get());
            if (log.isDebugEnabled()) {
                log.debug("receive segment");
            }

            HistogramMetrics.Timer timer = histogram.createTimer();
            try {
                final TraceAnalyzer traceAnalyzer = new TraceAnalyzer(
                    moduleManager, listenerManager, traceModuleConfig);
                traceAnalyzer.doAnalysis(segment);
            } catch (Exception e) {
                errorCounter.inc();
            } finally {
                timer.finish();
            }
        } catch (InvalidProtocolBufferException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public String getTopic() {
        return TOPIC_NAME;
    }

    @Override
    public TopicPartition getTopicPartition() {
        return new TopicPartition(TOPIC_NAME, config.getServerId());
    }
}
