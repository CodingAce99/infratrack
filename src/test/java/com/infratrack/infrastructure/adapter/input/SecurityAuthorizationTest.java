package com.infratrack.infrastructure.adapter.input;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infratrack.application.port.input.AuthenticateUserUseCase;
import com.infratrack.application.port.input.AuthenticationResult;
import com.infratrack.application.port.input.ManageAssetUseCase;
import com.infratrack.application.port.input.MonitorAssetUseCase;
import com.infratrack.application.port.output.TokenClaims;
import com.infratrack.application.port.output.TokenValidator;
import com.infratrack.domain.exception.InvalidTokenException;
import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetStatus;
import com.infratrack.domain.model.AssetType;
import com.infratrack.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Security Authorization")
@WebMvcTest({AssetRestController.class, MetricsRestController.class, AuthRestController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("demo")
class SecurityAuthorizationTest {

    private static final String BEARER = "Bearer test-token";
    private static final String INVALID_BEARER = "Bearer invalid-token";
    // AssetId.of() validates UUID format — a plain slug like "some-id" would throw.
    private static final String ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TokenValidator tokenValidator;

    @MockitoBean
    private ManageAssetUseCase manageAssetUseCase;

    @MockitoBean
    private MonitorAssetUseCase monitorAssetUseCase;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    // Builds an Asset mock that satisfies AssetResponse.from():
    // getId(), getName(), getType(), getIpAddress().getValue(),
    // getStatus(), getCredentials().getUsername().
    // Must be called BEFORE any surrounding when() to avoid UnfinishedStubbing.
    private Asset stubAsset() {
        Asset asset = mock(Asset.class, RETURNS_DEEP_STUBS);
        when(asset.getId().toString()).thenReturn(ID);
        when(asset.getName()).thenReturn("Test Server");
        when(asset.getType()).thenReturn(AssetType.SERVER);
        when(asset.getIpAddress().getValue()).thenReturn("10.0.0.1");
        when(asset.getStatus()).thenReturn(AssetStatus.ACTIVE);
        when(asset.getCredentials().getUsername()).thenReturn("admin");
        return asset;
    }

    // -------------------------------------------------------------------------
    // T-07 to T-10 — No token
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("No token — all protected endpoints reject with 401 JSON")
    class NoToken {

        @Test
        @DisplayName("T-07: GET /assets → 401 + {\"error\":\"...\"}")
        void get_assets_no_token_401() throws Exception {
            mockMvc.perform(get("/api/v1/assets"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("T-08: POST /assets → 401 JSON")
        void post_assets_no_token_401() throws Exception {
            mockMvc.perform(post("/api/v1/assets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("T-09: DELETE /assets/{id} → 401 JSON")
        void delete_asset_no_token_401() throws Exception {
            mockMvc.perform(delete("/api/v1/assets/" + ID))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("T-10: GET /assets/{id}/metrics → 401 JSON")
        void get_metrics_no_token_401() throws Exception {
            mockMvc.perform(get("/api/v1/assets/" + ID + "/metrics"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    // -------------------------------------------------------------------------
    // T-11 — Invalid token
    // The validator mock throws InvalidTokenException; the filter does not authenticate
    // and the downstream security rejects with 401. One case suffices at this level:
    // the filter only cares that the exception was thrown, not why.
    // tampered vs. expired is tested where the validator is real: JjwtTokenValidatorTest T-03/T-04.
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Invalid token — uniform 401 regardless of failure reason")
    class InvalidToken {

        @BeforeEach
        void setup() {
            when(tokenValidator.validate(any())).thenThrow(new InvalidTokenException());
        }

        @Test
        @DisplayName("T-11: validator throws InvalidTokenException → 401 JSON (uniform with no-token)")
        void invalid_token_401() throws Exception {
            mockMvc.perform(get("/api/v1/assets")
                            .header("Authorization", INVALID_BEARER))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    // -------------------------------------------------------------------------
    // T-14 to T-20 — VIEWER role
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("VIEWER role — reads allowed, writes forbidden")
    class ViewerRole {

        @BeforeEach
        void setup() {
            when(tokenValidator.validate(any())).thenReturn(new TokenClaims("viewer", "VIEWER"));
        }

        @Test
        @DisplayName("T-14: GET /assets with VIEWER token → 200")
        void get_assets_viewer_200() throws Exception {
            when(manageAssetUseCase.findAllAssets()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/assets")
                            .header("Authorization", BEARER))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T-15: GET /assets/{id}/metrics/history with VIEWER token → 200")
        // /metrics/history always returns 200 with an empty list, avoiding a MetricSnapshot stub.
        // The security rule tested is identical to /metrics (both match GET /api/v1/assets/**).
        void get_metrics_history_viewer_200() throws Exception {
            when(monitorAssetUseCase.getHistory(any(), anyInt())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/assets/" + ID + "/metrics/history")
                            .header("Authorization", BEARER))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T-16: POST /assets with VIEWER token → 403 + {\"error\":\"...\"}")
        void post_assets_viewer_403() throws Exception {
            mockMvc.perform(post("/api/v1/assets")
                            .header("Authorization", BEARER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("T-17: PUT /assets/{id}/status with VIEWER token → 403 JSON")
        void put_status_viewer_403() throws Exception {
            mockMvc.perform(put("/api/v1/assets/" + ID + "/status")
                            .header("Authorization", BEARER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("T-18: PUT /assets/{id}/ip with VIEWER token → 403 JSON")
        void put_ip_viewer_403() throws Exception {
            mockMvc.perform(put("/api/v1/assets/" + ID + "/ip")
                            .header("Authorization", BEARER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("T-19: PUT /assets/{id}/credentials with VIEWER token → 403 JSON")
        void put_credentials_viewer_403() throws Exception {
            mockMvc.perform(put("/api/v1/assets/" + ID + "/credentials")
                            .header("Authorization", BEARER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("T-20: DELETE /assets/{id} with VIEWER token → 403 JSON")
        void delete_asset_viewer_403() throws Exception {
            mockMvc.perform(delete("/api/v1/assets/" + ID)
                            .header("Authorization", BEARER))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    // -------------------------------------------------------------------------
    // T-21 to T-24 — ADMIN role
    // T-22 is the key ROLE_ prefix check: if "ROLE_ADMIN" were missing from the
    // authority, this request would 403 despite a valid ADMIN token.
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ADMIN role — reads and writes both allowed")
    class AdminRole {

        @BeforeEach
        void setup() {
            when(tokenValidator.validate(any())).thenReturn(new TokenClaims("admin", "ADMIN"));
        }

        @Test
        @DisplayName("T-21: GET /assets with ADMIN token → 200")
        void get_assets_admin_200() throws Exception {
            when(manageAssetUseCase.findAllAssets()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/assets")
                            .header("Authorization", BEARER))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T-22: POST /assets with ADMIN token → 201 (ROLE_ADMIN prefix check)")
        void post_assets_admin_201() throws Exception {
            // thenAnswer returns the same Asset the controller built from the request body —
            // a real Asset object, so AssetResponse.from() has all fields populated.
            when(manageAssetUseCase.createAsset(any())).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/assets")
                            .header("Authorization", BEARER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Test Server",
                                      "type": "SERVER",
                                      "ipAddress": "10.0.0.1",
                                      "username": "admin",
                                      "password": "secret"
                                    }
                                    """))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("T-23: PUT /assets/{id}/status with ADMIN token → 200")
        void put_status_admin_200() throws Exception {
            // stubAsset() must be called BEFORE when() to avoid UnfinishedStubbing:
            // RETURNS_DEEP_STUBS opens its own when() chains internally.
            Asset stub = stubAsset();
            when(manageAssetUseCase.updateAssetStatus(any(), any())).thenReturn(stub);

            mockMvc.perform(put("/api/v1/assets/" + ID + "/status")
                            .header("Authorization", BEARER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"ACTIVE\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T-24: DELETE /assets/{id} with ADMIN token → 204")
        void delete_asset_admin_204() throws Exception {
            // deleteAsset() is void — Mockito does nothing by default, which is correct.
            mockMvc.perform(delete("/api/v1/assets/" + ID)
                            .header("Authorization", BEARER))
                    .andExpect(status().isNoContent());
        }
    }

    // -------------------------------------------------------------------------
    // T-25 — Public endpoint
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Public endpoints — no token required")
    class PublicEndpoints {

        @Test
        @DisplayName("T-25: POST /auth/login without token → 200 (login is always public)")
        void login_no_token_200() throws Exception {
            when(authenticateUserUseCase.login(any()))
                    .thenReturn(new AuthenticationResult("token", "admin", "ADMIN"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"admin\", \"password\": \"admin\"}"))
                    .andExpect(status().isOk());
        }
    }
}
