package com.vshoba.boutique.exception;

import java.time.LocalDateTime;

/**
 * The uniform JSON shape every error response uses:
 *
 * {
 *   "status": 404,
 *   "message": "Product with id 999 does not exist",
 *   "timestamp": "2026-08-11T19:30:00"
 * }
 *
 * A record is a tiny immutable class - perfect for this.
 */
public record ErrorResponse(int status, String message, LocalDateTime timestamp) {

    public ErrorResponse(int status, String message) {
        this(status, message, LocalDateTime.now());
    }
}
