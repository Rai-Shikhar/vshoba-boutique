package com.vshoba.boutique.service;

import com.vshoba.boutique.dto.ChangePasswordRequest;
import com.vshoba.boutique.dto.RegisterRequest;
import com.vshoba.boutique.dto.UpdateProfileRequest;
import com.vshoba.boutique.exception.BusinessRuleException;
import com.vshoba.boutique.exception.ResourceNotFoundException;
import com.vshoba.boutique.model.User;
import com.vshoba.boutique.model.UserRole;
import com.vshoba.boutique.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for users: registration, login support and profiles.
 *
 * Since Phase 5, every password is BCrypt-hashed the moment it arrives -
 * the raw password is never stored and never leaves the service.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- Registration ----------

    /**
     * Self-registration from the shop's "create account" form.
     * The role is forced to CUSTOMER - nobody can register as admin.
     */
    public User registerCustomer(RegisterRequest request) {
        return register(request, UserRole.CUSTOMER);
    }

    /**
     * Admin accounts, created by existing admins from the back office.
     */
    public User registerAdmin(RegisterRequest request) {
        return register(request, UserRole.ADMIN);
    }

    private User register(RegisterRequest request, UserRole role) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("An account with email " + request.email() + " already exists");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setAddress(request.address());
        user.setRole(role);
        return userRepository.save(user);
    }

    // ---------- Reads ----------

    /**
     * Login helper: finds the account; Spring Security compares passwords.
     */
    public Optional<User> getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessRuleException("Email is required");
        }
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " does not exist"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ---------- Writes ----------

    /**
     * Change password for a logged-in user: old password must match,
     * then the new one is hashed and stored.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /**
     * Partial update of profile fields. All fields optional.
     * Changing to an email that another account uses is rejected.
     * A provided "password" is treated as a NEW password and hashed.
     */
    @Transactional
    public User updateProfile(Long id, UpdateProfileRequest request) {
        User existing = getUserById(id);

        if (request.fullName() != null) {
            existing.setFullName(request.fullName().trim());
        }
        if (request.phone() != null) {
            existing.setPhone(request.phone());
        }
        if (request.address() != null) {
            existing.setAddress(request.address());
        }
        if (request.password() != null && !request.password().isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.email() != null) {
            String newEmail = request.email().trim();
            if (!newEmail.equalsIgnoreCase(existing.getEmail())
                    && userRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new BusinessRuleException("An account with email " + newEmail + " already exists");
            }
            existing.setEmail(newEmail);
        }

        return userRepository.save(existing);
    }

    /**
     * Admins are protected from accidental deletion - they must be
     * demoted to CUSTOMER first (in a future admin-management phase).
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessRuleException("Admin accounts cannot be deleted. Demote the user to CUSTOMER first.");
        }
        userRepository.delete(user);
    }
}
