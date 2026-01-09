package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.LunchWave;
import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.dto.ScheduleGenerationRequest;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing lunch waves (multiple rotating lunch periods)
 *
 * Supports schools with multiple lunch periods to handle large student populations
 * within cafeteria capacity constraints.
 *
 * Real-world examples:
 * - Weeki Wachee HS: 3 lunch waves for 1,600 students (250 capacity each)
 * - Parrott MS: Grade-level lunches (6th, 7th, 8th grades)
 *
 * Phase 5B: Multiple Rotating Lunch Periods - Service Layer
 * Date: December 1, 2025
 */
public interface LunchWaveService {

    // ========== Creation Methods ==========

    /**
     * Create lunch waves for a schedule based on generation request configuration
     *
     * @param schedule The schedule to create lunch waves for
     * @param request The generation request containing lunch wave configuration
     * @return List of created lunch waves
     */
    List<LunchWave> createLunchWavesForSchedule(Schedule schedule, ScheduleGenerationRequest request);

    /**
     * Create a single lunch wave
     *
     * @param schedule The schedule this wave belongs to
     * @param name Display name (e.g., "Lunch 1", "6th Grade Lunch")
     * @param order Wave order (1, 2, 3) for split period sequencing
     * @param startTime Start time of lunch period
     * @param endTime End time of lunch period
     * @param maxCapacity Maximum students allowed (cafeteria capacity)
     * @return Created lunch wave
     */
    LunchWave createLunchWave(
        Schedule schedule,
        String name,
        int order,
        LocalTime startTime,
        LocalTime endTime,
        int maxCapacity
    );

    /**
     * Create a lunch wave with grade level restriction
     *
     * @param schedule The schedule this wave belongs to
     * @param name Display name
     * @param order Wave order
     * @param startTime Start time
     * @param endTime End time
     * @param maxCapacity Maximum capacity
     * @param gradeLevel Grade level restriction (6-12, or null for all grades)
     * @return Created lunch wave
     */
    LunchWave createLunchWave(
        Schedule schedule,
        String name,
        int order,
        LocalTime startTime,
        LocalTime endTime,
        int maxCapacity,
        Integer gradeLevel
    );

    // ========== Template Methods ==========

    /**
     * Create lunch waves using Weeki Wachee High School template
     * 3 lunch waves, 30 minutes each, 250 capacity
     * - Lunch 1: 10:04-10:34
     * - Lunch 2: 10:58-11:28
     * - Lunch 3: 11:52-12:22
     *
     * @param schedule The schedule to create waves for
     * @return List of 3 created lunch waves
     */
    List<LunchWave> createWeekiWacheeTemplate(Schedule schedule);

    /**
     * Create lunch waves using Parrott Middle School template
     * 3 grade-level lunches, 30 minutes each, 300 capacity
     * - 6th Grade: 11:00-11:30
     * - 7th Grade: 11:35-12:05
     * - 8th Grade: 12:10-12:40
     *
     * @param schedule The schedule to create waves for
     * @return List of 3 created lunch waves with grade restrictions
     */
    List<LunchWave> createParrottMSTemplate(Schedule schedule);

    /**
     * Create custom lunch waves with even spacing
     *
     * @param schedule The schedule
     * @param count Number of lunch waves (1-6)
     * @param firstLunchStart Start time of first lunch
     * @param lunchDuration Duration of each lunch in minutes
     * @param gapBetweenLunches Gap between lunch waves in minutes
     * @param capacity Cafeteria capacity per wave
     * @return List of created lunch waves
     */
    List<LunchWave> createCustomLunchWaves(
        Schedule schedule,
        int count,
        LocalTime firstLunchStart,
        int lunchDuration,
        int gapBetweenLunches,
        int capacity
    );

    // ========== Update Methods ==========

    /**
     * Update an existing lunch wave
     *
     * @param wave The lunch wave to update
     * @return Updated lunch wave
     */
    LunchWave updateLunchWave(LunchWave wave);

    /**
     * Update lunch wave capacity
     *
     * @param waveId Lunch wave ID
     * @param newCapacity New maximum capacity
     * @return Updated lunch wave
     */
    LunchWave updateCapacity(Long waveId, int newCapacity);

    /**
     * Update lunch wave times
     *
     * @param waveId Lunch wave ID
     * @param newStartTime New start time
     * @param newEndTime New end time
     * @return Updated lunch wave
     */
    LunchWave updateTimes(Long waveId, LocalTime newStartTime, LocalTime newEndTime);

