package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Substitute;
import com.heronix.scheduler.model.enums.SubstituteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Substitute entity
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Repository
public interface SubstituteRepository extends JpaRepository<Substitute, Long> {

    /**
     * Find substitute by employee ID
     */
    Optional<Substitute> findByEmployeeId(String employeeId);

    /**
     * Find all active substitutes
     */
    List<Substitute> findByActiveTrue();

    /**
     * Find all inactive substitutes
     */
    List<Substitute> findByActiveFalse();

    /**
     * Find substitutes by type
     */
    List<Substitute> findByType(SubstituteType type);

    /**
     * Find active substitutes by type
     */
    List<Substitute> findByTypeAndActiveTrue(SubstituteType type);

    /**
     * Find substitutes by first or last name (case-insensitive)
     */
    @Query("SELECT s FROM Substitute s WHERE LOWER(s.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Substitute> findByNameContaining(@Param("name") String name);

    /**
     * Find active substitutes by first or last name
     */
    @Query("SELECT s FROM Substitute s WHERE s.active = true AND " +
           "(LOWER(s.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Substitute> findActiveByNameContaining(@Param("name") String name);

    /**
     * Find substitutes with specific certification
     */
    @Query("SELECT s FROM Substitute s JOIN s.certifications c WHERE c = :certification")
    List<Substitute> findByCertification(@Param("certification") String certification);

    /**
     * Find active substitutes with specific certification
     */
    @Query("SELECT s FROM Substitute s JOIN s.certifications c WHERE s.active = true AND c = :certification")
    List<Substitute> findActiveByCertification(@Param("certification") String certification);

    /**
     * Find substitutes by email
     */
    Optional<Substitute> findByEmail(String email);

    /**
     * Find substitutes by phone number
     */
    Optional<Substitute> findByPhoneNumber(String phoneNumber);

    /**
     * Count all active substitutes
     */
    long countByActiveTrue();

    /**
     * Count substitutes by type
     */
    long countByType(SubstituteType type);

    /**
     * Count active substitutes by type
     */
    long countByTypeAndActiveTrue(SubstituteType type);

    /**
     * Check if substitute exists by employee ID
     */
    boolean existsByEmployeeId(String employeeId);

    /**
     * Check if substitute exists by email
     */
    boolean existsByEmail(String email);

    /**
     * Find all substitutes with certifications eagerly loaded
     * Use this to prevent LazyInitializationException when displaying in UI
     */
    @Query("SELECT DISTINCT s FROM Substitute s LEFT JOIN FETCH s.certifications")
    List<Substitute> findAllWithCertifications();

    /**
     * Find substitute by ID with certifications eagerly loaded
     * Use this to prevent LazyInitializationException when displaying in UI
     */
    @Query("SELECT DISTINCT s FROM Substitute s LEFT JOIN FETCH s.certifications WHERE s.id = :id")
    Optional<Substitute> findByIdWithCertifications(@Param("id") Long id);
}
