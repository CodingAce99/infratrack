package com.infratrack.domain.model;

public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(AssetId id) {
        super("Asset not found with id: " + id);
    }
}
