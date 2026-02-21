package com.infratrack.infrastructure.persistence;

import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.AssetStatus;
import com.infratrack.domain.model.AssetType;
import com.infratrack.domain.model.Credentials;
import com.infratrack.domain.model.IpAddress;

public final class AssetMapper {

    private AssetMapper() {
        // Utility class — not instantiable
    }

    public static AssetJpaEntity toJpaEntity(Asset asset) {
        return new AssetJpaEntity(
                asset.getId().toString(),
                asset.getName(),
                asset.getType().name(),
                asset.getIpAddress().getValue(),
                asset.getStatus().name(),
                asset.getCredentials().getUsername(),
                asset.getCredentials().getPassword()
        );
    }

    public static Asset toDomain(AssetJpaEntity entity) {
        return Asset.reconstitute(
                AssetId.of(entity.getId()),
                entity.getName(),
                AssetType.valueOf(entity.getType()),
                IpAddress.of(entity.getIpAddress()),
                AssetStatus.valueOf(entity.getStatus()),
                Credentials.of(entity.getUsername(), entity.getPassword())
        );
    }
}

