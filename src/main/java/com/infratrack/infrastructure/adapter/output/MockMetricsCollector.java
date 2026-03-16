package com.infratrack.infrastructure.adapter.output;

import com.infratrack.application.port.output.MetricsCollector;
import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.MetricSnapshot;

import java.util.concurrent.ThreadLocalRandom;

public class MockMetricsCollector implements MetricsCollector {

    @Override
    public MetricSnapshot collect(Asset asset) {
                double cpuUsage = ThreadLocalRandom.current().nextDouble(0.0, 90.0);
                double memoryUsage = ThreadLocalRandom.current().nextDouble(0.0, 90.0);
                double diskUsage = ThreadLocalRandom.current().nextDouble(0.0, 90.0);
        return MetricSnapshot.of(asset.getId(), cpuUsage, memoryUsage, diskUsage);
    }
}
