package com.infratrack.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "metrics")
public class MetricSnapshotJpaEntity {

    @Id
    private String id;

    @Column(name = "asset_id", nullable = false)
    private String assetId;

    @Column(name = "cpu_usage", nullable = false)
    private double cpuUsage;

    @Column(name = "memory_usage", nullable = false)
    private double memoryUsage;

    @Column(name = "disk_usage", nullable = false)
    private double diskUsage;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    protected MetricSnapshotJpaEntity() {}

    public MetricSnapshotJpaEntity(
            String id,
            String assetId,
            double cpuUsage,
            double memoryUsage,
            double diskUsage,
            Instant collectedAt
    ) {
        this.id = id;
        this.assetId = assetId;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.diskUsage = diskUsage;
        this.collectedAt = collectedAt;
    }

    public String getId() { return id; }
    public String getAssetId() { return assetId; }
    public double getCpuUsage() { return cpuUsage; }
    public double getMemoryUsage() { return memoryUsage; }
    public double getDiskUsage() { return diskUsage; }
    public Instant getCollectedAt() { return collectedAt; }

}
