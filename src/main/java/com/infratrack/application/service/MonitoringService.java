package com.infratrack.application.service;

import com.infratrack.application.port.input.MonitorAssetUseCase;
import com.infratrack.application.port.output.AssetRepository;
import com.infratrack.application.port.output.MetricSnapshotRepository;
import com.infratrack.application.port.output.MetricsCollector;
import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.MetricSnapshot;

import java.util.List;
import java.util.Objects;

public class MonitoringService implements MonitorAssetUseCase {

    private final AssetRepository assetRepository;
    private final MetricsCollector metricsCollector;
    private final MetricSnapshotRepository snapshotRepository;

    public MonitoringService(
            AssetRepository assetRepository,
            MetricsCollector metricsCollector,
            MetricSnapshotRepository snapshotRepository
    ) {
        this.assetRepository = Objects.requireNonNull(assetRepository, "assetRepository cannot be null");
        this.metricsCollector = Objects.requireNonNull(metricsCollector, "metricsCollector cannot be null");
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository, "snapshotRepository cannot be null");
    }

    @Override
    public MetricSnapshot collectNow(AssetId assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Asset Not Found: " + assetId.getValue()
                ));

        MetricSnapshot snapshot = metricsCollector.collect(asset);
        snapshotRepository.save(snapshot);
        return snapshot;
    }

    @Override
    public List<MetricSnapshot> getHistory(AssetId assetId, int limit) {
        return snapshotRepository.findLatestByAssetId(assetId, limit);
    }
}
