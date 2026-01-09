package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.LunchWave;
import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.domain.Student;
import com.heronix.scheduler.model.domain.StudentLunchAssignment;
import com.heronix.scheduler.model.enums.LunchAssignmentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for StudentLunchAssignment entities
 *
 * Manages student-to-lunch-wave mappings
 *
 * Phase 5A: Multiple Rotating Lunch Periods
 * Date: December 1, 2025
 */
@Repository
public interface StudentLunchAssignmentRepository extends JpaRepository<StudentLunchAssignment, Long> {

    /**
     * Find the lunch assignment for a specific student in a schedule
     */
    Optional<StudentLunchAssignment> findByStudentAndSchedule(Student student, Schedule schedule);

    /**
     * Find the lunch assignment by student ID and schedule ID
     */
    @Query("SELECT sla FROM StudentLunchAssignment sla " +
           "LEFT JOIN FETCH sla.student " +
           "LEFT JOIN FETCH sla.lunchWave " +
           "LEFT JOIN FETCH sla.schedule " +
           "WHERE sla.student.id = :studentId AND sla.schedule.id = :scheduleId")
    Optional<StudentLunchAssignment> findByStudentIdAndScheduleId(@Param("studentId") Long studentId, @Param("scheduleId") Long scheduleId);

    /**
     * Find all students assigned to a specific lunch wave
     */
    List<StudentLunchAssignment> findByLunchWaveOrderByStudentLastNameAsc(LunchWave lunchWave);

    /**
     * Find all students assigned to a lunch wave by ID
     */
    @Query("SELECT sla FROM StudentLunchAssignment sla " +
           "LEFT JOIN FETCH sla.student " +
           "LEFT JOIN FETCH sla.lunchWave " +
           "LEFT JOIN FETCH sla.schedule " +
           "WHERE sla.lunchWave.id = :lunchWaveId ORDER BY sla.student.lastName, sla.student.firstName")
    List<StudentLunchAssignment> findByLunchWaveIdOrderByStudentName(@Param("lunchWaveId") Long lunchWaveId);

    /**
     * Find all lunch assignments for a schedule
     */
    List<StudentLunchAssignment> findBySchedule(Schedule schedule);

    /**
     * Find all lunch assignments for a schedule by ID
     */
    @Query("SELECT sla FROM StudentLunchAssignment sla " +
           "LEFT JOIN FETCH sla.student " +
           "LEFT JOIN FETCH sla.lunchWave " +
           "LEFT JOIN FETCH sla.schedule " +
           "WHERE sla.schedule.id = :scheduleId")
    List<StudentLunchAssignment> findByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * Count students assigned to a specific lunch wave
     */
    long countByLunchWave(LunchWave lunchWave);

    /**
     * Count students assigned to a lunch wave by ID
     */
    @Query("SELECT COUNT(sla) FROM StudentLunchAssignment sla WHERE sla.lunchWave.id = :lunchWaveId")
    long countByLunchWaveId(@Param("lunchWaveId") Long lunchWaveId);

    /**
     * Find assignments by assignment method
     */
    @Query("SELECT sla FROM StudentLunchAssignment sla " +
           "LEFT JOIN FETCH sla.student " +
           "LEFT JOIN FETCH sla.lunchWave " +
           "LEFT JOIN FETCH sla.schedule " +
           "WHERE sla.schedule.id = :scheduleId AND sla.assignmentMethod = :method")
    List<StudentLunchAssignment> findByScheduleIdAndMethod(@Param("scheduleId") Long scheduleId, @Param("method") LunchAssignmentMethod method);

    /**
     * Find manually assigned students
     */
    @Query("SELECT sla FROM StudentLunchAssignment sla " +
           "LEFT JOIN FETCH sla.student " +
           "LEFT JOIN FETCH sla.lunchWave " +
           "LEFT JOIN FETCH sla.schedule " +
           "WHERE sla.schedule.id = :scheduleId AND (sla.manualOverride = true OR sla.assignmentMethod = 'MANUAL')")
    List<StudentLunchAssignment> findManualAssignments(@Param("scheduleId") Long scheduleId);

    /**
     * Find locked assignments (cannot be automatically changed)
     */
    @Query("SELECT sla FROM StudentLunchAssignment sla " +
           "LEFT JOIN FETCH sla.student " +
           "LEFT JOIN FETCH sla.lunchWave " +
           "LEFT JOIN FETCH sla.schedule " +
           "WHERE sla.schedule.id = :scheduleId AND sla.isLocked = true")
    List<StudentLunchAssignment> findLockedAssignments(@Param("scheduleId") Long scheduleId);

    /**
     * Find assignments that can be automatically reassigned
     */
    @Query("SELECT sla FROM StudentLunchAssignment sla " +
           "LEFT JOIN FETCH sla.student " +
           "LEFT JOIN FETCH sla.lunchWave " +
           "LEFT JOIN FETCH sla.schedule " +
           "WHERE sla.schedule.id = :scheduleId AND (sla.isLocked IS NULL OR sla.isLocked = false)")
    List<StudentLunchAssignment> findReassignableAssignments(@Param("scheduleId") Long scheduleId);

