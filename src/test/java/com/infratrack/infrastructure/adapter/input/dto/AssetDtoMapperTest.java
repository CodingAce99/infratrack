package com.infratrack.infrastructure.adapter.input.dto;

import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("AssetDtoMapper")
public class AssetDtoMapperTest {

    @Nested
    @DisplayName("toDomain(CreateAssetRequest)")
    class ToDomain {

        @Test
        @DisplayName("maps all fields correctly to domain Asset")
        void mapsAllFields() {
            CreateAssetRequest request = new CreateAssetRequest(
                    "router-01",
                    "ROUTER",
                    "192.168.1.1",
                    "admin",
                    "s3cr3t"
            );

            Asset asset = AssetDtoMapper.toDomain(request);

            assertThat(asset.getName()).isEqualTo("router-01");
            assertThat(asset.getType()).isEqualTo(AssetType.ROUTER);
            assertThat(asset.getIpAddress().getValue()).isEqualTo("192.168.1.1");
            assertThat(asset.getCredentials().getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("never exposes password in the mapped Asset credentials")
        void passwordIsNotExposed() {
            CreateAssetRequest request = new CreateAssetRequest(
                    "server-01",
                    "SERVER",
                    "10.0.0.1",
                    "root",
                    "sup3rs3cr3t"
            );

            Asset asset = AssetDtoMapper.toDomain(request);
            String assetAsString = asset.toString();

            assertThat(assetAsString).doesNotContain("sup3rs3cr3t");
        }
    }
}
