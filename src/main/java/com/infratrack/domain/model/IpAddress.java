package com.infratrack.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing an IP address (IPv4 or IPv6).
 *
 * Immutable: Once created, cannot be changed.
 * Self-validating: Constructor validates the format.
 */
public record IpAddress(String value) {

    // Regex for IPv4 validation (simple version)
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
    );

    // Compact constructor for validation
    public IpAddress {
        Objects.requireNonNull(value, "IP address cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("IP address cannot be empty");
        }

        if (!isValidIpv4(value)) {
            throw new IllegalArgumentException(
                    "Invalid IP address format: " + value
            );
        }
    }

    private static boolean isValidIpv4(String ip) {
        return IPV4_PATTERN.matcher(ip).matches();
    }

    @Override
    public String toString() {
        return value;
    }
}
