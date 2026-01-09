package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.ConflictSeverity;

import java.util.List;

/**
 * Conflict Detector Service Interface
 * Defines methods for detecting scheduling conflicts
 *
 * Location: src/main/java/com/eduscheduler/service/ConflictDetectorService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7B - Conflict Detection
 */
public interface ConflictDetectorService {

    // ========================================================================
    // COMPREHENSIVE DETECTION
    // ========================================================================

    /**
     * Detect all conflicts in a schedule
     * @param schedule The schedule to analyze
     * @return List of detected conflicts
     */
    List<Conflict> detectAllConflicts(Schedule schedule);

    /**
     * Detect conflicts for a specific schedule slot
     * @param slot The schedule slot to analyze
     * @return List of detected conflicts
     */
    List<Conflict> detectConflictsForSlot(ScheduleSlot slot);

    /**
     * Quick conflict check - returns true if any conflicts exist
     * @param schedule The schedule to check
     * @return true if conflicts exist, false otherwise
     */
    boolean hasConflicts(Schedule schedule);

    // ========================================================================
    // TIME-BASED DETECTION
    // ========================================================================

    /**
     * Detect time overlap conflicts
     * @param schedule The schedule to analyze
     * @return List of time overlap conflicts
     */
    List<Conflict> detectTimeOverlaps(Schedule schedule);

    /**
     * Detect back-to-back violations (no break between classes)
     * @param schedule The schedule to analyze
     * @return List of back-to-back violations
     */
    List<Conflict> detectBackToBackViolations(Schedule schedule);

    /**
     * Detect missing lunch breaks
     * @param schedule The schedule to analyze
     * @return List of missing lunch break conflicts
     */
    List<Conflict> detectMissingLunchBreaks(Schedule schedule);

    /**
     * Detect excessive consecutive classes
     * @param schedule The schedule to analyze
     * @return List of excessive consecutive class conflicts
     */
    List<Conflict> detectExcessiveConsecutiveClasses(Schedule schedule);

    // ========================================================================
    // ROOM-BASED DETECTION
    // ========================================================================

    /**
     * Detect room double-booking conflicts
     * @param schedule The schedule to analyze
     * @return List of room double-booking conflicts
     */
    List<Conflict> detectRoomDoubleBookings(Schedule schedule);

    /**
     * Detect room capacity violations
     * @param schedule The schedule to analyze
     * @return List of room capacity conflicts
     */
    List<Conflict> detectRoomCapacityViolations(Schedule schedule);

    /**
     * Detect room type mismatches
     * @param schedule The schedule to analyze
     * @return List of room type mismatch conflicts
     */
    List<Conflict> detectRoomTypeMismatches(Schedule schedule);

    /**
     * Detect equipment unavailability
     * @param schedule The schedule to analyze
     * @return List of equipment unavailability conflicts
     */
    List<Conflict> detectEquipmentUnavailability(Schedule schedule);

    // ========================================================================
    // TEACHER-BASED DETECTION
    // ========================================================================

    /**
     * Detect teacher overload conflicts (same teacher, multiple classes)
     * @param schedule The schedule to analyze
     * @return List of teacher overload conflicts
     */
    List<Conflict> detectTeacherOverloads(Schedule schedule);

    /**
     * Detect excessive teaching hours
     * @param schedule The schedule to analyze
     * @return List of excessive teaching hour conflicts
     */
    List<Conflict> detectExcessiveTeachingHours(Schedule schedule);

    /**
     * Detect missing preparation periods
     * @param schedule The schedule to analyze
     * @return List of missing preparation period conflicts
     */
    List<Conflict> detectMissingPreparationPeriods(Schedule schedule);

    /**
     * Detect subject mismatches (teacher teaching outside subject area)
     * @param schedule The schedule to analyze
     * @return List of subject mismatch conflicts
     */
    List<Conflict> detectSubjectMismatches(Schedule schedule);

    /**
     * Detect insufficient teacher travel time between buildings
     * @param schedule The schedule to analyze
     * @return List of teacher travel time conflicts
     */
    List<Conflict> detectTeacherTravelTimeIssues(Schedule schedule);

    // ========================================================================
    // STUDENT-BASED DETECTION
    // ========================================================================

    /**
     * Detect student schedule conflicts (same student, multiple classes)
     * @param schedule The schedule to analyze
     * @return List of student schedule conflicts
     */
    List<Conflict> detectStudentScheduleConflicts(Schedule schedule);

    /**
     * Detect prerequisite violations
     * @param schedule The schedule to analyze
     * @return List of prerequisite violation conflicts
     */
    List<Conflict> detectPrerequisiteViolations(Schedule schedule);

