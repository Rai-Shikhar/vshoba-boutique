package com.vshoba.boutique.dto;

/**
 * Response of successful login/registration.
 * The client stores this token and sends it as:
 *   Authorization: Bearer <token>
 * on every request that needs to know who you are.
 *
 * The password is never part of this - only the role.
 */
public record AuthResponse(
        String token,
        Long userId,
        String fullName,
        String email,
        String role
) {
}
