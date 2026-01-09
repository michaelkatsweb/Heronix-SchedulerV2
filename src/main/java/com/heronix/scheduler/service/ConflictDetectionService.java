package com.heronix.scheduler.service;

// ============================================================================
// FILE: ConflictDetectionService.java - UPDATED WITH ALL REQUIRED METHODS
// Location: src/main/java/com/eduscheduler/service/ConflictDetectionService.java
// ============================================================================

import com.heronix.scheduler.model.dto.Conflict;
import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.domain.ScheduleSlot;
import com.heronix.scheduler.model.domain.TimeSlot;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Service for detecting and resolving scheduling conflicts
 * 
 * ✅ UPDATED: Added all missing method signatures required by controllers
 * 
 * Location:
 * src/main/java/com/eduscheduler/service/ConflictDetectionService.java
 * 
 * @author Heronix Scheduling System Team
 * @version 2.0.0 - FIXED
 * @since 2025-10-30
 */
public interface ConflictDetectionService {

    /**
     * Detect all conflicts in a schedule (by schedule ID)
     * 
     * @param scheduleId The schedule ID to check
     * @return List of all conflicts found
     */
    List<Conflict> detectConflicts(Long scheduleId);

    /**
     * ✅ ADDED: Detect all conflicts for a Schedule object
     * This overload allows passing a Schedule entity directly
     * 
     * @param schedule The schedule object to check
     * @return List of all conflicts found
     */
    List<Conflict> detectConflicts(Schedule schedule);

    /**
     * Detect all conflicts and return as string descriptions (by Schedule object)
     * 
     * This method accepts a Schedule object and returns detailed conflict
     * descriptions
     * 
     * @param schedule The schedule object to check
     * @return List of conflict descriptions as strings
     */
    List<String> detectAllConflicts(Schedule schedule);

    /**
     * Check for conflicts in a specific slot
     * 
     * @param slotId The slot ID to check
     * @return List of conflicts for this slot
     */
    List<Conflict> checkSlotConflicts(Long slotId);

    /**
     * ✅ ADDED: Check for conflicts when moving a slot to a new time
     * 
     * @param slot    The slot to move
     * @param newTime The target time slot
     * @return List of conflicts that would be created
     */
    List<Conflict> checkMoveConflicts(ScheduleSlot slot, TimeSlot newTime);

    /**
     * Get resolution suggestions for a conflict
     * 
     * @param conflict The conflict to resolve
     * @return List of suggested resolutions
     */
    List<String> getResolutionSuggestions(Conflict conflict);

    /**
     * Attempt to auto-resolve conflicts
     * 
     * @param scheduleId The schedule ID
     * @return True if all conflicts were resolved
     */
    boolean autoResolveConflicts(Long scheduleId);

    /**
     * Check for teacher conflicts on specific day/time
     * 
     * @param teacherId Teacher ID
     * @param day       Day of week
     * @param start     Start time
     * @param end       End time
     * @return True if conflict exists
     */
    boolean hasTeacherConflict(Long teacherId, DayOfWeek day,
            LocalTime start, LocalTime end);

    /**
     * Check for room conflicts on specific day/time
     * 
     * @param roomId Room ID
     * @param day    Day of week
     * @param start  Start time
     * @param end    End time
     * @return True if conflict exists
     */
    boolean hasRoomConflict(Long roomId, DayOfWeek day,
            LocalTime start, LocalTime end);

    /**
     * Detect conflicts for manual override operations
     *
     * @param slot       The slot being modified
     * @param newTeacher The new teacher assignment
     * @param newRoom    The new room assignment
     * @return List of conflict descriptions
     */
    List<String> detectConflicts(ScheduleSlot slot,
            com.heronix.scheduler.model.domain.Teacher newTeacher,
            com.heronix.scheduler.model.domain.Room newRoom);

    /**
     * Check if teacher has conflict at specific period
     *
     * @param teacher      The teacher to check
     * @param periodNumber The period number
     * @param dayType      The day type (DAILY, ODD, EVEN)
     * @param scheduleId   The schedule ID to check within
     * @return True if conflict exists
     */
    boolean hasTeacherConflict(com.heronix.scheduler.model.domain.Teacher teacher,
            Integer periodNumber, String dayType, Long scheduleId);

    /**
     * Check if room has conflict at specific period
     *
     * @param room         The room to check
     * @param periodNumber The period number
     * @param dayType      The day type (DAILY, ODD, EVEN)
     * @param scheduleId   The schedule ID to check within
     * @return True if conflict exists
     */
    boolean hasRoomConflict(com.heronix.scheduler.model.domain.Room room,
            Integer periodNumber, String dayType, Long scheduleId);

    /**
     * Check if room capacity would be exceeded
     *
     * @param room               The room to check
     * @param currentEnrollment  The enrollment count
     * @return True if capacity exceeded
     */
    boolean hasCapacityConflict(com.heronix.scheduler.model.domain.Room room,
            Integer currentEnrollment);
}