package com.infratrack.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class UserId {

    private final UUID value;

    private UserId(UUID value) {
        this.value = Objects.requireNonNull(value, "UserId value cannot be null");
    }

    // --- Factory Methods ---
    public static UserId generate() { return new UserId(UUID.randomUUID()); }

    public static UserId of(UUID value) { return new UserId(value);}

    public static UserId of(String value) {
        Objects.requireNonNull(value, "UserId string value cannot be null");
        try {
            return new UserId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid userId format: '" + value + "'", e);
        }
    }

    // --- Accessors ---
    public UUID getValue() {
        return value;
    }

    // --- Standard Methods ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserId other)) return false;
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
