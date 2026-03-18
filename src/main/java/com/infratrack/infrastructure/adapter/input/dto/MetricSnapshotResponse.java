package com.infratrack.infrastructure.adapter.input.dto;

import com.infratrack.domain.model.MetricSnapshot;

import java.time.Instant;

public record MetricSnapshotResponse(
        String assetId,
        double cpuUsage,
        double memoryUsage,
        double diskUsage,
        Instant collectedAt
) {
    public static MetricSnapshotResponse from(MetricSnapshot snapshot) {
        return new MetricSnapshotResponse(
                snapshot.assetId().toString(),
                snapshot.cpuUsage(),
                snapshot.memoryUsage(),
                snapshot.diskUsage(),
                snapshot.collectedAt()
        );
    }
}
