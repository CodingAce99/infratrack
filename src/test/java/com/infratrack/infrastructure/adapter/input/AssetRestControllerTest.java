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
// Tests de AssetRestController usando WebMvcTest para simular peticiones HTTP
// -------------------------------------------------------------------------

@DisplayName("AssetRestController")
@WebMvcTest(AssetRestController.class)
public class AssetRestControllerTest {

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private ManageAssetUseCase useCase;

        private Asset testAsset;


        @BeforeEach // se ejecuta antes de cada test para inicializar el Asset de referencia
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

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

        @Nested
        @DisplayName("GET /api/v1/assets/{id}")
        class FindAsset {

            @Test
            @DisplayName("returns 200 and AssetResponse when asset exists")
            void returnsAsset() throws Exception {
                // Given
                when(useCase.findAsset(any())).thenReturn(testAsset);

                // When + Then
                mockMvc.perform(get("/api/v1/assets/{id}", "123e4567-e89b-12d3-a456-426614174000"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.name").value("Test Asset"))
                        .andExpect(jsonPath("$.password").doesNotExist());
            }
        }

        @Nested
        @DisplayName("POST /api/v1/assets")
        class CreateAsset {
            // tests del POST aquí
            @Test
            @DisplayName("returns 201 and AssetResponse when asset is created")
            void createsAsset() throws Exception {
                // Given
                when(useCase.createAsset(any())).thenReturn(testAsset);

                // When + Then
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
        }
}
