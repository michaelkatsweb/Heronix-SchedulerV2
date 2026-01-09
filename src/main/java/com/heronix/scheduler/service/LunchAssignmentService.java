package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.LunchAssignmentMethod;

import java.util.List;
import java.util.Optional;

/**
 * Service for assigning students and teachers to lunch waves
 *
 * Supports multiple assignment strategies:
 * - BY_GRADE_LEVEL: All 9th graders → Lunch 1, etc.
 * - ALPHABETICAL: A-H → Lunch 1, I-P → Lunch 2, Q-Z → Lunch 3
 * - BALANCED: Distribute evenly across waves
 * - RANDOM: Random distribution
 * - MANUAL: Admin assigns individually
 * - And more...
 *
 * Phase 5B: Multiple Rotating Lunch Periods - Service Layer
 * Date: December 1, 2025
 */
public interface LunchAssignmentService {

    // ========== Student Assignment Methods ==========

    /**
     * Assign all unassigned students to lunch waves using specified method
     *
     * @param scheduleId The schedule ID
     * @param method Assignment method to use
     * @return Number of students assigned
     */
    int assignStudentsToLunchWaves(Long scheduleId, LunchAssignmentMethod method);

    /**
     * Assign unassigned students from a specific campus to lunch waves
     *
     * @param scheduleId The schedule ID
     * @param campusId The campus ID to filter students by
     * @param method Assignment method to use
     * @return Number of students assigned
     */
    int assignStudentsToLunchWaves(Long scheduleId, Long campusId, LunchAssignmentMethod method);

    /**
     * Assign a single student to a lunch wave
     *
     * @param studentId The student ID to assign
     * @param lunchWaveId The lunch wave ID to assign to
     * @param username User performing the assignment
     * @return Created assignment
     */
    StudentLunchAssignment assignStudentToWave(Long studentId, Long lunchWaveId, String username);

    /**
     * Reassign a student to a different lunch wave
     *
     * @param assignmentId Current assignment ID
     * @param newWaveId New lunch wave ID
     * @param username User performing the reassignment
     * @return Updated assignment
     */
    StudentLunchAssignment reassignStudent(Long assignmentId, Long newWaveId, String username);

    /**
     * Remove a student's lunch assignment
     *
     * @param assignmentId Assignment ID to remove
     */
    void removeStudentAssignment(Long assignmentId);

    /**
     * Remove all student lunch assignments for a schedule
     *
     * @param scheduleId Schedule ID
     * @return Number of assignments removed
     */
    int removeAllStudentAssignments(Long scheduleId);

    // ========== Assignment Algorithm Methods ==========

    /**
     * Assign students by grade level
     * Example: All 9th → Lunch 1, All 10th → Lunch 2, etc.
     *
     * @param scheduleId The schedule ID
     * @return Number of students assigned
     */
    int assignStudentsByGradeLevel(Long scheduleId);

    /**
     * Assign students alphabetically by last name
     * Example: A-H → Lunch 1, I-P → Lunch 2, Q-Z → Lunch 3
     *
     * @param scheduleId The schedule ID
     * @return Number of students assigned
     */
    int assignStudentsAlphabetically(Long scheduleId);

    /**
     * Assign students randomly across lunch waves
     *
     * @param scheduleId The schedule ID
     * @return Number of students assigned
     */
    int assignStudentsRandomly(Long scheduleId);

    /**
     * Assign students to balance lunch wave capacities
     * Fills waves evenly, respecting grade restrictions
     *
     * @param scheduleId The schedule ID
     * @return Number of students assigned
     */
    int assignStudentsBalanced(Long scheduleId);

    /**
     * Assign students by student ID ranges
     * Example: IDs 1000-1999 → Lunch 1, 2000-2999 → Lunch 2
     *
     * @param scheduleId The schedule ID
     * @return Number of students assigned
     */
    int assignStudentsByStudentId(Long scheduleId);

    /**
     * Rebalance existing lunch assignments to optimize capacity usage
     * Only reassigns students who are not locked
     *
     * @param scheduleId The schedule ID
     * @return Number of students reassigned
     */
    int rebalanceLunchWaves(Long scheduleId);

    // ========== Student Query Methods ==========

    /**
     * Get all unassigned students for a schedule
     *
     * @param scheduleId Schedule ID
     * @return List of students without lunch assignments
     */
    List<Student> getUnassignedStudents(Long scheduleId);

