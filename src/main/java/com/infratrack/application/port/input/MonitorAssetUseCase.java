package com.infratrack.application.port.input;


import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.MetricSnapshot;

import java.util.List;

public interface MonitorAssetUseCase {

    MetricSnapshot collectNow(AssetId assetId);

    List<MetricSnapshot> getHistory(AssetId assetId, int limit);

    void collectAllActive();
}
