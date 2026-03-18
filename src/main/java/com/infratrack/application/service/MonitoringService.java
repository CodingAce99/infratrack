package com.infratrack.application.service;

import com.infratrack.application.port.input.MonitorAssetUseCase;
import com.infratrack.application.port.output.AssetRepository;
import com.infratrack.application.port.output.MetricSnapshotRepository;
import com.infratrack.application.port.output.MetricsCollector;
import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.AssetStatus;
import com.infratrack.domain.model.MetricSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;


public class MonitoringService implements MonitorAssetUseCase {

    private final AssetRepository assetRepository;
    private final MetricsCollector metricsCollector;
    private final MetricSnapshotRepository snapshotRepository;
    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

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

    @Override
    public void collectAllActive() {
        List<Asset> activeAssets = assetRepository.findAll().stream()
                .filter(asset -> asset.getStatus() == AssetStatus.ACTIVE)
                .toList();

        log.info("Collecting metrics for {} active assets", activeAssets.size());

        activeAssets.forEach(asset ->
                Thread.startVirtualThread(() -> {
                    try {
                        collectNow(asset.getId());
                    } catch (Exception e) {
                        log.error("Metrics collection failed for asset {}: {}",
                                asset.getId(), e.getMessage());
                    }
                })
        );
    }
}
