package com.vshoba.boutique.exception;

/**
 * Thrown when a requested record does not exist
 * (e.g. "get product with id 999" and there is no product 999).
 *
 * In Phase 4 this becomes an HTTP 404 Not Found automatically.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
