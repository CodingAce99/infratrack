package com.infratrack.infrastructure.adapter.input;

import com.infratrack.application.port.input.MonitorAssetUseCase;
import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.MetricSnapshot;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MetricsRestController")
@WebMvcTest(MetricsRestController.class)
public class MetricsRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitorAssetUseCase monitorUseCase;

    private MetricSnapshot testMetricSnapshot;

    private static final String ASSET_ID = "123e4567-e89b-12d3-a456-426614174000";

    private static final Instant FIXED_TIME = Instant.parse("2026-01-15T10:30:00Z");

    @BeforeEach
    void setUp() {
        testMetricSnapshot = MetricSnapshot.reconstruct(
                AssetId.of(ASSET_ID),
                50.0, 60.0, 70.0,
                FIXED_TIME
        );
    }

    @Nested
    @DisplayName("GET /{id}/metrics")
    class GetMetrics {

        @Test
        @DisplayName("returns 200 with snapshot when data exists")
        void getMetrics_shouldReturn200WithSnapshot_whenDataExists() throws Exception {

            when(monitorUseCase.getHistory(testMetricSnapshot.assetId(), 1))
                    .thenReturn(List.of(testMetricSnapshot));

            mockMvc.perform(get("/api/v1/assets/{id}/metrics", ASSET_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assetId").value(ASSET_ID))
                    .andExpect(jsonPath("$.cpuUsage").value(50.0))
                    .andExpect(jsonPath("$.memoryUsage").value(60.0))
                    .andExpect(jsonPath("$.diskUsage").value(70.0));
        }

        @Test
        @DisplayName("returns 404 when no snapshot exist")
        void getMetrics_shouldReturn404_whenNoSnapshotExist() throws Exception {

            when(monitorUseCase.getHistory(testMetricSnapshot.assetId(), 1))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/assets/{id}/metrics", ASSET_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /{id}/metrics/history")
    class GetMetricsHistory {

        @Test
        @DisplayName("returns 200 with list of snapshots")
        void getMetricsHistory_shouldReturn200WithListOfSnapshots() throws Exception {

            when(monitorUseCase.getHistory(testMetricSnapshot.assetId(), 20))
                    .thenReturn(List.of(testMetricSnapshot, testMetricSnapshot));

            mockMvc.perform(get("/api/v1/assets/{id}/metrics/history", ASSET_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));

        }

        @Test
        @DisplayName("returns 200 with empty when no snapshot exist")
        void getMetricsHistory_shouldReturn200WithEmptyList_whenNoSnapshotExist() throws Exception {

            when(monitorUseCase.getHistory(testMetricSnapshot.assetId(), 20))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/assets/{id}/metrics/history", ASSET_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("respects limit query parameter")
        void getMetricsHistory_shouldRespectLimitQueryParameter() throws Exception {

            when(monitorUseCase.getHistory(testMetricSnapshot.assetId(), 2))
                    .thenReturn(List.of(testMetricSnapshot, testMetricSnapshot));

            mockMvc.perform(get("/api/v1/assets/{id}/metrics/history", ASSET_ID)
                            .param("limit", "2"))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }
}
