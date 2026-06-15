package com.infratrack.infrastructure.adapter.output;

import com.infratrack.application.port.output.TokenClaims;
import com.infratrack.application.port.output.TokenValidator;
import com.infratrack.domain.exception.InvalidTokenException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

// Output adapter: implements TokenValidator using jjwt 0.13 parse API.
// No @Component — wired manually in BeanConfiguration with @Profile({"demo","prod"}).
public class JjwtTokenValidator implements TokenValidator {

    private final SecretKey secretKey;

    // Key is built once at startup from the same secret used by JjwtTokenGenerator.
    // A mismatched or too-short secret fails fast here, not on the first request.
    public JjwtTokenValidator(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public TokenClaims validate(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new TokenClaims(
                    claims.getSubject(),
                    claims.get("role", String.class)
            );
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException covers: SignatureException, ExpiredJwtException,
            // MalformedJwtException, UnsupportedJwtException.
            // IllegalArgumentException covers a null/blank token string.
            throw new InvalidTokenException();
        }
    }
}
