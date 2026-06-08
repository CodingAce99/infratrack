package com.infratrack.infrastructure.adapter.output;

import com.infratrack.application.port.output.TokenGenerator;
import com.infratrack.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

// Output adapter: implements the TokenGenerator port using jjwt 0.13 (HS256).
// No @Component — wired manually in BeanConfiguration with @Profile({"demo","prod"}).
public class JjwtTokenGenerator implements TokenGenerator {

    private final SecretKey secretKey;
    private final long expirationMs;

    // The SecretKey is built ONCE in the constructor, not on every generateToken() call.
    // Keys.hmacShaKeyFor() is not expensive, but rebuilding it per-request is unnecessary
    // churn. More importantly, building it once makes the constraint visible: if the secret
    // is too short (< 32 bytes for HS256), we fail fast at startup, not mid-request.
    // jjwt enforces 256-bit minimum for HS256 — a shorter key throws WeakKeyException here.
    public JjwtTokenGenerator(String secret, long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String generateToken(User user) {
        Instant now = Instant.now();

        return Jwts.builder()
                // subject: the "who is this token for" claim — standard JWT field.
                // We use the username string (not the UserId) because that's what
                // the frontend will display and what the filter will look up in 7.3.
                .subject(user.getUsername().toString())

                // role: custom claim. "role" is our chosen key — must match exactly
                // in 7.3 when the filter reads it back. Values: "ADMIN" or "VIEWER".
                // getUserRole().name() gives the enum name as a String.
                .claim("role", user.getUserRole().name())

                // iat (issued at): standard JWT claim. Tells the receiver when the
                // token was created. jjwt requires java.util.Date, not Instant —
                // hence Date.from(now).
                .issuedAt(Date.from(now))

                // exp (expiration): standard JWT claim. After this timestamp, the
                // token is invalid. 1 hour = 3,600,000 ms. In production you would
                // use shorter-lived access tokens (5–15 min) plus a refresh token
                // to limit the blast radius of a stolen token. Simplicity has been
                // choosen knowingly for this portfolio.
                .expiration(Date.from(now.plusMillis(expirationMs)))

                // signWith(key): jjwt 0.13 infers the algorithm from the key type
                // and length. A 256-bit HMAC key → HS256. No need to pass
                // SignatureAlgorithm.HS256 explicitly — that API is removed in 0.12+.
                .signWith(secretKey)

                // compact() serializes to the final "header.payload.signature" string.
                .compact();
    }
}
