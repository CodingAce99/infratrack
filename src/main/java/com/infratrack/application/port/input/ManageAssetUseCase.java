package com.infratrack.application.port.input;

import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.AssetStatus;
import com.infratrack.domain.model.Credentials;
import com.infratrack.domain.model.IpAddress;

import java.util.List;

public interface ManageAssetUseCase {

    Asset createAsset(Asset asset);

    Asset findAsset(AssetId id);

    List<Asset> findAllAssets();

    Asset updateAssetStatus(AssetId id, AssetStatus newStatus);

    Asset updateAssetCredentials(AssetId id, Credentials newCredentials);

    Asset updateAssetIpAddress(AssetId id, IpAddress newIpAddress);

    void deleteAsset(AssetId id);
}

