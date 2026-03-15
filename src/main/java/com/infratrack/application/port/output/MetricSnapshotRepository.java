package com.infratrack.application.port.output;

import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.MetricSnapshot;

import java.util.List;

public interface MetricSnapshotRepository {

    void save(MetricSnapshot snapshot);

    List<MetricSnapshot> findLatestByAssetId(AssetId assetId, int numberOfSnapshots);
}
