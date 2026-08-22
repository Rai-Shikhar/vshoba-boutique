package com.vshoba.boutique.security;

import com.vshoba.boutique.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Creates and verifies JWT tokens.
 *
 * A JWT is a signed string like:
 *   eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwcml5YUBleGFtcGxlLmNvbSIs...signature
 * It contains the email (subject) and role, signed with a secret key.
 * Anyone with the token is treated as that user - so the token IS
 * the login. It expires after 24 hours (app.jwt.expiration-ms).
 */
@Service
public class JwtService {

    private final String secret;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    /**
     * Build the cryptographic key from the secret string.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Create a token for a logged-in user.
     */
    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(getSigningKey())
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

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
