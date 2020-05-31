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
import org.apache.skywalking.apm.network.language.profile.v3.ThreadSnapshot;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.analysis.worker.RecordStreamProcessor;
import org.apache.skywalking.oap.server.core.profile.ProfileThreadSnapshotRecord;
import org.apache.skywalking.oap.server.receiver.kafka.module.KafkaReceiverConfig;

/**
 *
 */
@Slf4j
public class ProfileTaskReceiveHandler implements KafkaReceiveHandler {
    public static final String TOPIC_NAME = "skywalking-profile";

    private final KafkaReceiverConfig config;

    public ProfileTaskReceiveHandler(KafkaReceiverConfig config) {
        this.config = config;
    }

    @Override
    public void handle(final ConsumerRecord<String, Bytes> record) {
        try {
            ThreadSnapshot snapshot = ThreadSnapshot.parseFrom(record.value().get());

            final ProfileThreadSnapshotRecord snapshotRecord = new ProfileThreadSnapshotRecord();
            snapshotRecord.setTaskId(snapshot.getTaskId());
            snapshotRecord.setSegmentId(snapshot.getTraceSegmentId());
            snapshotRecord.setDumpTime(snapshot.getTime());
            snapshotRecord.setSequence(snapshot.getSequence());
            snapshotRecord.setStackBinary(snapshot.getStack().toByteArray());
            snapshotRecord.setTimeBucket(TimeBucket.getRecordTimeBucket(snapshot.getTime()));

            RecordStreamProcessor.getInstance().in(snapshotRecord);
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
