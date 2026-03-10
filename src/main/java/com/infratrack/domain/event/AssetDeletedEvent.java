package com.infratrack.domain.event;

import com.infratrack.domain.model.AssetId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event: an Asset was successfully deleted.
 *
 * <p>This is a plain Java record — no Spring, no JPA, no framework.
 * It captures the minimum information listeners need to react:
 * which asset, what type, and when it happened.
 *
 * <p>Published by AssetService after a successful repository deletion.
 * Consumed by any infrastructure adapter that registers a listener.
 */
public record AssetDeletedEvent(
        AssetId assetId,
        Instant occurredOn
) {
    // Compact constructor - validation without repeating field assignments
    public AssetDeletedEvent {
        Objects.requireNonNull(assetId, "AssetId cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
    }

    /**
     * Factory method — the canonical way to create this event.
     * Captures "now" automatically so callers don't have to supply the timestamp.
     */
    public static AssetDeletedEvent of(AssetId assetId) {
        return new AssetDeletedEvent(assetId, Instant.now());
    }
}
