package com.infratrack.infrastructure.adapter.output;

import com.infratrack.application.port.output.MetricSnapshotRepository;
import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.MetricSnapshot;

import java.util.ArrayList;
import java.util.List;

public class InMemoryMetricsSnapshotRepository implements MetricSnapshotRepository {

    private final List<MetricSnapshot> store = new ArrayList<>();

    @Override
    public void save(MetricSnapshot snapshot) {
        store.add(snapshot);
    }

    @Override
    public List<MetricSnapshot> findLatestByAssetId(AssetId assetId, int numberOfSnapshots) {
        return store.stream()
                .filter(s -> s.assetId().equals(assetId))
                .sorted((a, b) -> b.collectedAt().compareTo(a.collectedAt()))
                .limit(numberOfSnapshots)
                .toList();
    }
}
