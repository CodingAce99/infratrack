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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoringService — application service")
public class MonitoringServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private MetricsCollector metricsCollector;

    @Mock
    private MetricSnapshotRepository metricSnapshotRepository;

    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {monitoringService = new MonitoringService(assetRepository, metricsCollector, metricSnapshotRepository);}

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

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
        @DisplayName("debe crear y guardar un snapshot de un asset existente")
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
        @DisplayName("debe lanzar IllegalArgumentException cuando el asset no exista")
        void collectNow_shouldThrowIllegalArgumentException_whenAssetDoesNotExist() {
            AssetId unknownId = AssetId.generate();
            when(assetRepository.findById(unknownId)).thenReturn(java.util.Optional.empty());
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> monitoringService.collectNow(unknownId)
            );

            assertTrue(ex.getMessage().contains(unknownId.toString()),
                    "El mensaje de error debe contener el id buscado");
        }
    }

    @Nested
    @DisplayName("getHistory()")
    class GetHistoryTests {

        @Test
        @DisplayName("debe buscar y devolver una lista con el historial de snapshots de un asset existente")
        void getHistory_shouldSearchAndReturnListOfSnapshots_whenAssetExists() {
            Asset asset = sampleAsset();

            ArrayList<MetricSnapshot> expectedSnapshots = new ArrayList<>();
            expectedSnapshots.add(MetricSnapshot.of(asset.getId(), 50.0, 70.0, 60));
            expectedSnapshots.add(MetricSnapshot.of(asset.getId(), 55.0, 72.0, 65));
            expectedSnapshots.add(MetricSnapshot.of(asset.getId(), 49.0, 71.0, 63));
            expectedSnapshots.add(MetricSnapshot.of(asset.getId(), 45.0, 65.0, 58));
            expectedSnapshots.add(MetricSnapshot.of(asset.getId(), 62.0, 75.0, 70));
            when(metricSnapshotRepository.findLatestByAssetId(asset.getId(),5)).thenReturn(expectedSnapshots);
            List<MetricSnapshot> result = monitoringService.getHistory(asset.getId(), 5);
            assertEquals(expectedSnapshots, result);
            verify(metricSnapshotRepository, times(1)).findLatestByAssetId(asset.getId(), 5);
        }
    }
}
