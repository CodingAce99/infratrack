package com.infratrack.infrastructure.adapter.input.dto;


import com.infratrack.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("AssetResponse")
public class AssetResponseTest {

    @Nested
    @DisplayName("from(Asset)")
    class From {

        @Test
        @DisplayName("maps all fields correctly from domain Asset")
        void mapsAllFields() {
            Asset asset = Asset.reconstitute(
                    AssetId.of("123e4567-e89b-12d3-a456-426614174000"),
                    "server-01",
                    AssetType.SERVER,
                    IpAddress.of("192.168.1.1"),
                    AssetStatus.ACTIVE,
                    Credentials.of("admin", "s3cr3t")
            );

            AssetResponse assetResponse = AssetResponse.from(asset);
            assertThat(assetResponse.id()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
            assertThat(assetResponse.name()).isEqualTo("server-01");
            assertThat(assetResponse.type()).isEqualTo(AssetType.SERVER);
            assertThat(assetResponse.ipAddress()).isEqualTo("192.168.1.1");
            assertThat(assetResponse.status()).isEqualTo(AssetStatus.ACTIVE);
            assertThat(assetResponse.username()).isEqualTo("admin");
        }

        @Test
        @DisplayName("never exposes password in the mapped AssetResponse")
        void passwordIsNotExposed() {
            Asset asset = Asset.reconstitute(
                    AssetId.of("123e4567-e89b-12d3-a456-426614174000"),
                    "server-01",
                    AssetType.SERVER,
                    IpAddress.of("192.168.1.1"),
                    AssetStatus.ACTIVE,
                    Credentials.of("admin", "s3cr3t")
            );

            AssetResponse assetResponse = AssetResponse.from(asset);
            String assetResponseAsString = assetResponse.toString();

            assertThat(assetResponseAsString).doesNotContain("s3cr3t");
        }
    }
}
