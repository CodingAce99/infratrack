package com.infratrack.infrastructure.adapter.input;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infratrack.application.port.input.ManageAssetUseCase;
import com.infratrack.domain.model.*;
import com.infratrack.infrastructure.adapter.input.dto.CreateAssetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// -------------------------------------------------------------------------
// AssetRestController test using WebMvcTest to simulate HTTP requests
// -------------------------------------------------------------------------

@DisplayName("AssetRestController")
@WebMvcTest(AssetRestController.class)
public class AssetRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ManageAssetUseCase useCase;

    private Asset testAsset;


    @BeforeEach
        // runs before every test to start the reference asset
    void setUp() {
        testAsset = Asset.reconstitute(
                AssetId.of("123e4567-e89b-12d3-a456-426614174000"),
                "Test Asset",
                AssetType.SERVER,
                IpAddress.of("192.168.1.1"),
                AssetStatus.ACTIVE,
                Credentials.of("admin", "password")
        );
    }

    @Nested
    @DisplayName("GET /api/v1/assets/{id}")
    class FindAsset {

        @Test
        @DisplayName("returns 200 and AssetResponse when asset exists")
        void returnsAsset() throws Exception {

            when(useCase.findAsset(any())).thenReturn(testAsset);

            mockMvc.perform(get("/api/v1/assets/{id}", "123e4567-e89b-12d3-a456-426614174000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Test Asset"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("returns 404 when asset is not found")
        void findAsset_returns404_whenAssetIsNotFound() throws Exception {
            when(useCase.findAsset(any()))
                    .thenThrow(new AssetNotFoundException(AssetId.of("00000000-0000-0000-0000-000000000000")));

            mockMvc.perform(get("/api/v1/assets/{id}", "00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/assets")
    class CreateAsset {
        @Test
        @DisplayName("returns 201 and AssetResponse when asset is created")
        void createsAsset() throws Exception {

            when(useCase.createAsset(any())).thenReturn(testAsset);

            CreateAssetRequest request = new CreateAssetRequest(
                    "Test Asset",
                    "SERVER",
                    "192.168.1.1",
                    "admin",
                    "password"
            );
            mockMvc.perform(post("/api/v1/assets")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Test Asset"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("returns 409 when IP address already exists")
        void createAsset_returns409_whenIpIsDuplicate() throws Exception {
            when(useCase.createAsset(any()))
                    .thenThrow(new DuplicateIpAddressException(IpAddress.of("web-server-01")));

            mockMvc.perform(post("/api/v1/assets")
                            .contentType("application/json")
                            .content("""
                                    {"name":"web-server-01","type":"SERVER","ipAddress":"web-server-01",
                                      "username":"sshuser","password":"sshpass"}
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").exists());
        }
    }
}

