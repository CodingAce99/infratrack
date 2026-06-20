package com.infratrack.infrastructure.config;

import com.infratrack.infrastructure.adapter.input.MetricsScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Actuator Endpoints")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "infratrack.encryption.key=yt1+CDm1+7KdsybbxWrFcunLl8hnMTROPrEdi2daEuc=",
        "management.prometheus.metrics.export.enabled=true"
})
class ActuatorEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MetricsScheduler metricsScheduler;

    @Test
    @DisplayName("GET /actuator/health returns 200 UP without authentication")
    void healthReturnsUpWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/health is accessible without auth header")
    void healthIsPublicEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /actuator/prometheus returns 200 with Prometheus text exposition format")
    void prometheusReturns200WithTextFormat() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("<html"))));
    }

    @Test
    @DisplayName("GET /actuator/prometheus is accessible without authentication")
    void prometheusIsPublicEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    // ── Unsafe endpoint rejection ──────────────────────────────────

    @Test
    @DisplayName("GET /actuator/env returns 404 — not in the exposure include list")
    void envReturns404() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /actuator/configprops returns 404 — not in the exposure include list")
    void configpropsReturns404() throws Exception {
        mockMvc.perform(get("/actuator/configprops"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /actuator/info returns 200 — included alongside health and prometheus")
    void infoReturns200() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    // ── Custom sweep metric visible through Prometheus endpoint ────

    @Test
    @DisplayName("Prometheus endpoint includes infratrack_monitoring_collection_duration after sweep")
    void prometheusExposesCustomSweepMetricAfterCollectAll() throws Exception {
        metricsScheduler.collectAll();

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string(containsString(
                        "infratrack_monitoring_collection_duration")));
    }
}
