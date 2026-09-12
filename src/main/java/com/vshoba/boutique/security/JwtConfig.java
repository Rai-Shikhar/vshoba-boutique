package com.vshoba.boutique.security;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Builds the JWT signing key from the configured secret, fail-fast.
 *
 * Previously the key was built lazily inside JwtService on every sign/verify
 * call. A missing or too-short secret therefore only blew up at runtime
 * (confusing 401/500 responses instead of a clear startup error), and could
 * silently differ between the signing and verifying paths.
 *
 * This bean validates ONCE at startup and never falls back to a randomly
 * generated key under any circumstance.
 */
@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSigningKey(@Value("${app.jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret is not set. Refusing to start the app without a JWT signing key.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 bytes for HS256; got " + keyBytes.length
                            + " bytes. Generate one with: openssl rand -base64 48");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}