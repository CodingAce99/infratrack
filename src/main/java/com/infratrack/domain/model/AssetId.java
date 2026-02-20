package com.infratrack.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class AssetId {

    private final UUID value;

    private AssetId(UUID value) {
        this.value = Objects.requireNonNull(value, "AssetId value cannot be null");
    }

    // --- Factory methods ---

    public static AssetId generate() {
        return new AssetId(UUID.randomUUID());
    }

    public static AssetId of(UUID value) {
        return new AssetId(value);
    }

    public static AssetId of(String value) {
        Objects.requireNonNull(value, "AssetId string value cannot be null");
        try {
            return new AssetId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid AssetId format: '" + value + "'", e);
        }
    }

    // --- Accessors ---

    public UUID getValue() {
        return value;
    }

    // --- Standard methods ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetId other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}