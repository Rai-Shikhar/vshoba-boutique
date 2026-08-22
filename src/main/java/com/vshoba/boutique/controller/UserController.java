package com.vshoba.boutique.controller;

import com.vshoba.boutique.dto.UpdateProfileRequest;
import com.vshoba.boutique.exception.ResourceNotFoundException;
import com.vshoba.boutique.model.User;
import com.vshoba.boutique.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only user management (see SecurityConfig: /api/users/** = ADMIN).
 * Registration and login live in AuthController instead.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/users/admin - create an admin account.
     * Body is the same RegisterRequest shape as customer registration.
     */
    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public User registerAdmin(@RequestBody com.vshoba.boutique.dto.RegisterRequest request) {
        return userService.registerAdmin(request);
    }

    /**
     * GET /api/users - list all accounts.
     */
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * GET /api/users/{id} - one account.
     */
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    /**
     * GET /api/users/by-email?email=... - find by email. 404 if missing.
     */
    @GetMapping("/by-email")
    public User getUserByEmail(@RequestParam String email) {
        return userService.getUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with email " + email + " does not exist"));
    }

    /**
     * PUT /api/users/{id} - update profile fields.
     * All fields optional; a provided "password" is hashed before saving.
     */
    @PutMapping("/{id}")
    public User updateProfile(@PathVariable Long id, @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(id, request);
    }

    /**
     * DELETE /api/users/{id} - remove a customer account.
     * Admin accounts are protected (400) until demoted.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
