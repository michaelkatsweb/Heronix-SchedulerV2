package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.DutyAssignment;
import com.heronix.scheduler.model.domain.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════
 * DUTY ASSIGNMENT REPOSITORY
 * Database operations for duty assignments
 * ═══════════════════════════════════════════════════════════════
 * 
 * Location:
 * src/main/java/com/eduscheduler/repository/DutyAssignmentRepository.java
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-02
 */
@Repository
public interface DutyAssignmentRepository extends JpaRepository<DutyAssignment, Long> {

    /**
     * Find all duties for a specific teacher
     */
    List<DutyAssignment> findByTeacherId(Long teacherId);

    /**
     * Find all duties for a specific date
     */
    List<DutyAssignment> findByDutyDate(LocalDate date);

    /**
     * Find duties by date range
     */
    @Query("SELECT d FROM DutyAssignment d WHERE d.dutyDate BETWEEN :startDate AND :endDate AND d.isActive = true")
    List<DutyAssignment> findByDateRange(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find duties by type
     */
    List<DutyAssignment> findByDutyTypeAndIsActiveTrue(String dutyType);

    /**
     * Find active duties for a teacher on a specific date
     */
    @Query("SELECT d FROM DutyAssignment d WHERE d.teacher.id = :teacherId " +
            "AND d.dutyDate = :date AND d.isActive = true")
    List<DutyAssignment> findByTeacherAndDate(@Param("teacherId") Long teacherId,
            @Param("date") LocalDate date);

    /**
     * Find duties by location
     */
    List<DutyAssignment> findByDutyLocationAndIsActiveTrue(String location);

    /**
     * Find recurring duties
     */
    List<DutyAssignment> findByIsRecurringTrueAndIsActiveTrue();

    /**
     * Find substitute duties
     */
    List<DutyAssignment> findByIsSubstituteTrueAndIsActiveTrue();

    /**
     * Find duties for a teacher in a date range
     */
    @Query("SELECT d FROM DutyAssignment d WHERE d.teacher.id = :teacherId " +
            "AND d.dutyDate BETWEEN :startDate AND :endDate AND d.isActive = true " +
            "ORDER BY d.dutyDate, d.startTime")
    List<DutyAssignment> findByTeacherAndDateRange(
            @Param("teacherId") Long teacherId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find conflicting duties (same teacher, overlapping time)
     */
    @Query("SELECT d FROM DutyAssignment d WHERE d.teacher.id = :teacherId " +
            "AND d.dutyDate = :date AND d.isActive = true " +
            "AND ((d.startTime <= :endTime AND d.endTime >= :startTime))")
    List<DutyAssignment> findConflictingDuties(
            @Param("teacherId") Long teacherId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    /**
     * Count duties for a teacher in a date range
     */
    @Query("SELECT COUNT(d) FROM DutyAssignment d WHERE d.teacher.id = :teacherId " +
            "AND d.dutyDate BETWEEN :startDate AND :endDate AND d.isActive = true")
    long countByTeacherAndDateRange(
            @Param("teacherId") Long teacherId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find duties by schedule
     */
    List<DutyAssignment> findByScheduleIdAndIsActiveTrue(Long scheduleId);

    /**
     * Find all active duties ordered by date and time
     */
    @Query("SELECT d FROM DutyAssignment d WHERE d.isActive = true " +
            "ORDER BY d.dutyDate, d.startTime")
    List<DutyAssignment> findAllActiveDutiesOrdered();

    /**
     * Find duties for a specific day of week (for recurring duties)
     */
    @Query("SELECT d FROM DutyAssignment d WHERE d.dayOfWeek = :dayOfWeek " +
            "AND d.isRecurring = true AND d.isActive = true")
    List<DutyAssignment> findRecurringDutiesByDayOfWeek(@Param("dayOfWeek") Integer dayOfWeek);
}
