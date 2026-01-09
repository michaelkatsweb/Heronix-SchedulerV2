package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.LunchWave;
import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.model.domain.TeacherLunchAssignment;
import com.heronix.scheduler.model.enums.LunchAssignmentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TeacherLunchAssignment entities
 *
 * Manages teacher-to-lunch-wave mappings and duty assignments
 *
 * Phase 5A: Multiple Rotating Lunch Periods
 * Date: December 1, 2025
 */
@Repository
public interface TeacherLunchAssignmentRepository extends JpaRepository<TeacherLunchAssignment, Long> {

    /**
     * Find the lunch assignment for a specific teacher in a schedule
     */
    Optional<TeacherLunchAssignment> findByTeacherAndSchedule(Teacher teacher, Schedule schedule);

    /**
     * Find the lunch assignment by teacher ID and schedule ID
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.teacher.id = :teacherId AND tla.schedule.id = :scheduleId")
    Optional<TeacherLunchAssignment> findByTeacherIdAndScheduleId(@Param("teacherId") Long teacherId, @Param("scheduleId") Long scheduleId);

    /**
     * Find all teachers assigned to a specific lunch wave
     */
    List<TeacherLunchAssignment> findByLunchWaveOrderByTeacherLastNameAsc(LunchWave lunchWave);

    /**
     * Find all teachers assigned to a lunch wave by ID
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.lunchWave.id = :lunchWaveId ORDER BY tla.teacher.lastName, tla.teacher.firstName")
    List<TeacherLunchAssignment> findByLunchWaveIdOrderByTeacherName(@Param("lunchWaveId") Long lunchWaveId);

    /**
     * Find all lunch assignments for a schedule
     */
    List<TeacherLunchAssignment> findBySchedule(Schedule schedule);

    /**
     * Find all lunch assignments for a schedule by ID
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.schedule.id = :scheduleId")
    List<TeacherLunchAssignment> findByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * Count teachers assigned to a specific lunch wave
     */
    long countByLunchWave(LunchWave lunchWave);

    /**
     * Find teachers with duty-free lunch
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.schedule.id = :scheduleId AND tla.isDutyFree = true")
    List<TeacherLunchAssignment> findDutyFreeAssignments(@Param("scheduleId") Long scheduleId);

    /**
     * Find teachers with supervision duty during their lunch
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.schedule.id = :scheduleId AND tla.hasSupervisionDuty = true")
    List<TeacherLunchAssignment> findWithSupervisionDuty(@Param("scheduleId") Long scheduleId);

    /**
     * Find teachers with duty during other lunch waves
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.schedule.id = :scheduleId AND tla.hasDutyDuringOtherWaves = true")
    List<TeacherLunchAssignment> findWithDutyDuringOtherWaves(@Param("scheduleId") Long scheduleId);

    /**
     * Find teachers supervising at a specific location
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.schedule.id = :scheduleId AND tla.supervisionLocation = :location")
    List<TeacherLunchAssignment> findBySupervisionLocation(@Param("scheduleId") Long scheduleId, @Param("location") String location);

    /**
     * Find assignments by assignment method
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.schedule.id = :scheduleId AND tla.assignmentMethod = :method")
    List<TeacherLunchAssignment> findByScheduleIdAndMethod(@Param("scheduleId") Long scheduleId, @Param("method") LunchAssignmentMethod method);

    /**
     * Find manually assigned teachers
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.schedule.id = :scheduleId AND (tla.manualOverride = true OR tla.assignmentMethod = 'MANUAL')")
    List<TeacherLunchAssignment> findManualAssignments(@Param("scheduleId") Long scheduleId);

    /**
     * Find locked assignments (cannot be automatically changed)
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.schedule.id = :scheduleId AND tla.isLocked = true")
    List<TeacherLunchAssignment> findLockedAssignments(@Param("scheduleId") Long scheduleId);

    /**
     * Find assignments that can be automatically reassigned
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.schedule.id = :scheduleId AND (tla.isLocked IS NULL OR tla.isLocked = false)")
    List<TeacherLunchAssignment> findReassignableAssignments(@Param("scheduleId") Long scheduleId);

    /**
     * Find high-priority assignments
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.schedule.id = :scheduleId AND tla.priority >= 8")
    List<TeacherLunchAssignment> findHighPriorityAssignments(@Param("scheduleId") Long scheduleId);

    /**
     * Find unassigned teachers for a schedule
     * Uses campus relationship to scope teachers:
     * - If teachers are already assigned: returns unassigned teachers from those same campuses
     * - If no teachers assigned yet: returns teachers from a SINGLE campus (to prevent test cross-contamination)
     */
    @Query("SELECT t FROM Teacher t WHERE " +
           "t.id NOT IN (SELECT tla.teacher.id FROM TeacherLunchAssignment tla WHERE tla.schedule.id = :scheduleId) " +
           "AND t.primaryCampus IS NOT NULL " +
           "AND (t.primaryCampus.id IN (SELECT DISTINCT tc.primaryCampus.id FROM TeacherLunchAssignment tla2 " +
           "                            JOIN tla2.teacher tc WHERE tla2.schedule.id = :scheduleId AND tc.primaryCampus IS NOT NULL) " +
           "     OR (NOT EXISTS (SELECT 1 FROM TeacherLunchAssignment tla3 WHERE tla3.schedule.id = :scheduleId) " +
           "         AND t.primaryCampus.id = (SELECT MAX(c.id) FROM Campus c)))")
    List<Teacher> findUnassignedTeachers(@Param("scheduleId") Long scheduleId);