    /**
     * Count unassigned students
     *
     * @param scheduleId Schedule ID
     * @return Number of unassigned students
     */
    long countUnassignedStudents(Long scheduleId);

    /**
     * Get student's lunch assignment
     *
     * @param studentId Student ID
     * @param scheduleId Schedule ID
     * @return Assignment if found
     */
    Optional<StudentLunchAssignment> getStudentAssignment(Long studentId, Long scheduleId);

    /**
     * Get all students assigned to a lunch wave (roster)
     *
     * @param lunchWaveId Lunch wave ID
     * @return List of assignments for this wave
     */
    List<StudentLunchAssignment> getWaveRoster(Long lunchWaveId);

    /**
     * Get all student assignments for a schedule
     *
     * @param scheduleId Schedule ID
     * @return List of all assignments
     */
    List<StudentLunchAssignment> getAllStudentAssignments(Long scheduleId);

    /**
     * Get manual assignments (admin-assigned or overridden)
     *
     * @param scheduleId Schedule ID
     * @return List of manual assignments
     */
    List<StudentLunchAssignment> getManualAssignments(Long scheduleId);

    /**
     * Get locked assignments (cannot be auto-reassigned)
     *
     * @param scheduleId Schedule ID
     * @return List of locked assignments
     */
    List<StudentLunchAssignment> getLockedAssignments(Long scheduleId);

    // ========== Assignment Management ==========

    /**
     * Lock an assignment to prevent automatic reassignment
     *
     * @param assignmentId Assignment ID
     * @param username User performing the lock
     */
    void lockAssignment(Long assignmentId, String username);

    /**
     * Unlock an assignment to allow automatic reassignment
     *
     * @param assignmentId Assignment ID
     * @param username User performing the unlock
     */
    void unlockAssignment(Long assignmentId, String username);

    /**
     * Set assignment priority
     *
     * @param assignmentId Assignment ID
     * @param priority Priority level (1-10)
     * @param username User setting priority
     */
    void setAssignmentPriority(Long assignmentId, int priority, String username);

    /**
     * Mark assignment as manual override
     *
     * @param assignmentId Assignment ID
     * @param username User performing override
     */
    void markAsManualOverride(Long assignmentId, String username);

    // ========== Teacher Assignment Methods ==========

    /**
     * Assign all teachers to lunch waves
     * Distributes teachers evenly across waves
     *
     * @param scheduleId The schedule ID
     * @return Number of teachers assigned
     */
    int assignTeachersToLunchWaves(Long scheduleId);

    /**
     * Assign a teacher to a lunch wave
     *
     * @param teacherId The teacher ID
     * @param lunchWaveId The lunch wave ID
     * @param username User performing assignment
     * @return Created assignment
     */
    TeacherLunchAssignment assignTeacherToWave(Long teacherId, Long lunchWaveId, String username);

    /**
     * Reassign a teacher to a different lunch wave
     *
     * @param assignmentId Current assignment ID
     * @param newWaveId New lunch wave ID
     * @param username User performing reassignment
     * @return Updated assignment
     */
    TeacherLunchAssignment reassignTeacher(Long assignmentId, Long newWaveId, String username);

    /**
     * Assign cafeteria supervision duty to a teacher
     *
     * @param assignmentId Assignment ID
     * @param location Supervision location (e.g., "Main Cafeteria")
     * @param username User assigning duty
     * @return Updated assignment
     */
    TeacherLunchAssignment assignSupervisionDuty(Long assignmentId, String location, String username);

    /**
     * Remove supervision duty from a teacher
     *
     * @param assignmentId Assignment ID
     * @param username User removing duty
     * @return Updated assignment
     */
    TeacherLunchAssignment removeSupervisionDuty(Long assignmentId, String username);

    /**
     * Mark teacher as having duty during other lunch waves
     *
     * @param assignmentId Assignment ID
     * @param username User updating duty status
     * @return Updated assignment
     */
    TeacherLunchAssignment markDutyDuringOtherWaves(Long assignmentId, String username);

    // ========== Teacher Query Methods ==========

    /**
     * Get all unassigned teachers for a schedule
     *
     * @param scheduleId Schedule ID
     * @return List of teachers without lunch assignments
     */
    List<Teacher> getUnassignedTeachers(Long scheduleId);

