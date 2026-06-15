package com.infratrack.infrastructure.adapter.output;

import com.infratrack.application.port.output.TokenClaims;
import com.infratrack.domain.exception.InvalidTokenException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JjwtTokenValidator")
class JjwtTokenValidatorTest {

    // Same secret as JjwtTokenGeneratorTest — 32+ bytes satisfies HS256 minimum.
    private static final String TEST_SECRET = "test-secret-key-for-unit-tests-only";

    private JjwtTokenValidator validator;
    private SecretKey signingKey;

    @BeforeEach
    void setup() {
        validator = new JjwtTokenValidator(TEST_SECRET);
        // Build the same key independently to sign tokens inside the tests.
        signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String buildToken(String username, String role, Instant expiry) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        @DisplayName("valid token with ADMIN role returns correct TokenClaims")
        void valid_admin_token_returns_claims() {
            // GIVEN
            String token = buildToken("admin", "ADMIN", Instant.now().plusSeconds(3600));

            // WHEN
            TokenClaims claims = validator.validate(token);

            // THEN
            assertAll(
                    () -> assertEquals("admin", claims.username()),
                    () -> assertEquals("ADMIN", claims.role())
            );
        }

        @Test
        @DisplayName("valid token with VIEWER role returns correct TokenClaims")
        void valid_viewer_token_returns_claims() {
            // GIVEN
            String token = buildToken("viewer", "VIEWER", Instant.now().plusSeconds(3600));

            // WHEN
            TokenClaims claims = validator.validate(token);

            // THEN
            assertAll(
                    () -> assertEquals("viewer", claims.username()),
                    () -> assertEquals("VIEWER", claims.role())
            );
        }

        @Test
        @DisplayName("tampered signature throws InvalidTokenException")
        void tampered_signature_throws() {
            // GIVEN
            String token = buildToken("admin", "ADMIN", Instant.now().plusSeconds(3600));
            // Corrupt the FIRST character of the signature segment (all 6 bits are
            // significant there — unlike the last character which may have unused padding bits).
            String[] parts = token.split("\\.");
            char first = parts[2].charAt(0);
            char replacement = (first == 'A') ? 'B' : 'A';
            String tampered = parts[0] + "." + parts[1] + "." + replacement + parts[2].substring(1);

            // WHEN + THEN
            assertThrows(InvalidTokenException.class, () -> validator.validate(tampered));
        }

        @Test
        @DisplayName("expired token throws InvalidTokenException")
        void expired_token_throws() {
            // GIVEN — expiration 10 seconds in the past
            String token = buildToken("admin", "ADMIN", Instant.now().minusSeconds(10));

            // WHEN + THEN
            assertThrows(InvalidTokenException.class, () -> validator.validate(token));
        }

        @Test
        @DisplayName("malformed string throws InvalidTokenException")
        void malformed_token_throws() {
            // WHEN + THEN
            assertThrows(InvalidTokenException.class, () -> validator.validate("not.a.jwt"));
        }

        @Test
        @DisplayName("token signed with a different key throws InvalidTokenException")
        void wrong_key_throws() {
            // GIVEN
            SecretKey wrongKey = Keys.hmacShaKeyFor(
                    "wrong-secret-key-for-unit-tests-only".getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                    .subject("admin")
                    .claim("role", "ADMIN")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(3600)))
                    .signWith(wrongKey)
                    .compact();

            // WHEN + THEN
            assertThrows(InvalidTokenException.class, () -> validator.validate(token));
        }
    }
}
