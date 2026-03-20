package com.infratrack.application.service;

import com.infratrack.application.port.input.ManageAssetUseCase;
import com.infratrack.application.port.output.AssetRepository;
import com.infratrack.application.port.output.DomainEventPublisher;
import com.infratrack.domain.event.AssetCreatedEvent;
import com.infratrack.domain.event.AssetDeletedEvent;
import com.infratrack.domain.event.AssetStatusChangedEvent;
import com.infratrack.domain.model.*;

import java.util.List;
import java.util.Objects;

public class AssetService implements ManageAssetUseCase {

    private final AssetRepository assetRepository;
    private final DomainEventPublisher publisher;

    public AssetService(AssetRepository assetRepository, DomainEventPublisher publisher) {
        this.assetRepository = Objects.requireNonNull(assetRepository, "AssetRepository cannot be null");
        this.publisher = Objects.requireNonNull(publisher, "DomainEventPublisher cannot be null");
    }

    @Override
    public Asset createAsset(Asset asset) {
        if (assetRepository.existsByIpAddress(asset.getIpAddress())) {
            throw new DuplicateIpAddressException(asset.getIpAddress());
        }
        assetRepository.save(asset);
        publisher.publish(AssetCreatedEvent.of(asset.getId(), asset.getType()));
        return asset;
    }

    @Override
    public Asset findAsset(AssetId id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(id));
    }

    @Override
    public List<Asset> findAllAssets() {
        return assetRepository.findAll();
    }

    @Override
    public Asset updateAssetStatus(AssetId id, AssetStatus newStatus) {
        Asset asset = findAsset(id);
        switch (newStatus) {
            case ACTIVE -> asset.activate();
            case INACTIVE -> asset.deactivate();
            case MAINTENANCE -> asset.putInMaintenance();
        }
        assetRepository.save(asset);
        publisher.publish(AssetStatusChangedEvent.of(id, newStatus));
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
        publisher.publish(AssetDeletedEvent.of(id));
    }
}