    /**
     * Get teacher's lunch assignment
     *
     * @param teacherId Teacher ID
     * @param scheduleId Schedule ID
     * @return Assignment if found
     */
    Optional<TeacherLunchAssignment> getTeacherAssignment(Long teacherId, Long scheduleId);

    /**
     * Get all teachers assigned to a lunch wave
     *
     * @param lunchWaveId Lunch wave ID
     * @return List of teacher assignments
     */
    List<TeacherLunchAssignment> getTeachersInWave(Long lunchWaveId);

    /**
     * Get teachers with duty-free lunch
     *
     * @param scheduleId Schedule ID
     * @return List of duty-free assignments
     */
    List<TeacherLunchAssignment> getDutyFreeTeachers(Long scheduleId);

    /**
     * Get teachers with supervision duty
     *
     * @param scheduleId Schedule ID
     * @return List of teachers supervising cafeteria
     */
    List<TeacherLunchAssignment> getTeachersWithSupervisionDuty(Long scheduleId);

    /**
     * Get teachers available for supervision duty assignment
     *
     * @param scheduleId Schedule ID
     * @return List of teachers who could be assigned duty
     */
    List<TeacherLunchAssignment> getTeachersAvailableForSupervision(Long scheduleId);

    // ========== Validation Methods ==========

    /**
     * Validate that all students are assigned to lunch waves
     *
     * @param scheduleId Schedule ID
     * @return true if all students assigned
     */
    boolean areAllStudentsAssigned(Long scheduleId);

    /**
     * Validate that lunch wave capacities are not exceeded
     *
     * @param scheduleId Schedule ID
     * @return true if no wave is over capacity
     */
    boolean areCapacitiesRespected(Long scheduleId);

    /**
     * Validate that grade-level restrictions are respected
     *
     * @param scheduleId Schedule ID
     * @return true if all students in appropriate grade waves
     */
    boolean areGradeLevelsRespected(Long scheduleId);

    /**
     * Check if assignments are ready for schedule generation
     * Validates:
     * - All students assigned
     * - Capacities respected
     * - Grade levels respected
     * - At least one teacher per wave
     *
     * @param scheduleId Schedule ID
     * @return true if assignments are valid
     */
    boolean areAssignmentsValid(Long scheduleId);

    // ========== Statistics Methods ==========

    /**
     * Get assignment statistics for a schedule
     *
     * @param scheduleId Schedule ID
     * @return Statistics object
     */
    LunchAssignmentStatistics getAssignmentStatistics(Long scheduleId);

    /**
     * Statistics container for lunch assignments
     */
    class LunchAssignmentStatistics {
        private long totalStudents;
        private long assignedStudents;
        private long unassignedStudents;
        private long lockedAssignments;
        private long manualOverrides;
        private long totalTeachers;
        private long assignedTeachers;
        private long teachersWithDuty;
        private long dutyFreeTeachers;

        // Getters and setters
        public long getTotalStudents() { return totalStudents; }
        public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }

        public long getAssignedStudents() { return assignedStudents; }
        public void setAssignedStudents(long assignedStudents) { this.assignedStudents = assignedStudents; }

        public long getUnassignedStudents() { return unassignedStudents; }
        public void setUnassignedStudents(long unassignedStudents) { this.unassignedStudents = unassignedStudents; }

        public long getLockedAssignments() { return lockedAssignments; }
        public void setLockedAssignments(long lockedAssignments) { this.lockedAssignments = lockedAssignments; }

        public long getManualOverrides() { return manualOverrides; }
        public void setManualOverrides(long manualOverrides) { this.manualOverrides = manualOverrides; }

        public long getTotalTeachers() { return totalTeachers; }
        public void setTotalTeachers(long totalTeachers) { this.totalTeachers = totalTeachers; }

        public long getAssignedTeachers() { return assignedTeachers; }
        public void setAssignedTeachers(long assignedTeachers) { this.assignedTeachers = assignedTeachers; }

        public long getTeachersWithDuty() { return teachersWithDuty; }
        public void setTeachersWithDuty(long teachersWithDuty) { this.teachersWithDuty = teachersWithDuty; }

        public long getDutyFreeTeachers() { return dutyFreeTeachers; }
        public void setDutyFreeTeachers(long dutyFreeTeachers) { this.dutyFreeTeachers = dutyFreeTeachers; }
    }
}
