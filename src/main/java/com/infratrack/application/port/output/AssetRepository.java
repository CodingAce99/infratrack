package com.infratrack.application.port.output;

import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetId;

import java.util.List;
import java.util.Optional;

public interface AssetRepository {

    Optional<Asset> findById(AssetId id);

    List<Asset> findAll();

    void save(Asset asset);

    void delete(AssetId id);
}

