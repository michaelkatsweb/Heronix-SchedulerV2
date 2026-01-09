package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Schedule Slot Repository - COMPLETE WITH ALL METHODS
 * Location: src/main/java/com/eduscheduler/repository/ScheduleSlotRepository.java
 * 
 * ✅ ALL MISSING METHODS ADDED
 */
@Repository
public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {

    // ========== EXISTING METHODS ==========
    
    @Query("SELECT DISTINCT s FROM ScheduleSlot s " +
           "LEFT JOIN FETCH s.teacher " +
           "LEFT JOIN FETCH s.course " +
           "LEFT JOIN FETCH s.room " +
           "WHERE s.schedule.id = :scheduleId")
    List<ScheduleSlot> findByScheduleIdWithDetails(@Param("scheduleId") Long scheduleId);

    List<ScheduleSlot> findByScheduleId(Long scheduleId);

    @Query("SELECT s FROM ScheduleSlot s WHERE s.schedule.id = ?1")
    List<ScheduleSlot> findAllByScheduleId(Long scheduleId);

    @Query("SELECT DISTINCT s FROM ScheduleSlot s " +
           "LEFT JOIN FETCH s.teacher " +
           "LEFT JOIN FETCH s.course " +
           "LEFT JOIN FETCH s.room " +
           "WHERE s.teacher.id = :teacherId")
    List<ScheduleSlot> findByTeacherIdWithDetails(@Param("teacherId") Long teacherId);

    @Query("SELECT DISTINCT s FROM ScheduleSlot s " +
           "LEFT JOIN FETCH s.teacher " +
           "LEFT JOIN FETCH s.course " +
           "LEFT JOIN FETCH s.room " +
           "WHERE s.room.id = :roomId")
    List<ScheduleSlot> findByRoomIdWithDetails(@Param("roomId") Long roomId);

    @Query("SELECT DISTINCT s FROM ScheduleSlot s " +
           "LEFT JOIN FETCH s.teacher " +
           "LEFT JOIN FETCH s.course " +
           "LEFT JOIN FETCH s.room " +
           "WHERE s.course.id = :courseId")
    List<ScheduleSlot> findByCourseIdWithDetails(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(s) FROM ScheduleSlot s WHERE s.schedule.id = :scheduleId")
    long countByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT DISTINCT s FROM ScheduleSlot s " +
           "LEFT JOIN FETCH s.teacher " +
           "LEFT JOIN FETCH s.course " +
           "LEFT JOIN FETCH s.room " +
           "WHERE s.schedule.id = :scheduleId AND s.hasConflict = true")
    List<ScheduleSlot> findConflictingSlots(@Param("scheduleId") Long scheduleId);

    // ========== NEWLY ADDED METHODS FOR CONFLICT DETECTION ==========

    /**
     * Find teacher time conflicts
     * Used by ConflictDetectionServiceImpl and SubstituteServiceImpl
     */
    @Query("SELECT s FROM ScheduleSlot s WHERE s.teacher.id = :teacherId " +
           "AND s.dayOfWeek = :day " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    List<ScheduleSlot> findTeacherTimeConflicts(
        @Param("teacherId") Long teacherId,
        @Param("day") DayOfWeek day,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    /**
     * Find room time conflicts
     * Used by ConflictDetectionServiceImpl
     */
    @Query("SELECT s FROM ScheduleSlot s WHERE s.room.id = :roomId " +
           "AND s.dayOfWeek = :day " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    List<ScheduleSlot> findRoomTimeConflicts(
        @Param("roomId") Long roomId,
        @Param("day") DayOfWeek day,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    /**
     * Find teacher schedule by day
     * Used by SubstituteServiceImpl
     */
    @Query("SELECT s FROM ScheduleSlot s WHERE s.teacher.id = :teacherId " +
           "AND s.dayOfWeek = :day")
    List<ScheduleSlot> findTeacherScheduleByDay(
        @Param("teacherId") Long teacherId,
        @Param("day") DayOfWeek day
    );

    /**
     * Find slots by day of week
     * Used by SubstituteServiceImpl
     */
    @Query("SELECT s FROM ScheduleSlot s WHERE s.dayOfWeek = :day")
    List<ScheduleSlot> findByDayOfWeek(@Param("day") DayOfWeek day);

    // ========== METHODS FOR SUBSTITUTE ASSIGNMENT DIALOG ==========

    /**
     * Find teacher schedule slots for a specific date
     * Used by SubstituteAssignmentDialogController to display teacher's schedule
     * Finds all slots where the schedule is active on the given date
     */
    @Query("SELECT DISTINCT s FROM ScheduleSlot s " +
           "LEFT JOIN FETCH s.teacher " +
           "LEFT JOIN FETCH s.course " +
           "LEFT JOIN FETCH s.room " +
           "LEFT JOIN FETCH s.schedule sch " +
           "WHERE s.teacher.id = :teacherId " +
           "AND sch.startDate <= :date " +
           "AND (sch.endDate IS NULL OR sch.endDate >= :date)")
    List<ScheduleSlot> findByTeacherAndDate(
        @Param("teacherId") Long teacherId,
        @Param("date") java.time.LocalDate date
    );
}