    /**
     * Activate a lunch wave
     *
     * @param waveId Lunch wave ID
     * @return Updated lunch wave
     */
    LunchWave activateWave(Long waveId);

    /**
     * Deactivate a lunch wave (students can no longer be assigned)
     *
     * @param waveId Lunch wave ID
     * @return Updated lunch wave
     */
    LunchWave deactivateWave(Long waveId);

    // ========== Delete Methods ==========

    /**
     * Delete a lunch wave
     * Note: This will cascade delete all student/teacher assignments
     *
     * @param waveId Lunch wave ID
     */
    void deleteLunchWave(Long waveId);

    /**
     * Delete all lunch waves for a schedule
     *
     * @param scheduleId Schedule ID
     */
    void deleteAllLunchWaves(Long scheduleId);

    // ========== Query Methods ==========

    /**
     * Get all lunch waves for a schedule, ordered by wave order
     *
     * @param scheduleId Schedule ID
     * @return List of lunch waves
     */
    List<LunchWave> getAllLunchWaves(Long scheduleId);

    /**
     * Get only active lunch waves for a schedule
     *
     * @param scheduleId Schedule ID
     * @return List of active lunch waves
     */
    List<LunchWave> getActiveLunchWaves(Long scheduleId);

    /**
     * Get a specific lunch wave by ID
     *
     * @param waveId Lunch wave ID
     * @return Lunch wave if found
     */
    Optional<LunchWave> getLunchWaveById(Long waveId);

    /**
     * Find lunch wave by name within a schedule
     *
     * @param scheduleId Schedule ID
     * @param waveName Wave name (e.g., "Lunch 1")
     * @return Lunch wave if found
     */
    Optional<LunchWave> findLunchWaveByName(Long scheduleId, String waveName);

    /**
     * Find lunch waves with available capacity
     *
     * @param scheduleId Schedule ID
     * @return List of waves that can accept more students
     */
    List<LunchWave> getAvailableLunchWaves(Long scheduleId);

    /**
     * Find lunch waves at or over capacity
     *
     * @param scheduleId Schedule ID
     * @return List of full lunch waves
     */
    List<LunchWave> getFullLunchWaves(Long scheduleId);

    /**
     * Find lunch wave with most available capacity
     *
     * @param scheduleId Schedule ID
     * @return Lunch wave with most seats available
     */
    Optional<LunchWave> findWaveWithMostCapacity(Long scheduleId);

    /**
     * Find lunch waves available for a specific grade level
     *
     * @param scheduleId Schedule ID
     * @param gradeLevel Student grade level (6-12)
     * @return List of waves that accept this grade level and have capacity
     */
    List<LunchWave> getAvailableWavesForGradeLevel(Long scheduleId, Integer gradeLevel);

    // ========== Statistics Methods ==========

    /**
     * Get total cafeteria capacity across all active lunch waves
     *
     * @param scheduleId Schedule ID
     * @return Total capacity (sum of all max_capacity values)
     */
    int getTotalCapacity(Long scheduleId);

    /**
     * Get total current assignments across all active lunch waves
     *
     * @param scheduleId Schedule ID
     * @return Total assignments (sum of all current_assignments values)
     */
    int getTotalAssignments(Long scheduleId);

    /**
     * Get overall utilization percentage across all lunch waves
     *
     * @param scheduleId Schedule ID
     * @return Percentage (0-100) of capacity used
     */
    double getOverallUtilization(Long scheduleId);

    /**
     * Count active lunch waves for a schedule
     *
     * @param scheduleId Schedule ID
     * @return Number of active lunch waves
     */
    long countActiveLunchWaves(Long scheduleId);

    /**
     * Check if a schedule has any lunch waves defined
     *
     * @param scheduleId Schedule ID
     * @return true if at least one lunch wave exists
     */
    boolean hasLunchWaves(Long scheduleId);

    /**
     * Check if lunch waves are properly configured for schedule generation
     * Validates:
     * - At least one active wave exists
     * - Total capacity is sufficient for student population
     * - No time overlaps between waves
     * - Wave order is sequential
     *
     * @param scheduleId Schedule ID
     * @return true if configuration is valid
     */
    boolean isLunchWaveConfigurationValid(Long scheduleId);
}
