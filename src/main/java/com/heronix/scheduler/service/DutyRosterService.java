package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.DutyAssignment;
import com.heronix.scheduler.model.domain.Teacher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════
 * DUTY ROSTER SERVICE INTERFACE
 * Business logic for duty assignments
 * ═══════════════════════════════════════════════════════════════
 * 
 * Location: src/main/java/com/eduscheduler/service/DutyRosterService.java
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-02
 */
public interface DutyRosterService {

    // ═══════════════════════════════════════════════════════════════
    // CRUD OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create a new duty assignment
     */
    DutyAssignment createDuty(DutyAssignment duty);

    /**
     * Update an existing duty assignment
     */
    DutyAssignment updateDuty(Long id, DutyAssignment duty);

    /**
     * Delete a duty assignment
     */
    void deleteDuty(Long id);

    /**
     * Get duty by ID
     */
    DutyAssignment getDutyById(Long id);

    /**
     * Get all active duties
     */
    List<DutyAssignment> getAllActiveDuties();

    // ═══════════════════════════════════════════════════════════════
    // QUERY OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get duties for a specific teacher
     */
    List<DutyAssignment> getDutiesForTeacher(Long teacherId);

    /**
     * Get duties for a specific date
     */
    List<DutyAssignment> getDutiesForDate(LocalDate date);

    /**
     * Get duties in date range
     */
    List<DutyAssignment> getDutiesInDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Get duties by type (AM, PM, LUNCH, etc.)
     */
    List<DutyAssignment> getDutiesByType(String dutyType);

    /**
     * Get duties by location
     */
    List<DutyAssignment> getDutiesByLocation(String location);

    // ═══════════════════════════════════════════════════════════════
    // AUTOMATIC GENERATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Auto-generate duties for a date range with fair distribution
     */
    List<DutyAssignment> generateDutiesForDateRange(
            LocalDate startDate,
            LocalDate endDate,
            Long scheduleId);

    /**
     * Generate rotating duty schedule
     */
    List<DutyAssignment> generateRotatingSchedule(
            List<Teacher> teachers,
            LocalDate startDate,
            int weeks,
            String dutyType,
            String location);

    // ═══════════════════════════════════════════════════════════════
    // CONFLICT DETECTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if a duty assignment has conflicts
     */
    boolean hasConflicts(DutyAssignment duty);

    /**
     * Get conflicting duties for a teacher on a specific date/time
     */
    List<DutyAssignment> getConflicts(Long teacherId, LocalDate date,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime);

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS & REPORTING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get duty count per teacher in date range
     */
    Map<Teacher, Long> getDutyCountByTeacher(LocalDate startDate, LocalDate endDate);

    /**
     * Get duty distribution statistics
     */
    Map<String, Object> getDutyStatistics(LocalDate startDate, LocalDate endDate);

    /**
     * Get teacher duty workload balance score (0-100, 100 = perfectly balanced)
     */
    double getDutyBalanceScore(LocalDate startDate, LocalDate endDate);

    // ═══════════════════════════════════════════════════════════════
    // SUBSTITUTE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Assign a substitute for a duty
     */
    DutyAssignment assignSubstitute(Long dutyId, Long substituteTeacherId);

    /**
     * Remove substitute and restore original teacher
     */
    DutyAssignment removeSubstitute(Long dutyId);

    /**
     * Get all substitute duties
     */
    List<DutyAssignment> getSubstituteDuties();

    // ═══════════════════════════════════════════════════════════════
    // RECURRING DUTIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create recurring duty assignment
     */
    DutyAssignment createRecurringDuty(
            Teacher teacher,
            String dutyType,
            String location,
            Integer dayOfWeek,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            String recurrencePattern);

    /**
     * Get all recurring duties
     */
    List<DutyAssignment> getRecurringDuties();

    /**
     * Generate instances of recurring duties for a date range
     */
    List<DutyAssignment> generateRecurringInstances(LocalDate startDate, LocalDate endDate);
}