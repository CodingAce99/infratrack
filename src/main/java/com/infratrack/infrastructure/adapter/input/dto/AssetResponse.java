package com.infratrack.infrastructure.adapter.input.dto;

import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetStatus;
import com.infratrack.domain.model.AssetType;

public record AssetResponse(
        String id,
        String name,
        AssetType type,
        String ipAddress,
        AssetStatus status,
        String username
) {
    public static AssetResponse from(Asset asset) {
        return new AssetResponse(
                asset.getId().toString(),
                asset.getName(),
                asset.getType(),
                asset.getIpAddress().getValue(),
                asset.getStatus(),
                asset.getCredentials().getUsername()
        );
    }
}
