package com.infratrack.infrastructure.adapter.output;

import com.infratrack.domain.model.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("JjwtTokenGenerator")
public class JjwtTokenGeneratorTest {

    // 32+ bytes -- satisfies HS256 minimum key length
    private static final String TEST_SECRET = "test-secret-key-for-unit-tests-only";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JjwtTokenGenerator generator;
    private SecretKey verificationKey;
    @BeforeEach
    public void setup() {
        generator = new JjwtTokenGenerator(TEST_SECRET, EXPIRATION_MS);
        // Build the same key independently so we can verify the token in tests.
        // In production, parsing belongs to Sprint 7.3. Here it is test-only.
        verificationKey = Keys.hmacShaKeyFor(
                TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private User testUser() {
        return User.reconstitute(
                UserId.generate(),
                new Username("admin"),
                new EncodedPassword("$2a$10$hash"),
                UserRole.ADMIN
        );
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {
        @Test
        @DisplayName("generated token contains correct subject and role claim")
        void generateToken() {
            // GIVEN
            User user = testUser();

            // WHEN
            String token = generator.generateToken(user);

            // THEN -- parse the token to inspect the claims
            Claims claims = Jwts.parser()
                    .verifyWith(verificationKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            assertAll(
                    () -> assertEquals("admin", claims.getSubject()),
                    () -> assertEquals("ADMIN", claims.get("role", String.class))
            );
        }

        @Test
        @DisplayName("expiration time is 1 hour aprox in the future (margin ±5sec)")
        void generateTokenWithExpirationTime() {
            // GIVEN
            User user = testUser();
            String token = generator.generateToken(user);

            // WHEN
            Claims claims = Jwts.parser()
                    .verifyWith(verificationKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // THEN
            long now = System.currentTimeMillis();
            long expTime = claims.getExpiration().getTime();
            assertTrue(expTime > now, "Expiration time should be in the future");
            assertTrue(expTime <= now + EXPIRATION_MS + 5000, "Expiration time should be within expected range");
        }

        @Test
        @DisplayName("verify with wrong key throws exception")
        void verifyWithWrongKey_throwsException() {
            // GIVEN
            User user = testUser();
            String token = generator.generateToken(user);

            // WHEN
            SecretKey key = Keys.hmacShaKeyFor("wrong-secret-key-for-unit-tests-only".getBytes(StandardCharsets.UTF_8));

            // THEN
            assertThrows(io.jsonwebtoken.security.SignatureException.class, () ->
                            Jwts.parser()
                                    .verifyWith(key)
                                    .build()
                                    .parseSignedClaims(token)
            );
        }
    }
}
