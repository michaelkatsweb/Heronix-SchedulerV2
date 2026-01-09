package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.LunchWave;
import com.heronix.scheduler.model.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for LunchWave entities
 *
 * Provides database access for multiple lunch period management
 *
 * Phase 5A: Multiple Rotating Lunch Periods
 * Date: December 1, 2025
 */
@Repository
public interface LunchWaveRepository extends JpaRepository<LunchWave, Long> {

    /**
     * Find all lunch waves for a specific schedule
     * Ordered by wave order (1, 2, 3)
     */
    List<LunchWave> findByScheduleOrderByWaveOrderAsc(Schedule schedule);

    /**
     * Find all lunch waves for a schedule ID
     */
    @Query("SELECT lw FROM LunchWave lw WHERE lw.schedule.id = :scheduleId ORDER BY lw.waveOrder")
    List<LunchWave> findByScheduleIdOrderByWaveOrderAsc(@Param("scheduleId") Long scheduleId);

    /**
     * Find active lunch waves for a schedule
     */
    @Query("SELECT lw FROM LunchWave lw WHERE lw.schedule.id = :scheduleId AND lw.isActive = true ORDER BY lw.waveOrder")
    List<LunchWave> findActiveByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * Find a specific lunch wave by name within a schedule
     */
    Optional<LunchWave> findByScheduleAndWaveName(Schedule schedule, String waveName);

    /**
     * Find lunch waves for a specific grade level
     */
    @Query("SELECT lw FROM LunchWave lw WHERE lw.schedule.id = :scheduleId AND lw.gradeLevelRestriction = :gradeLevel ORDER BY lw.waveOrder")
    List<LunchWave> findByScheduleIdAndGradeLevel(@Param("scheduleId") Long scheduleId, @Param("gradeLevel") Integer gradeLevel);

    /**
     * Find lunch waves with available capacity
     */
    @Query("SELECT lw FROM LunchWave lw WHERE lw.schedule.id = :scheduleId AND lw.isActive = true AND (lw.maxCapacity IS NULL OR lw.currentAssignments < lw.maxCapacity) ORDER BY lw.waveOrder")
    List<LunchWave> findAvailableByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * Find lunch waves at or over capacity
     */
    @Query("SELECT lw FROM LunchWave lw WHERE lw.schedule.id = :scheduleId AND lw.maxCapacity IS NOT NULL AND lw.currentAssignments >= lw.maxCapacity ORDER BY lw.waveOrder")
    List<LunchWave> findFullByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * Count lunch waves for a schedule
     */
    long countBySchedule(Schedule schedule);

    /**
     * Count active lunch waves for a schedule
     */
    @Query("SELECT COUNT(lw) FROM LunchWave lw WHERE lw.schedule.id = :scheduleId AND lw.isActive = true")
    long countActiveByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * Get total capacity across all lunch waves for a schedule
     */
    @Query("SELECT COALESCE(SUM(lw.maxCapacity), 0) FROM LunchWave lw WHERE lw.schedule.id = :scheduleId AND lw.isActive = true")
    Integer getTotalCapacityByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * Get total current assignments across all lunch waves
     */
    @Query("SELECT COALESCE(SUM(lw.currentAssignments), 0) FROM LunchWave lw WHERE lw.schedule.id = :scheduleId AND lw.isActive = true")
    Integer getTotalAssignmentsByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * Find the lunch wave with the most available capacity
     */
    @Query("SELECT lw FROM LunchWave lw WHERE lw.schedule.id = :scheduleId AND lw.isActive = true AND (lw.maxCapacity IS NULL OR lw.currentAssignments < lw.maxCapacity) ORDER BY (lw.maxCapacity - lw.currentAssignments) DESC, lw.waveOrder ASC LIMIT 1")
    Optional<LunchWave> findWaveWithMostCapacity(@Param("scheduleId") Long scheduleId);

    /**
     * Find lunch waves that can accommodate a specific grade level
     */
    @Query("SELECT lw FROM LunchWave lw WHERE lw.schedule.id = :scheduleId AND lw.isActive = true AND (lw.gradeLevelRestriction IS NULL OR lw.gradeLevelRestriction = :gradeLevel) AND (lw.maxCapacity IS NULL OR lw.currentAssignments < lw.maxCapacity) ORDER BY lw.waveOrder")
    List<LunchWave> findAvailableForGradeLevel(@Param("scheduleId") Long scheduleId, @Param("gradeLevel") Integer gradeLevel);

    /**
     * Check if a schedule has any lunch waves defined
     */
    boolean existsBySchedule(Schedule schedule);

    /**
     * Delete all lunch waves for a schedule
     */
    void deleteBySchedule(Schedule schedule);
}
