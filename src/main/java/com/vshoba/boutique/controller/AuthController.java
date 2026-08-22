package com.vshoba.boutique.controller;

import com.vshoba.boutique.dto.AuthResponse;
import com.vshoba.boutique.dto.ChangePasswordRequest;
import com.vshoba.boutique.dto.LoginRequest;
import com.vshoba.boutique.dto.RegisterRequest;
import com.vshoba.boutique.model.User;
import com.vshoba.boutique.security.JwtService;
import com.vshoba.boutique.security.SecurityUser;
import com.vshoba.boutique.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public auth API: create an account, log in, inspect your own profile.
 *
 * register and login are the ONLY endpoints anyone can call without
 * a token (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService,
                          JwtService jwtService,
                          AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * POST /api/auth/register - create a customer account.
     * Example body:
     * {
     *   "fullName": "Priya Shoba",
     *   "email": "priya@example.com",
     *   "password": "secret123",
     *   "phone": "9876543210",
     *   "address": "12 MG Road, Chennai"
     * }
     * Returns a login token immediately - no second login needed.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerCustomer(request);
        return new AuthResponse(jwtService.generateToken(user), user.getId(),
                user.getFullName(), user.getEmail(), user.getRole().name());
    }

    /**
     * POST /api/auth/login - body: { "email": "...", "password": "..." }
     * Wrong credentials -> 401 via GlobalExceptionHandler.
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
        User user = ((SecurityUser) authentication.getPrincipal()).getUser();
        return new AuthResponse(jwtService.generateToken(user), user.getId(),
                user.getFullName(), user.getEmail(), user.getRole().name());
    }

    /**
     * GET /api/auth/me - "who am I?" using the token from the header.
     * Returns the fresh user data from the database.
     */
    @GetMapping("/me")
    public User me(@AuthenticationPrincipal SecurityUser currentUser) {
        return currentUser.getUser();
    }

    /**
     * POST /api/auth/change-password
     * Body: { "oldPassword": "...", "newPassword": "..." }
     */
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal SecurityUser currentUser,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.getUser().getId(), request);
    }
}
