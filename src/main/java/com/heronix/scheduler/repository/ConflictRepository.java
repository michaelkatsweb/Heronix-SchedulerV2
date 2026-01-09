package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Conflict;
import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.enums.ConflictSeverity;
import com.heronix.scheduler.model.enums.ConflictType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Conflict Repository
 * Data access layer for Conflict entities
 *
 * Location: src/main/java/com/eduscheduler/repository/ConflictRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7B - Conflict Detection
 */
@Repository
public interface ConflictRepository extends JpaRepository<Conflict, Long> {

    // ========================================================================
    // BASIC QUERIES
    // ========================================================================

    /**
     * Find all conflicts for a schedule
     */
    List<Conflict> findBySchedule(Schedule schedule);

    /**
     * Find all conflicts for a schedule ordered by severity
     */
    @Query("SELECT c FROM Conflict c WHERE c.schedule = :schedule ORDER BY c.severity DESC, c.detectedAt DESC")
    List<Conflict> findByScheduleOrderBySeverity(@Param("schedule") Schedule schedule);

    /**
     * Find active conflicts (not resolved and not ignored)
     */
    @Query("SELECT c FROM Conflict c WHERE c.isResolved = false AND c.isIgnored = false")
    List<Conflict> findAllActive();

    /**
     * Find active conflicts for a schedule
     */
    @Query("SELECT c FROM Conflict c WHERE c.schedule = :schedule AND c.isResolved = false AND c.isIgnored = false")
    List<Conflict> findActiveBySchedule(@Param("schedule") Schedule schedule);

    // ========================================================================
    // SEVERITY QUERIES
    // ========================================================================

    /**
     * Find conflicts by severity
     */
    List<Conflict> findBySeverity(ConflictSeverity severity);

    /**
     * Find conflicts by severity for a schedule
     */
    List<Conflict> findByScheduleAndSeverity(Schedule schedule, ConflictSeverity severity);

    /**
     * Find critical conflicts
     */
    @Query("SELECT c FROM Conflict c WHERE c.severity = 'CRITICAL' AND c.isResolved = false AND c.isIgnored = false")
    List<Conflict> findAllCritical();

    /**
     * Find critical conflicts for a schedule
     */
    @Query("SELECT c FROM Conflict c WHERE c.schedule = :schedule AND c.severity = 'CRITICAL' AND c.isResolved = false AND c.isIgnored = false")
    List<Conflict> findCriticalBySchedule(@Param("schedule") Schedule schedule);

    // ========================================================================
    // TYPE QUERIES
    // ========================================================================

    /**
     * Find conflicts by type
     */
    List<Conflict> findByConflictType(ConflictType conflictType);

    /**
     * Find conflicts by category
     */
    List<Conflict> findByCategory(ConflictType.ConflictCategory category);

    /**
     * Find conflicts by type for a schedule
     */
    List<Conflict> findByScheduleAndConflictType(Schedule schedule, ConflictType conflictType);

    // ========================================================================
    // STATUS QUERIES
    // ========================================================================

    /**
     * Find resolved conflicts
     */
    List<Conflict> findByIsResolvedTrue();

    /**
     * Find unresolved conflicts
     */
    List<Conflict> findByIsResolvedFalse();

    /**
     * Find ignored conflicts
     */
    List<Conflict> findByIsIgnoredTrue();

    /**
     * Find conflicts resolved by a specific user
     */
    @Query("SELECT c FROM Conflict c WHERE c.resolvedBy.id = :userId")
    List<Conflict> findByResolvedByUserId(@Param("userId") Long userId);

    // ========================================================================
    // TIME-BASED QUERIES
    // ========================================================================

    /**
     * Find conflicts detected after a certain date
     */
    List<Conflict> findByDetectedAtAfter(LocalDateTime dateTime);

    /**
     * Find conflicts detected between dates
     */
    List<Conflict> findByDetectedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find recent conflicts (last N days)
     */
    @Query("SELECT c FROM Conflict c WHERE c.detectedAt >= :since ORDER BY c.detectedAt DESC")
    List<Conflict> findRecentConflicts(@Param("since") LocalDateTime since);

