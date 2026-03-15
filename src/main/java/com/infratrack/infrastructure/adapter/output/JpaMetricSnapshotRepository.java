package com.infratrack.infrastructure.adapter.output;

import com.infratrack.application.port.output.MetricSnapshotRepository;
import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.MetricSnapshot;
import com.infratrack.infrastructure.persistence.MetricSnapshotMapper;
import com.infratrack.infrastructure.persistence.SpringDataMetricSnapshotRepository;
import org.springframework.data.domain.Limit;

import java.util.List;
import java.util.Objects;

public class JpaMetricSnapshotRepository implements MetricSnapshotRepository {

    private final SpringDataMetricSnapshotRepository snapshotRepository;

    public JpaMetricSnapshotRepository(SpringDataMetricSnapshotRepository snapshotRepository) {
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository, "snapshotRepository cannot be null");
    }

    @Override
    public void save(MetricSnapshot snapshot) {
        snapshotRepository.save(MetricSnapshotMapper.toJpaEntity(snapshot));
    }

    @Override
    public List<MetricSnapshot> findLatestByAssetId(AssetId assetId, int numberOfSnapshots) {
        return snapshotRepository.findByAssetIdOrderByCollectedAtDesc(assetId.toString(), Limit.of(numberOfSnapshots))
                .stream().map(MetricSnapshotMapper::fromJpaEntity).toList();
    }
}
