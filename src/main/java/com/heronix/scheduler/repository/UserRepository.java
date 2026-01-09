package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.User;
import com.heronix.scheduler.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository
 * Manages user accounts for the scheduler application
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-12-22
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username (exact match)
     * Used for login authentication
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by username (case-insensitive)
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Find user by email (exact match)
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by email (case-insensitive)
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if username exists (exact match)
     */
    boolean existsByUsername(String username);

    /**
     * Check if username exists (case-insensitive)
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Check if email exists (exact match)
     */
    boolean existsByEmail(String email);

    /**
     * Check if email exists (case-insensitive)
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find all users by primary role
     */
    List<User> findByPrimaryRole(Role role);

    /**
     * Count users by primary role
     */
    long countByPrimaryRole(Role role);

    /**
     * Find all enabled users
     */
    List<User> findByEnabledTrue();

    /**
     * Find all disabled users
     */
    List<User> findByEnabledFalse();

    /**
     * Count enabled users
     */
    long countByEnabledTrue();

    /**
     * Find users by role (legacy string-based roles)
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") String role);

    /**
     * Find locked accounts
     */
    List<User> findByAccountNonLockedFalse();

    /**
     * Find all administrators (SUPER_ADMIN or ADMIN)
     */
    @Query("SELECT u FROM User u WHERE u.primaryRole IN ('SUPER_ADMIN', 'ADMIN') AND u.enabled = true")
    List<User> findAllAdministrators();

    /**
     * Find all counselors
     */
    @Query("SELECT u FROM User u WHERE u.primaryRole = 'COUNSELOR' AND u.enabled = true")
    List<User> findAllCounselors();

    /**
     * Find all teachers
     */
    @Query("SELECT u FROM User u WHERE u.primaryRole = 'TEACHER' AND u.enabled = true")
    List<User> findAllTeachers();

    /**
     * Find users with failed login attempts >= threshold
     */
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :threshold")
    List<User> findUsersWithFailedLogins(@Param("threshold") int threshold);

    /**
     * Check if any super admin exists
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.primaryRole = 'SUPER_ADMIN'")
    boolean existsSuperAdmin();
}
