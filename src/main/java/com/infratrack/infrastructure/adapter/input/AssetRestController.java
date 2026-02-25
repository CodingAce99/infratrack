package com.infratrack.infrastructure.adapter.input;

import com.infratrack.application.port.input.ManageAssetUseCase;
import com.infratrack.domain.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetRestController {

    private final ManageAssetUseCase useCase;

    // contructor con inyección

    public AssetRestController(ManageAssetUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ManageAssetUseCase cannot be null");
    }

    // endpoints REST para crear, obtener, actualizar y eliminar activos
    @GetMapping
    public ResponseEntity<List<Asset>> findAllAssets() {
        List<Asset> assets = useCase.findAllAssets();
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Asset> findAsset(@PathVariable String id) {
        AssetId assetId = AssetId.of(id);
        Asset asset = useCase.findAsset(assetId);
        return ResponseEntity.ok(asset);
    }

    @PostMapping
    public ResponseEntity<Asset> createAsset(
            @RequestParam String name,
            @RequestParam String type,
            @RequestParam String ipAddress,
            @RequestParam String username,
            @RequestParam String password
    ){
        Asset asset = useCase.createAsset(
                name,
                AssetType.valueOf(type.toUpperCase()),
                IpAddress.of(ipAddress),
                Credentials.of(username, password)
        );
        return ResponseEntity.status(201).body(asset);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Asset> updateAssetStatus(
            @PathVariable String id,
            @RequestParam String status
    ) {
        AssetId assetId = AssetId.of(id);
        AssetStatus assetStatus = AssetStatus.valueOf(status.toUpperCase());
        Asset updatedAsset = useCase.updateAssetStatus(assetId, assetStatus);
        return ResponseEntity.ok(updatedAsset);
    }

    @PutMapping("/{id}/credentials")
    public ResponseEntity<Asset> updateAssetCredentials(
            @PathVariable String id,
            @RequestParam String username,
            @RequestParam String password
    ) {
        AssetId assetId = AssetId.of(id);
        Credentials newCredentials = Credentials.of(username, password);
        Asset updatedAsset = useCase.updateAssetCredentials(assetId, newCredentials);
        return ResponseEntity.ok(updatedAsset);
    }

    @PutMapping("/{id}/ip")
    public ResponseEntity<Asset> updateAssetIpAddress(
            @PathVariable String id,
            @RequestParam String ipAddress
    ) {
        AssetId assetId = AssetId.of(id);
        IpAddress newIpAddress = IpAddress.of(ipAddress);
        Asset updatedAsset = useCase.updateAssetIpAddress(assetId, newIpAddress);
        return ResponseEntity.ok(updatedAsset);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable String id) {
        AssetId assetId = AssetId.of(id);
        useCase.deleteAsset(assetId);
        return ResponseEntity.noContent().build();
    }

    // @ExceptionHandler para manejar errores
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