    /**
     * Find high-priority assignments
     */
    @Query("SELECT sla FROM StudentLunchAssignment sla " +
           "LEFT JOIN FETCH sla.student " +
           "LEFT JOIN FETCH sla.lunchWave " +
           "LEFT JOIN FETCH sla.schedule " +
           "WHERE sla.schedule.id = :scheduleId AND sla.priority >= 8")
    List<StudentLunchAssignment> findHighPriorityAssignments(@Param("scheduleId") Long scheduleId);

    /**
     * Find students in a specific grade assigned to a lunch wave
     */
    @Query("SELECT sla FROM StudentLunchAssignment sla " +
           "LEFT JOIN FETCH sla.student " +
           "LEFT JOIN FETCH sla.lunchWave " +
           "LEFT JOIN FETCH sla.schedule " +
           "WHERE sla.lunchWave.id = :lunchWaveId AND sla.student.gradeLevel = :gradeLevel")
    List<StudentLunchAssignment> findByLunchWaveIdAndGradeLevel(@Param("lunchWaveId") Long lunchWaveId, @Param("gradeLevel") Integer gradeLevel);

    /**
     * Find unassigned students for a schedule from a specific set of student IDs
     * This allows callers to provide the scope of students to consider (e.g., from course enrollments)
     */
    @Query("SELECT s FROM Student s WHERE s.id IN :studentIds " +
           "AND s.id NOT IN (SELECT sla.student.id FROM StudentLunchAssignment sla WHERE sla.schedule.id = :scheduleId)")
    List<Student> findUnassignedStudentsFromIds(@Param("scheduleId") Long scheduleId, @Param("studentIds") List<Long> studentIds);

    /**
     * Find unassigned students for a schedule by campus
     * Filters students to only those in the specified campus who don't have a lunch assignment
     * This prevents finding students from other campuses/schedules
     */
    @Query("SELECT s FROM Student s WHERE " +
           "s.campus.id = :campusId " +
           "AND s.id NOT IN (SELECT sla.student.id FROM StudentLunchAssignment sla WHERE sla.schedule.id = :scheduleId)")
    List<Student> findUnassignedStudentsByCampus(@Param("scheduleId") Long scheduleId, @Param("campusId") Long campusId);

    /**
     * Find unassigned students for a schedule
     * Uses campus relationship to scope students:
     * - If students are already assigned: returns unassigned students from those same campuses
     * - If no students assigned yet: returns students from a SINGLE campus (to prevent test cross-contamination)
     *
     * For production use, consider using findUnassignedStudentsFromIds with explicit student list
     * from course enrollments/schedule slots.
     */
    @Query("SELECT s FROM Student s WHERE " +
           "s.id NOT IN (SELECT sla.student.id FROM StudentLunchAssignment sla WHERE sla.schedule.id = :scheduleId) " +
           "AND s.campus IS NOT NULL " +
           "AND (s.campus.id IN (SELECT DISTINCT st.campus.id FROM StudentLunchAssignment sla2 " +
           "                     JOIN sla2.student st WHERE sla2.schedule.id = :scheduleId AND st.campus IS NOT NULL) " +
           "     OR (NOT EXISTS (SELECT 1 FROM StudentLunchAssignment sla3 WHERE sla3.schedule.id = :scheduleId) " +
           "         AND s.campus.id = (SELECT MAX(c.id) FROM Campus c)))")
    List<Student> findUnassignedStudents(@Param("scheduleId") Long scheduleId);

    /**
     * Count unassigned students
     * Uses same campus scoping logic as findUnassignedStudents()
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE " +
           "s.id NOT IN (SELECT sla.student.id FROM StudentLunchAssignment sla WHERE sla.schedule.id = :scheduleId) " +
           "AND s.campus IS NOT NULL " +
           "AND (s.campus.id IN (SELECT DISTINCT st.campus.id FROM StudentLunchAssignment sla2 " +
           "                     JOIN sla2.student st WHERE sla2.schedule.id = :scheduleId AND st.campus IS NOT NULL) " +
           "     OR (NOT EXISTS (SELECT 1 FROM StudentLunchAssignment sla3 WHERE sla3.schedule.id = :scheduleId) " +
           "         AND s.campus.id = (SELECT MAX(c.id) FROM Campus c)))")
    long countUnassignedStudents(@Param("scheduleId") Long scheduleId);

    /**
     * Find assignments by username (who assigned them)
     */
    @Query("SELECT sla FROM StudentLunchAssignment sla " +
           "LEFT JOIN FETCH sla.student " +
           "LEFT JOIN FETCH sla.lunchWave " +
           "LEFT JOIN FETCH sla.schedule " +
           "WHERE sla.schedule.id = :scheduleId AND sla.assignedBy = :username")
    List<StudentLunchAssignment> findByScheduleIdAndAssignedBy(@Param("scheduleId") Long scheduleId, @Param("username") String username);

    /**
     * Check if a student has a lunch assignment for a schedule
     */
    boolean existsByStudentAndSchedule(Student student, Schedule schedule);

    /**
     * Delete all assignments for a schedule
     */
    void deleteBySchedule(Schedule schedule);

    /**
     * Delete all assignments for a lunch wave
     */
    void deleteByLunchWave(LunchWave lunchWave);

    /**
     * Delete assignment for a specific student in a schedule
     */
    void deleteByStudentAndSchedule(Student student, Schedule schedule);
}
