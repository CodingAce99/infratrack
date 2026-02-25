package com.infratrack.infrastructure.adapter.output;

import com.infratrack.application.port.output.AssetRepository;
import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryAssetRepository implements AssetRepository {

    private final Map<String, Asset> store = new HashMap<>();

    @Override
    public Optional<Asset> findById(AssetId id) {
        return Optional.ofNullable(store.get(id.toString()));
    }

    @Override
    public List<Asset> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void save(Asset asset) {
        store.put(asset.getId().toString(), asset);
    }

    @Override
    public void delete(AssetId id) {
        store.remove(id.toString());
    }
}

