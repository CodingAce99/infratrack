package com.infratrack.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("MetricSnapshot — domain value object")
public class MetricSnapshotTest {

    private final AssetId sampleId = AssetId.generate();

    @Nested
    @DisplayName("factory method of()")
    class OfTests {

        @Test
        @DisplayName("crea snapshot con valores válidos y timestamp automático")
        void of_shouldCreateSnapshotWithAutoTimestamp() {
            Instant before = Instant.now();
            MetricSnapshot snapshot = MetricSnapshot.of(sampleId, 50.0, 60.0, 70.0);
            Instant after = Instant.now();

            assertEquals(sampleId, snapshot.assetId());
            assertEquals(50.0, snapshot.cpuUsage());
            assertEquals(60.0, snapshot.memoryUsage());
            assertEquals(70.0, snapshot.diskUsage());
            assertFalse(snapshot.collectedAt().isBefore(before));
            assertFalse(snapshot.collectedAt().isAfter(after));
        }

        @Test
        @DisplayName("lanza NullPointerException si assetId es null")
        void of_shouldThrowNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> MetricSnapshot.of(null, 50.0, 60.0, 70));
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si cpuUsage supera 100")
        void of_shouldThrowIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> MetricSnapshot.of(sampleId, 101.0, 60.0, 70));
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si memoryUsage es Negativo")
        void of_shouldThrow_whenMemoryUsageIsNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> MetricSnapshot.of(sampleId, 50.0, -1.0, 70.0));
        }
    }

    @Nested
    @DisplayName("factory method reconstruct()")
    class ReconstructTests {

        @Test
        @DisplayName("preserva el timestamp recibido sin modificarlo")
        void reconstruct_shouldPreserveTimestamp() {
            Instant fixedTime = Instant.parse("2024-01-15T10:30:00Z");
            MetricSnapshot snapshot = MetricSnapshot.reconstruct(
                    sampleId, 40.0, 55.0, 65.0, fixedTime);

            assertEquals(fixedTime, snapshot.collectedAt());
        }

        @Test
        @DisplayName("lanza NullPointerException si collectedAt es null")
        void reconstruct_shouldThrow_whenCollectedAtIsNull() {
            assertThrows(NullPointerException.class,
                    () -> MetricSnapshot.reconstruct(sampleId, 40.0, 55.0, 65.0, null));
        }
    }
}
