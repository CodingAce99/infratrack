package com.infratrack.application.service;

import com.infratrack.application.port.input.ManageAssetUseCase;
import com.infratrack.application.port.output.AssetRepository;
import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.AssetStatus;
import com.infratrack.domain.model.AssetType;
import com.infratrack.domain.model.Credentials;
import com.infratrack.domain.model.IpAddress;

import java.util.List;
import java.util.Objects;

public class AssetService implements ManageAssetUseCase {

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = Objects.requireNonNull(assetRepository, "AssetRepository cannot be null");
    }

    @Override
    public Asset createAsset(String name, AssetType type, IpAddress ipAddress, Credentials credentials) {
        Asset asset = Asset.create(name, type, ipAddress, credentials);
        assetRepository.save(asset);
        return asset;
    }

    @Override
    public Asset findAsset(AssetId id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Asset not found with id: " + id));
    }

    @Override
    public List<Asset> findAllAssets() {
        return assetRepository.findAll();
    }

    @Override
    public Asset updateAssetStatus(AssetId id, AssetStatus newStatus) {
        Asset asset = findAsset(id);
        switch (newStatus) {
            case ACTIVE      -> asset.activate();
            case INACTIVE    -> asset.deactivate();
            case MAINTENANCE -> asset.putInMaintenance();
        }
        assetRepository.save(asset);
        return asset;
    }

    @Override
    public Asset updateAssetCredentials(AssetId id, Credentials newCredentials) {
        Asset asset = findAsset(id);
        asset.updateCredentials(newCredentials);
        assetRepository.save(asset);
        return asset;
    }

    @Override
    public Asset updateAssetIpAddress(AssetId id, IpAddress newIpAddress) {
        Asset asset = findAsset(id);
        asset.updateIpAddress(newIpAddress);
        assetRepository.save(asset);
        return asset;
    }

    @Override
    public void deleteAsset(AssetId id) {
        findAsset(id);
        assetRepository.delete(id);
    }
}

