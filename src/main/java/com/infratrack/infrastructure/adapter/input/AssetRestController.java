package com.infratrack.infrastructure.adapter.input;

import com.infratrack.application.port.input.ManageAssetUseCase;
import com.infratrack.domain.model.*;
import com.infratrack.infrastructure.adapter.input.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Objects;

//(Adapter Rest HTTP de entrada)
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
    public ResponseEntity<List<AssetResponse>> findAllAssets() {
        List<Asset> assets = useCase.findAllAssets();
        List<AssetResponse> assetResponse = assets.stream()
                .map(AssetDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(assetResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponse> findAsset(@PathVariable String id) {
        Asset asset = useCase.findAsset(AssetId.of(id));
        return ResponseEntity.status(HttpStatus.OK).body(AssetDtoMapper.toResponse(asset));
    }

    @PostMapping
    public ResponseEntity<AssetResponse> createAsset(@Valid @RequestBody CreateAssetRequest request) {
        Asset asset = useCase.createAsset(AssetDtoMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AssetDtoMapper.toResponse(asset));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AssetResponse> updateAssetStatus(
            @PathVariable String id,
            @RequestBody @Valid UpdateStatusRequest request
            ) {
        Asset updatedAsset = useCase.updateAssetStatus(AssetId.of(id), AssetDtoMapper.toStatus(request));
        return ResponseEntity.status(HttpStatus.OK)
                .body(AssetDtoMapper.toResponse(updatedAsset));
    }

    @PutMapping("/{id}/credentials")
    public ResponseEntity<AssetResponse> updateAssetCredentials(
            @PathVariable String id,
            @Valid @RequestBody UpdateCredentialsRequest request
    ) {
        Asset updatedAsset = useCase.updateAssetCredentials(AssetId.of(id), AssetDtoMapper.toCredentials(request));
        return ResponseEntity.status(HttpStatus.OK)
                .body(AssetDtoMapper.toResponse(updatedAsset));
    }

    @PutMapping("/{id}/ip")
    public ResponseEntity<AssetResponse> updateAssetIpAddress(
            @PathVariable String id,
            @Valid @RequestBody UpdateIpAddressRequest request
    ) {
        Asset updatedAsset = useCase.updateAssetIpAddress(AssetId.of(id), AssetDtoMapper.toIpAddress(request));
        return ResponseEntity.status(HttpStatus.OK)
                .body(AssetDtoMapper.toResponse(updatedAsset));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable String id) {
        AssetId assetId = AssetId.of(id);
        useCase.deleteAsset(assetId);
        return ResponseEntity.noContent().build();
    }
}
