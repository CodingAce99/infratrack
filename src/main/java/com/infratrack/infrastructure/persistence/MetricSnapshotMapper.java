package com.infratrack.infrastructure.persistence;

import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.MetricSnapshot;

import java.util.UUID;

public class MetricSnapshotMapper {

    private MetricSnapshotMapper() {
        // Utility class -- not instantiable
    }

    public static MetricSnapshotJpaEntity toJpaEntity(MetricSnapshot snapshot) {
        return new MetricSnapshotJpaEntity(
                UUID.randomUUID().toString(),
                snapshot.assetId().toString(),
                snapshot.cpuUsage(),
                snapshot.memoryUsage(),
                snapshot.diskUsage(),
                snapshot.collectedAt()
        );
    }

    public static MetricSnapshot fromJpaEntity(MetricSnapshotJpaEntity jpaEntity) {
        return MetricSnapshot.reconstruct(
                AssetId.of(jpaEntity.getAssetId()),
                jpaEntity.getCpuUsage(),
                jpaEntity.getMemoryUsage(),
                jpaEntity.getDiskUsage(),
                jpaEntity.getCollectedAt()
        );
    }
}