    // ========================================================================
    // COUNT QUERIES
    // ========================================================================

    /**
     * Count all active conflicts
     */
    @Query("SELECT COUNT(c) FROM Conflict c WHERE c.isResolved = false AND c.isIgnored = false")
    long countActive();

    /**
     * Count active conflicts for a schedule
     */
    @Query("SELECT COUNT(c) FROM Conflict c WHERE c.schedule = :schedule AND c.isResolved = false AND c.isIgnored = false")
    long countActiveBySchedule(@Param("schedule") Schedule schedule);

    /**
     * Count by severity
     */
    long countBySeverity(ConflictSeverity severity);

    /**
     * Count by severity for a schedule
     */
    long countByScheduleAndSeverity(Schedule schedule, ConflictSeverity severity);

    /**
     * Count critical conflicts for a schedule
     */
    @Query("SELECT COUNT(c) FROM Conflict c WHERE c.schedule = :schedule AND c.severity = 'CRITICAL' AND c.isResolved = false AND c.isIgnored = false")
    long countCriticalBySchedule(@Param("schedule") Schedule schedule);

    /**
     * Count by type
     */
    long countByConflictType(ConflictType conflictType);

    // ========================================================================
    // STATISTICS QUERIES
    // ========================================================================

    /**
     * Get conflict count by severity (for statistics)
     */
    @Query("SELECT c.severity, COUNT(c) FROM Conflict c WHERE c.schedule = :schedule AND c.isResolved = false AND c.isIgnored = false GROUP BY c.severity")
    List<Object[]> countBySeverityForSchedule(@Param("schedule") Schedule schedule);

    /**
     * Get conflict count by type (for statistics)
     */
    @Query("SELECT c.conflictType, COUNT(c) FROM Conflict c WHERE c.schedule = :schedule AND c.isResolved = false AND c.isIgnored = false GROUP BY c.conflictType")
    List<Object[]> countByTypeForSchedule(@Param("schedule") Schedule schedule);

    /**
     * Get conflict count by category (for statistics)
     */
    @Query("SELECT c.category, COUNT(c) FROM Conflict c WHERE c.schedule = :schedule AND c.isResolved = false AND c.isIgnored = false GROUP BY c.category")
    List<Object[]> countByCategoryForSchedule(@Param("schedule") Schedule schedule);

    // ========================================================================
    // ENTITY-SPECIFIC QUERIES
    // ========================================================================

    /**
     * Find conflicts involving a specific teacher
     */
    @Query("SELECT c FROM Conflict c JOIN c.affectedTeachers t WHERE t.id = :teacherId AND c.isResolved = false AND c.isIgnored = false")
    List<Conflict> findActiveByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * Find conflicts involving a specific student
     */
    @Query("SELECT c FROM Conflict c JOIN c.affectedStudents s WHERE s.id = :studentId AND c.isResolved = false AND c.isIgnored = false")
    List<Conflict> findActiveByStudentId(@Param("studentId") Long studentId);

    /**
     * Find conflicts involving a specific room
     */
    @Query("SELECT c FROM Conflict c JOIN c.affectedRooms r WHERE r.id = :roomId AND c.isResolved = false AND c.isIgnored = false")
    List<Conflict> findActiveByRoomId(@Param("roomId") Long roomId);

    /**
     * Find conflicts involving a specific course
     */
    @Query("SELECT c FROM Conflict c JOIN c.affectedCourses course WHERE course.id = :courseId AND c.isResolved = false AND c.isIgnored = false")
    List<Conflict> findActiveByCourseId(@Param("courseId") Long courseId);

    // ========================================================================
    // DELETE QUERIES
    // ========================================================================

    /**
     * Delete all conflicts for a schedule
     */
    void deleteBySchedule(Schedule schedule);

    /**
     * Delete resolved conflicts older than a certain date
     */
    @Query("DELETE FROM Conflict c WHERE c.isResolved = true AND c.resolvedAt < :date")
    void deleteResolvedOlderThan(@Param("date") LocalDateTime date);

    /**
     * Delete all resolved conflicts
     */
    void deleteByIsResolvedTrue();
}
