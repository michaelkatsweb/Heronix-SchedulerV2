package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.LunchPeriod;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing lunch periods
 */
public interface LunchPeriodService {
    
    /**
     * Create a new lunch period
     */
    LunchPeriod createLunchPeriod(LunchPeriod lunchPeriod);
    
    /**
     * Get lunch period by ID
     */
    Optional<LunchPeriod> getLunchPeriodById(Long id);
    
    /**
     * Get all lunch periods
     */
    List<LunchPeriod> getAllLunchPeriods();
    
    /**
     * Get all active lunch periods
     */
    List<LunchPeriod> getActiveLunchPeriods();
    
    /**
     * Get lunch periods by lunch group
     */
    List<LunchPeriod> getLunchPeriodsByGroup(String lunchGroup);
    
    /**
     * Get lunch periods for specific day
     */
    List<LunchPeriod> getLunchPeriodsForDay(Integer dayOfWeek);
    
    /**
     * Get lunch periods for grade level
     */
    List<LunchPeriod> getLunchPeriodsForGradeLevel(String gradeLevel);
    
    /**
     * Update lunch period
     */
    LunchPeriod updateLunchPeriod(Long id, LunchPeriod lunchPeriod);
    
    /**
     * Delete lunch period (soft delete - set inactive)
     */
    void deleteLunchPeriod(Long id);
    
    /**
     * Activate lunch period
     */
    void activateLunchPeriod(Long id);
    
    /**
     * Check if lunch period exists
     */
    boolean existsById(Long id);
    
    /**
     * Check for time conflicts with existing lunch periods
     */
    boolean hasTimeConflict(LocalTime startTime, LocalTime endTime, String location);

    /**
     * Assign lunch periods to schedule based on configuration
     */
    void assignLunchPeriods(com.heronix.scheduler.model.domain.Schedule schedule,
            com.heronix.scheduler.model.domain.LunchConfiguration config);

    /**
     * Get lunch slots for a specific student
     */
    List<com.heronix.scheduler.model.domain.ScheduleSlot> getLunchSlotsForStudent(
            com.heronix.scheduler.model.domain.Student student);

    /**
     * Get lunch slots for a specific teacher
     */
    List<com.heronix.scheduler.model.domain.ScheduleSlot> getLunchSlotsForTeacher(
            com.heronix.scheduler.model.domain.Teacher teacher);

    /**
     * Get lunch distribution across waves
     */
    java.util.Map<Integer, List<com.heronix.scheduler.model.domain.Student>> getLunchDistribution(
            com.heronix.scheduler.model.domain.Schedule schedule);

    /**
     * Validate lunch capacity doesn't exceed limits
     */
    boolean validateLunchCapacity(com.heronix.scheduler.model.domain.Schedule schedule,
            com.heronix.scheduler.model.domain.LunchConfiguration config);

    /**
     * Stagger lunch periods by grade level
     */
    void staggerLunchByGrade(com.heronix.scheduler.model.domain.Schedule schedule,
            com.heronix.scheduler.model.domain.LunchConfiguration config);

    /**
     * Determine lunch wave for a student
     */
    Integer determineLunchWave(com.heronix.scheduler.model.domain.Student student,
            com.heronix.scheduler.model.domain.LunchConfiguration config);

    /**
     * Check if slot conflicts with lunch period
     */
    boolean hasLunchConflict(com.heronix.scheduler.model.domain.ScheduleSlot slot);
}