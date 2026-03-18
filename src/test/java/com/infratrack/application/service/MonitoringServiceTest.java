package com.infratrack.application.service;

import com.infratrack.application.port.output.AssetRepository;
import com.infratrack.application.port.output.MetricSnapshotRepository;
import com.infratrack.application.port.output.MetricsCollector;
import com.infratrack.domain.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("MonitoringService — application service")
@ExtendWith(MockitoExtension.class)
public class MonitoringServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private MetricsCollector metricsCollector;

    @Mock
    private MetricSnapshotRepository metricSnapshotRepository;

    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        monitoringService = new MonitoringService(assetRepository, metricsCollector, metricSnapshotRepository);
    }

    private Asset sampleAsset() {
        return Asset.create(
                "Core Router",
                AssetType.ROUTER,
                IpAddress.of("192.168.1.1"),
                Credentials.of("admin", "s3cr3t")
        );
    }

    @Nested
    @DisplayName("collectNow()")
    class CollectNowTests {

        @Test
        @DisplayName("should create and save a snapshot when asset exists")
        void collectNow_shouldCreateAndSaveSnapshot_whenAssetExists() {
            Asset asset = sampleAsset();

            when(assetRepository.findById(asset.getId())).thenReturn(java.util.Optional.of(asset));
            MetricSnapshot expectedSnapshot = MetricSnapshot.of(asset.getId(), 50.0, 70.0, 60.0);

            when(metricsCollector.collect(asset)).thenReturn(expectedSnapshot);
            MetricSnapshot result = monitoringService.collectNow(asset.getId());

            assertEquals(expectedSnapshot, result);

            verify(metricSnapshotRepository, times(1)).save(expectedSnapshot);
            verify(metricsCollector, times(1)).collect(asset);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when asset does not exist")
        void collectNow_shouldThrowIllegalArgumentException_whenAssetDoesNotExist() {
            AssetId unknownId = AssetId.generate();

            when(assetRepository.findById(unknownId)).thenReturn(java.util.Optional.empty());
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> monitoringService.collectNow(unknownId)
            );

            assertTrue(ex.getMessage().contains(unknownId.toString()),
                    "error message should contain the searched id");
        }
    }

    @Nested
    @DisplayName("getHistory()")
    class GetHistoryTests {

        @Test
        @DisplayName("should return list of snapshots when asset exists")
        void getHistory_shouldSearchAndReturnListOfSnapshots_whenAssetExists() {
            Asset asset = sampleAsset();

            ArrayList<MetricSnapshot> expectedSnapshots = new ArrayList<>();
            expectedSnapshots.add(MetricSnapshot.of(asset.getId(), 50.0, 70.0, 60));
            expectedSnapshots.add(MetricSnapshot.of(asset.getId(), 55.0, 72.0, 65));
            expectedSnapshots.add(MetricSnapshot.of(asset.getId(), 49.0, 71.0, 63));
            expectedSnapshots.add(MetricSnapshot.of(asset.getId(), 45.0, 65.0, 58));
            expectedSnapshots.add(MetricSnapshot.of(asset.getId(), 62.0, 75.0, 70));

            when(metricSnapshotRepository.findLatestByAssetId(asset.getId(), 5)).thenReturn(expectedSnapshots);
            List<MetricSnapshot> result = monitoringService.getHistory(asset.getId(), 5);

            assertEquals(expectedSnapshots, result);
            verify(metricSnapshotRepository, times(1)).findLatestByAssetId(asset.getId(), 5);
        }
    }

    @Nested
    @DisplayName("Collect all active assets")
    class CollectAllActiveTests {

        @Test
        @DisplayName("should collect metrics only for ACTIVE assets")
        void collectAllActive_shouldCollectMetrics_whenAssetStateIsActive() throws InterruptedException {
            Asset active = sampleAsset(); // ACTIVE status by default
            Asset inactive = sampleAsset();
            inactive.deactivate(); // Force INACTIVE

            when(assetRepository.findAll()).thenReturn(List.of(active, inactive));
            when(assetRepository.findById(active.getId())).thenReturn(Optional.of(active));
            when(metricsCollector.collect(active))
                    .thenReturn(MetricSnapshot.of(active.getId(), 50.0, 60.0, 70));

            monitoringService.collectAllActive();
            Thread.sleep(500);

            verify(metricsCollector, times(1)).collect(active);
            verify(metricsCollector, never()).collect(inactive);
        }

        @Test
        @DisplayName("should continue collecting remaining assets when one fails")
        void collectAllActive_shouldContinueCollectingRemaining_whenOneFails() throws InterruptedException {
            Asset asset1 = sampleAsset();
            Asset asset2 = sampleAsset();

            when(assetRepository.findAll()).thenReturn(List.of(asset1, asset2));
            when(assetRepository.findById(asset2.getId())).thenReturn(Optional.of(asset2));
            when(metricsCollector.collect(asset2)).thenReturn(MetricSnapshot.of(asset2.getId(), 50.0, 70.0, 60));

            monitoringService.collectAllActive();
            Thread.sleep(500);

            verify(metricsCollector, never()).collect(asset1);
            verify(metricsCollector, times(1)).collect(asset2);
        }
    }
}
