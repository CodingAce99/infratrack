package com.infratrack.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable snapshot of resource metrics collected from an asset at a point in time.
 *
 * This is a Value Object — its identity is defined by its content, not by a
 * surrogate key. Two snapshots are equal if all fields are equal.
 *
 * Usage values are percentages: 0.0 (idle) to 100.0 (saturated).
 */
public record MetricSnapshot(
        AssetId assetId,
        double cpuUsage,
        double memoryUsage,
        double diskUsage,
        Instant collectedAt
) {

    //Compact constructor - validation without repeating field assignments
    public MetricSnapshot {
        Objects.requireNonNull(assetId, "assetId cannot be null");
        Objects.requireNonNull(collectedAt, "collectedAt cannot be null");
        validateUsage(cpuUsage, "cpuUsage");
        validateUsage(memoryUsage, "memoryUsage");
        validateUsage(diskUsage, "diskUsage");
    }

    /**
     * Factory method — the intended entry point.
     * Captures the collection timestamp automatically.
     */
    public static MetricSnapshot of(
            AssetId assetId,
            double cpuUsage,
            double memoryUsage,
            double diskUsage
    ) {
        return new MetricSnapshot(assetId, cpuUsage, memoryUsage, diskUsage, Instant.now());
    }


    /**
     * Factory method for reconstruction from persistence.
     * The timestamp comes from the database, not from now().
     */

    public static MetricSnapshot reconstruct(
            AssetId assetId,
            double cpuUsage,
            double memoryUsage,
            double diskUsage,
            Instant collectedAt
    ) {
        return new MetricSnapshot(assetId, cpuUsage, memoryUsage, diskUsage, collectedAt);
    }

    private static void validateUsage(double value, String fieldName) {
        if (value < 0.0 || value > 100.0) {
            throw new IllegalArgumentException(
                    fieldName + " must be between 0.0 and 100.0, got: " + value
            );
        }
    }
}
