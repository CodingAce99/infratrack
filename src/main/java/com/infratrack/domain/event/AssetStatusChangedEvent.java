package com.infratrack.domain.event;

import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.AssetStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event: the status of an Asset was changed.
 *
 * <p>This is a plain Java record — no Spring, no JPA, no framework.
 * It captures the minimum information listeners need to react:
 * which asset, what type, and when it happened.
 *
 * <p>Published by AssetService after a successful repository update.
 * Consumed by any infrastructure adapter that registers a listener.
 */
public record AssetStatusChangedEvent(
        AssetId assetId,
        AssetStatus assetStatus,
        Instant occurredOn
) {
    // Compact constructor - validation without repeating field assignments
    public AssetStatusChangedEvent {
        Objects.requireNonNull(assetId, "AssetId cannot be null");
        Objects.requireNonNull(assetStatus, "AssetStatus cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
    }

    /**
     * Factory method — the canonical way to create this event.
     * Captures "now" automatically so callers don't have to supply the timestamp.
     */
    public static AssetStatusChangedEvent of(AssetId assetId, AssetStatus assetStatus) {
        return new AssetStatusChangedEvent(assetId, assetStatus, Instant.now());
    }
}
