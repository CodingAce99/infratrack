package com.infratrack.domain.model;

import java.util.Objects;

public final class EncodedPassword {

    private final String value;
    public EncodedPassword(String value) {
        Objects.requireNonNull(value, "value cannot be null");

        if(value.isBlank()) {
            throw new IllegalArgumentException("value cannot be blank");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncodedPassword other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
