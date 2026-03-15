package com.infratrack.infrastructure.persistence;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataMetricSnapshotRepository extends JpaRepository<MetricSnapshotJpaEntity, String> {

    List<MetricSnapshotJpaEntity> findByAssetIdOrderByCollectedAtDesc(String assetId, Limit limit);
}
