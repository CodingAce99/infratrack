package com.infratrack.infrastructure.adapter.input.dto;

import com.infratrack.domain.model.*;

public class AssetDtoMapper {

    private AssetDtoMapper() {
    }

    public static Asset toDomain(CreateAssetRequest request) {
        return Asset.create(
                request.name(),
                AssetType.valueOf(request.type()),
                IpAddress.of(request.ipAddress()),
                Credentials.of(request.username(), request.password())
        );
    }

    public static AssetStatus toStatus(UpdateStatusRequest request) {
        return AssetStatus.valueOf(request.status());
    }

    public static Credentials toCredentials(UpdateCredentialsRequest request) {
        return Credentials.of(request.username(), request.password());
    }

    public static IpAddress toIpAddress(UpdateIpAddressRequest request) {
        return IpAddress.of(request.ipAddress());
    }

    public static AssetResponse toResponse(Asset asset) {
        return AssetResponse.from(asset);
    }
}
