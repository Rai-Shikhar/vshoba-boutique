package com.vshoba.boutique.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/auth/register and POST /api/users/admin.
 * Note: the API field is "password" (the real password),
 * NOT "passwordHash" - hashing is done inside the service.
 */
public record RegisterRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name cannot exceed 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email address is not valid")
        @Size(max = 200, message = "Email cannot exceed 200 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        String phone,

        @Size(max = 500, message = "Address cannot exceed 500 characters")
        String address
) {
}
