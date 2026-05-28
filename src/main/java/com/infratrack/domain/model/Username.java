package com.infratrack.domain.model;

import com.infratrack.domain.exception.InvalidUsernameException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing a username in the system.
 * <p>
 * Rules: 3–50 characters, only [a-zA-Z0-9_-]. No whitespace, no special characters.
 * Immutable and self-validating: invalid input is rejected at construction time.
 */
public final class Username {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;

    // Only alphanumeric, underscore, and hypen. Anchored with ^ and $ to ensure full string.
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private final String value;

    public Username(String value) {
        Objects.requireNonNull(value, "Username cannot be null");

        if (value.isBlank()) {
            throw new InvalidUsernameException("Username cannot be blank");
        }
        if (value.length() < MIN_LENGTH) {
            throw new InvalidUsernameException(
                    "Username must be at least " + MIN_LENGTH + " characters, got: " + value.length()
            );
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidUsernameException(
                    "Username must be at most " + MAX_LENGTH + " characters, got: " + value.length()
            );
        }
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new InvalidUsernameException(
                    "Username contains invalid characters. Only letters, digits, '_' and '-' are allowed."
            );
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Username other)) return false;
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
