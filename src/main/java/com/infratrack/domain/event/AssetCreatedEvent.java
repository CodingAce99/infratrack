package com.infratrack.domain.event;

import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.AssetType;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event: an Asset was successfully created and persisted.
 *
 * <p>This is a plain Java record — no Spring, no JPA, no framework.
 * It captures the minimum information listeners need to react:
 * which asset, what type, and when it happened.
 *
 * <p>Published by AssetService after a successful repository save.
 * Consumed by any infrastructure adapter that registers a listener.
 */
public record AssetCreatedEvent(
        AssetId assetId,
        AssetType assetType,
        Instant occurredOn
) {
    // Compact constructor - validation without repeating field assignments
    public AssetCreatedEvent {
        Objects.requireNonNull(assetId, "assetId cannot be null");
        Objects.requireNonNull(assetType, "assetType cannot be null");
        Objects.requireNonNull(occurredOn, "occurredOn cannot be null");
    }

    /**
     * Factory method — the canonical way to create this event.
     * Captures "now" automatically so callers don't have to supply the timestamp.
     */
    public static AssetCreatedEvent of(AssetId assetId, AssetType assetType) {
        return new AssetCreatedEvent(assetId, assetType, Instant.now());
    }
}
