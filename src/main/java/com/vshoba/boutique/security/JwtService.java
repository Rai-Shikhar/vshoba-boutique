package com.vshoba.boutique.security;

import com.vshoba.boutique.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

/**
 * Creates and verifies JWT tokens.
 *
 * A JWT is a signed string like:
 *   eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwcml5YUBleGFtcGxlLmNvbSIs...signature
 * It contains the email (subject) and role, signed with a secret key.
 * Anyone with the token is treated as that user - so the token IS
 * the login. It expires after 24 hours (app.jwt.expiration-ms).
 *
 * Since the KeyRotation hardening, the app can hold TWO secrets:
 * the current signing key (index 0) plus an optional "previous" key
 * (app.jwt.previous-secret) used only for VERIFYING tokens that were
 * signed before a secret rotation. New tokens are ALWAYS signed with
 * the current key - never the previous one.
 *
 * The key itself is validated at startup by JwtConfig (fail-fast),
 * so a missing or too-short secret stops the app instead of producing
 * confusing 401/500 errors at runtime.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final List<SecretKey> validKeys;
    private final long expirationMs;

    public JwtService(SecretKey jwtSigningKey,
                      @Value("${app.jwt.previous-secret:}") String previousSecret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = jwtSigningKey;
        this.expirationMs = expirationMs;

        List<SecretKey> keys = new ArrayList<>();
        keys.add(jwtSigningKey);
        if (previousSecret != null && !previousSecret.isBlank()) {
            keys.add(Keys.hmacShaKeyFor(previousSecret.getBytes(StandardCharsets.UTF_8)));
        }
        this.validKeys = Collections.unmodifiableList(keys);
    }

    /**
     * TEMPORARY DIAGNOSTIC - used to prove whether the JWT secret is stable
     * across Render restarts/instances. Logs a fingerprint of the active
     * signing key (never the secret itself). Compares fingerprints across
     * restarts in the Render logs; if they differ, the secret is being
     * regenerated. REMOVE THIS METHOD AFTER DEBUGGING.
     */
    @PostConstruct
    void logSecretFingerprint() {
        log.info("TEMPORARY DIAGNOSTIC: JWT current secret length={} sha256(first8)={}, previous-secret configured={}",
                signingKey.getEncoded().length,
                sha256First8(signingKey.getEncoded()),
                validKeys.size() > 1);
    }

    private static String sha256First8(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(hash).substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            return "n/a";
        }
    }

    /**
     * Create a token for a logged-in user.
     * Always signed with the CURRENT secret - never the previous one.
     */
    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extract the email out of a token.
     * Throws if the token is tampered with - callers catch that.
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * True if the token is unexpired AND belongs to this user.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = parseClaims(token);
        boolean emailMatches = userDetails.getUsername().equalsIgnoreCase(claims.getSubject());
        boolean notExpired = claims.getExpiration().after(new Date());
        return emailMatches && notExpired;
    }

    /**
     * Verify the token's signature against every known key (current first,
     * then the optional previous one). A token signed under a rotated-out
     * secret stays valid for its remaining lifetime.
     */
    private Claims parseClaims(String token) {
        JwtException last = null;
        for (SecretKey key : validKeys) {
            try {
                return Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (JwtException ex) {
                last = ex;
            }
        }
        throw last;
    }
}