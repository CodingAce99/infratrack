package com.infratrack.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing an IP address (IPv4 or IPv6).
 * <p>
 * Immutable: Once created, cannot be changed.
 * Self-validating: Constructor validates the format.
 */
public final class IpAddress {

    // Accepts IPv4 addresses and valid hostnames (RFC 1123)
    private static final Pattern VALID_HOST_PATTERN = Pattern.compile(
            "^(((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)" +
                    "|([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?" +
                    "(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*))$"
    );

    private final String value;

    private IpAddress(String value) {
        Objects.requireNonNull(value, "IP address cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("IP address cannot be empty");
        }

        if (!isValidHostAddress(value)) {
            throw new IllegalArgumentException(
                    "Invalid IP address format: " + value
            );
        }

        this.value = value;
    }

    public static IpAddress of(String value) {
        return new IpAddress(value);
    }

    public String getValue() {
        return value;
    }

    private static boolean isValidHostAddress(String Value) {
        return VALID_HOST_PATTERN.matcher(Value).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IpAddress other)) return false;
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
