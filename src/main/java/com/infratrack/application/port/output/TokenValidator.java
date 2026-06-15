package com.infratrack.application.port.output;

import com.infratrack.domain.exception.InvalidTokenException;

public interface TokenValidator {
    // Validates the raw JWT string and returns its claims.
    // Throws InvalidTokenException on bad signature, expiry, or malformed input.
    TokenClaims validate(String token) throws InvalidTokenException;
}
