package com.vshoba.boutique.repository;

import com.vshoba.boutique.model.User;
import com.vshoba.boutique.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Database access for users.
 *
 * Optional<User> is the safe return type for "find something that may not
 * exist": it forces the caller to handle the empty case instead of
 * crashing on a null.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find one user by email. Emails are unique (enforced by the model),
     * so at most one row can match.
     */
    Optional<User> findByEmail(String email);

    /**
     * Case-insensitive version - users type "SHOBA@gmail.com" and
     * "shoba@gmail.com" alike; both must match the same account.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Cheap existence check - much faster than loading the whole user.
     * Used at registration to reject duplicate accounts.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * All users with a given role (e.g. all admins).
     */
    List<User> findByRole(UserRole role);
}
