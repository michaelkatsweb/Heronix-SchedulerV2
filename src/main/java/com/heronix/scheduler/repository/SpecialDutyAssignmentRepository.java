package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.SpecialDutyAssignment;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.model.enums.DayOfWeek;
import com.heronix.scheduler.model.enums.DutyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Special Duty Assignment Repository
 * Data access layer for special duty assignments
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-06
 */
@Repository
public interface SpecialDutyAssignmentRepository extends JpaRepository<SpecialDutyAssignment, Long> {

    // ========================================================================
    // FIND BY DUTY TYPE
    // ========================================================================

    List<SpecialDutyAssignment> findByDutyType(DutyType dutyType);

    List<SpecialDutyAssignment> findByDutyTypeAndActiveTrue(DutyType dutyType);

    // ========================================================================
    // FIND BY STAFF
    // ========================================================================

    List<SpecialDutyAssignment> findByTeacher(Teacher teacher);

    List<SpecialDutyAssignment> findByTeacherAndActiveTrue(Teacher teacher);

    List<SpecialDutyAssignment> findByTeacherId(Long teacherId);

    List<SpecialDutyAssignment> findByStaffName(String staffName);

    // ========================================================================
    // FIND BY SCHEDULE
    // ========================================================================

    /**
     * Find all recurring duties for a specific day of week
     */
    @Query("SELECT d FROM SpecialDutyAssignment d WHERE d.dayOfWeek = :dayOfWeek AND d.isRecurring = true AND d.active = true")
    List<SpecialDutyAssignment> findDailyDutiesByDay(@Param("dayOfWeek") DayOfWeek dayOfWeek);

    /**
     * Find all special events for a specific date
     */
    @Query("SELECT d FROM SpecialDutyAssignment d WHERE d.eventDate = :eventDate AND d.active = true")
    List<SpecialDutyAssignment> findSpecialEventsByDate(@Param("eventDate") LocalDate eventDate);

    /**
     * Find all special events within a date range
     */
    @Query("SELECT d FROM SpecialDutyAssignment d WHERE d.eventDate BETWEEN :startDate AND :endDate AND d.active = true ORDER BY d.eventDate, d.startTime")
    List<SpecialDutyAssignment> findSpecialEventsBetweenDates(@Param("startDate") LocalDate startDate,
                                                                @Param("endDate") LocalDate endDate);

    /**
     * Find all active daily duties (recurring)
     */
    @Query("SELECT d FROM SpecialDutyAssignment d LEFT JOIN FETCH d.teacher WHERE d.isRecurring = true AND d.active = true ORDER BY d.dayOfWeek, d.startTime")
    List<SpecialDutyAssignment> findAllActiveDailyDuties();

    /**
     * Find all active special events
     */
    @Query("SELECT d FROM SpecialDutyAssignment d LEFT JOIN FETCH d.teacher WHERE d.isRecurring = false AND d.active = true ORDER BY d.eventDate, d.startTime")
    List<SpecialDutyAssignment> findAllActiveSpecialEvents();

    // ========================================================================
    // FIND BY STATUS
    // ========================================================================

    @Query("SELECT d FROM SpecialDutyAssignment d LEFT JOIN FETCH d.teacher WHERE d.active = true")
    List<SpecialDutyAssignment> findByActiveTrue();

    List<SpecialDutyAssignment> findByCompletedTrue();

    List<SpecialDutyAssignment> findByConfirmedByStaffFalse();

    /**
     * Find unconfirmed assignments for a teacher
     */
    @Query("SELECT d FROM SpecialDutyAssignment d WHERE d.teacher = :teacher AND d.confirmedByStaff = false AND d.active = true")
    List<SpecialDutyAssignment> findUnconfirmedByTeacher(@Param("teacher") Teacher teacher);

    // ========================================================================
    // FIND BY PRIORITY
    // ========================================================================

    @Query("SELECT d FROM SpecialDutyAssignment d WHERE d.priority = :priority AND d.active = true ORDER BY d.eventDate, d.dayOfWeek")
    List<SpecialDutyAssignment> findByPriority(@Param("priority") Integer priority);

    /**
     * Find high priority duties (priority = 1)
     */
    @Query("SELECT d FROM SpecialDutyAssignment d WHERE d.priority = 1 AND d.active = true")
    List<SpecialDutyAssignment> findHighPriorityDuties();

    // ========================================================================
    // FIND BY LOCATION
    // ========================================================================

    List<SpecialDutyAssignment> findByDutyLocation(String dutyLocation);

    // ========================================================================
    // FIND CONFLICTING ASSIGNMENTS
    // ========================================================================

    /**
     * Find duties that conflict with a given teacher's schedule on a specific day/time
     */
    @Query("SELECT d FROM SpecialDutyAssignment d WHERE d.teacher.id = :teacherId " +
           "AND d.dayOfWeek = :dayOfWeek " +
           "AND d.active = true " +
           "AND d.isRecurring = true " +
           "AND ((d.startTime <= :endTime AND d.endTime >= :startTime))")
    List<SpecialDutyAssignment> findConflictingDailyDuties(@Param("teacherId") Long teacherId,
                                                             @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                                             @Param("startTime") java.time.LocalTime startTime,
                                                             @Param("endTime") java.time.LocalTime endTime);

    /**
     * Find special event duties that conflict with a given date/time
     */
    @Query("SELECT d FROM SpecialDutyAssignment d WHERE d.teacher.id = :teacherId " +
           "AND d.eventDate = :eventDate " +
           "AND d.active = true " +
           "AND ((d.startTime <= :endTime AND d.endTime >= :startTime))")
    List<SpecialDutyAssignment> findConflictingSpecialEvents(@Param("teacherId") Long teacherId,
                                                               @Param("eventDate") LocalDate eventDate,
                                                               @Param("startTime") java.time.LocalTime startTime,
                                                               @Param("endTime") java.time.LocalTime endTime);

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Count duties assigned to a teacher
     */
    @Query("SELECT COUNT(d) FROM SpecialDutyAssignment d WHERE d.teacher.id = :teacherId AND d.active = true")
    Long countByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * Count duties by type
     */
    @Query("SELECT COUNT(d) FROM SpecialDutyAssignment d WHERE d.dutyType = :dutyType AND d.active = true")
    Long countByDutyType(@Param("dutyType") DutyType dutyType);

    /**
     * Find teachers with most duties
     */
    @Query("SELECT d.teacher, COUNT(d) as dutyCount FROM SpecialDutyAssignment d " +
           "WHERE d.teacher IS NOT NULL AND d.active = true " +
           "GROUP BY d.teacher " +
           "ORDER BY dutyCount DESC")
    List<Object[]> findTeachersWithMostDuties();
}
