package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.User;
import java.util.List;
import java.util.Optional;

/**
 * User Service Interface
 * Manages user operations for the scheduler application
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-12-22
 */
public interface UserService {

    /**
     * Get all users
     */
    List<User> findAll();

    /**
     * Find user by ID
     */
    Optional<User> findById(Long id);

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Get all enabled users
     */
    List<User> findAllEnabled();

    /**
     * Get all administrators
     */
    List<User> findAllAdministrators();
}
