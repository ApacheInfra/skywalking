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

package org.apache.skywalking.apm.meter.micrometer;

import io.micrometer.core.instrument.AbstractDistributionSummary;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.apache.skywalking.apm.toolkit.meter.Counter;
import org.apache.skywalking.apm.toolkit.meter.Gauge;
import org.apache.skywalking.apm.toolkit.meter.Histogram;
import org.apache.skywalking.apm.toolkit.meter.MeterId;
import org.apache.skywalking.apm.toolkit.meter.Percentile;

import java.util.Optional;
import java.util.concurrent.atomic.DoubleAccumulator;

public class SkywalkingDistributionSummary extends AbstractDistributionSummary {

    private final Counter counter;
    private final Counter sum;
    private final Gauge max;
    private final DoubleAccumulator maxAdder;

    private final Optional<Histogram> histogram;
    private final Optional<Percentile> percentile;

    protected SkywalkingDistributionSummary(Id id, MeterId meterId, Clock clock, DistributionStatisticConfig distributionStatisticConfig, double scale, boolean supportsAggregablePercentiles) {
        super(id, clock, distributionStatisticConfig, scale, supportsAggregablePercentiles);

        // meter base name
        String baseName = meterId.getName();

        this.counter = new Counter.Builder(meterId.copyTo(baseName + "_count", MeterId.MeterType.COUNTER)).build();
        this.sum = new Counter.Builder(meterId.copyTo(baseName + "_sum", MeterId.MeterType.COUNTER)).build();
        this.maxAdder = new DoubleAccumulator((a, b) -> a > b ? a : b, 0.000);
        this.max = new Gauge.Builder(meterId.copyTo(baseName + "_max", MeterId.MeterType.GAUGE),
            () -> maxAdder.doubleValue()).build();

        this.histogram = MeterBuilder.buildHistogram(meterId, supportsAggregablePercentiles, distributionStatisticConfig);
        this.percentile = MeterBuilder.buildPercentile(meterId, distributionStatisticConfig);
    }

    @Override
    protected void recordNonNegative(double amount) {
        counter.increment(1d);
        this.sum.increment(amount);
        maxAdder.accumulate(amount);

        histogram.ifPresent(h -> h.addValue(amount));
        percentile.ifPresent(p -> p.record(amount));
    }

    @Override
    public long count() {
        return (long) counter.get();
    }

    @Override
    public double totalAmount() {
        return sum.get();
    }

    @Override
    public double max() {
        return max.get();
    }
}