    /**
     * Count unassigned teachers
     * Uses same campus scoping logic as findUnassignedTeachers()
     */
    @Query("SELECT COUNT(t) FROM Teacher t WHERE " +
           "t.id NOT IN (SELECT tla.teacher.id FROM TeacherLunchAssignment tla WHERE tla.schedule.id = :scheduleId) " +
           "AND t.primaryCampus IS NOT NULL " +
           "AND (t.primaryCampus.id IN (SELECT DISTINCT tc.primaryCampus.id FROM TeacherLunchAssignment tla2 " +
           "                            JOIN tla2.teacher tc WHERE tla2.schedule.id = :scheduleId AND tc.primaryCampus IS NOT NULL) " +
           "     OR (NOT EXISTS (SELECT 1 FROM TeacherLunchAssignment tla3 WHERE tla3.schedule.id = :scheduleId) " +
           "         AND t.primaryCampus.id = (SELECT MAX(c.id) FROM Campus c)))")
    long countUnassignedTeachers(@Param("scheduleId") Long scheduleId);

    /**
     * Find teachers available for supervision duty (duty-free and no other duties)
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.schedule.id = :scheduleId AND tla.isDutyFree = true AND tla.hasSupervisionDuty = false AND tla.hasDutyDuringOtherWaves = false")
    List<TeacherLunchAssignment> findAvailableForSupervision(@Param("scheduleId") Long scheduleId);

    /**
     * Count teachers with supervision duty in a specific lunch wave
     */
    @Query("SELECT COUNT(tla) FROM TeacherLunchAssignment tla WHERE tla.lunchWave.id = :lunchWaveId AND tla.hasSupervisionDuty = true")
    long countSupervisorsInWave(@Param("lunchWaveId") Long lunchWaveId);

    /**
     * Find assignments by username (who assigned them)
     */
    @Query("SELECT tla FROM TeacherLunchAssignment tla " +
           "LEFT JOIN FETCH tla.teacher " +
           "LEFT JOIN FETCH tla.lunchWave " +
           "LEFT JOIN FETCH tla.schedule " +
           "WHERE tla.schedule.id = :scheduleId AND tla.assignedBy = :username")
    List<TeacherLunchAssignment> findByScheduleIdAndAssignedBy(@Param("scheduleId") Long scheduleId, @Param("username") String username);

    /**
     * Check if a teacher has a lunch assignment for a schedule
     */
    boolean existsByTeacherAndSchedule(Teacher teacher, Schedule schedule);

    /**
     * Delete all assignments for a schedule
     */
    void deleteBySchedule(Schedule schedule);

    /**
     * Delete all assignments for a lunch wave
     */
    void deleteByLunchWave(LunchWave lunchWave);

    /**
     * Delete assignment for a specific teacher in a schedule
     */
    void deleteByTeacherAndSchedule(Teacher teacher, Schedule schedule);
}
