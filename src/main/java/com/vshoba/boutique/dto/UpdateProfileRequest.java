package com.vshoba.boutique.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /api/users/{id} - partial profile update.
 * Every field is optional; only the ones sent get changed.
 * If "password" is sent, it is treated as a NEW raw password and hashed.
 */
public record UpdateProfileRequest(

        @Size(max = 150, message = "Full name cannot exceed 150 characters")
        String fullName,

        @Email(message = "Email address is not valid")
        @Size(max = 200, message = "Email cannot exceed 200 characters")
        String email,

        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        String phone,

        @Size(max = 500, message = "Address cannot exceed 500 characters")
        String address,

        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