    /**
     * Detect credit hour violations
     * @param schedule The schedule to analyze
     * @return List of credit hour violation conflicts
     */
    List<Conflict> detectCreditHourViolations(Schedule schedule);

    /**
     * Detect graduation requirement issues
     * @param schedule The schedule to analyze
     * @return List of graduation requirement conflicts
     */
    List<Conflict> detectGraduationRequirementIssues(Schedule schedule);

    /**
     * Detect course sequence violations
     * @param schedule The schedule to analyze
     * @return List of course sequence violation conflicts
     */
    List<Conflict> detectCourseSequenceViolations(Schedule schedule);

    // ========================================================================
    // COURSE-BASED DETECTION
    // ========================================================================

    /**
     * Detect section over-enrollment
     * @param schedule The schedule to analyze
     * @return List of over-enrollment conflicts
     */
    List<Conflict> detectSectionOverEnrollment(Schedule schedule);

    /**
     * Detect section under-enrollment
     * @param schedule The schedule to analyze
     * @return List of under-enrollment conflicts
     */
    List<Conflict> detectSectionUnderEnrollment(Schedule schedule);

    /**
     * Detect duplicate enrollments
     * @param schedule The schedule to analyze
     * @return List of duplicate enrollment conflicts
     */
    List<Conflict> detectDuplicateEnrollments(Schedule schedule);

    /**
     * Detect co-requisite violations
     * @param schedule The schedule to analyze
     * @return List of co-requisite violation conflicts
     */
    List<Conflict> detectCoRequisiteViolations(Schedule schedule);

    // ========================================================================
    // REAL-TIME DETECTION
    // ========================================================================

    /**
     * Detect conflicts that would be created by adding a slot
     * @param schedule The schedule
     * @param potentialSlot The slot to potentially add
     * @return List of conflicts that would be created
     */
    List<Conflict> detectPotentialConflicts(Schedule schedule, ScheduleSlot potentialSlot);

    /**
     * Validate a schedule before publication
     * @param schedule The schedule to validate
     * @return ValidationResult with all conflicts and severity levels
     */
    ValidationResult validateSchedule(Schedule schedule);

    // ========================================================================
    // CONFLICT PERSISTENCE
    // ========================================================================

    /**
     * Save detected conflicts to database
     * @param conflicts The conflicts to save
     * @return List of saved conflicts
     */
    List<Conflict> saveConflicts(List<Conflict> conflicts);

    /**
     * Clear all conflicts for a schedule
     * @param schedule The schedule
     */
    void clearConflicts(Schedule schedule);

    /**
     * Refresh conflicts for a schedule (clear and re-detect)
     * @param schedule The schedule
     * @return List of newly detected conflicts
     */
    List<Conflict> refreshConflicts(Schedule schedule);

    // ========================================================================
    // VALIDATION RESULT
    // ========================================================================

    /**
     * Validation result containing conflicts and summary
     */
    class ValidationResult {
        private List<Conflict> conflicts;
        private long criticalCount;
        private long highCount;
        private long mediumCount;
        private long lowCount;
        private long infoCount;
        private boolean isValid;

        public ValidationResult(List<Conflict> conflicts) {
            this.conflicts = conflicts;
            this.criticalCount = conflicts.stream().filter(c -> c.getSeverity() == ConflictSeverity.CRITICAL).count();
            this.highCount = conflicts.stream().filter(c -> c.getSeverity() == ConflictSeverity.HIGH).count();
            this.mediumCount = conflicts.stream().filter(c -> c.getSeverity() == ConflictSeverity.MEDIUM).count();
            this.lowCount = conflicts.stream().filter(c -> c.getSeverity() == ConflictSeverity.LOW).count();
            this.infoCount = conflicts.stream().filter(c -> c.getSeverity() == ConflictSeverity.INFO).count();
            this.isValid = criticalCount == 0; // Valid if no critical conflicts
        }

        // Getters
        public List<Conflict> getConflicts() { return conflicts; }
        public long getCriticalCount() { return criticalCount; }
        public long getHighCount() { return highCount; }
        public long getMediumCount() { return mediumCount; }
        public long getLowCount() { return lowCount; }
        public long getInfoCount() { return infoCount; }
        public boolean isValid() { return isValid; }
        public long getTotalCount() { return conflicts.size(); }

        public String getSummary() {
            return String.format("Total: %d | Critical: %d | High: %d | Medium: %d | Low: %d | Info: %d",
                getTotalCount(), criticalCount, highCount, mediumCount, lowCount, infoCount);
        }
    }
}
