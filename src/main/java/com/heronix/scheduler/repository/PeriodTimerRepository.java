package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.PeriodTimer;
import com.heronix.scheduler.model.domain.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Period Timer operations
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025 - QR Attendance System Phase 1
 */
@Repository
public interface PeriodTimerRepository extends JpaRepository<PeriodTimer, Long> {

    /**
     * Find all active period timers
     */
    List<PeriodTimer> findByActiveTrueOrderByPeriodNumberAsc();

    /**
     * Find period timer by period number
     */
    Optional<PeriodTimer> findByPeriodNumberAndActiveTrue(Integer periodNumber);

    /**
     * Find all period timers for a specific academic year
     */
    List<PeriodTimer> findByAcademicYearAndActiveTrueOrderByPeriodNumberAsc(AcademicYear academicYear);

    /**
     * Find period timer for academic year by period number
     */
    Optional<PeriodTimer> findByAcademicYearAndPeriodNumberAndActiveTrue(
        AcademicYear academicYear,
        Integer periodNumber
    );

    /**
     * Find all period timers (including inactive)
     */
    List<PeriodTimer> findAllByOrderByPeriodNumberAsc();

    /**
     * Find period timer that contains a specific time
     */
    @Query("SELECT p FROM PeriodTimer p WHERE p.active = true " +
           "AND p.startTime <= :currentTime AND p.endTime >= :currentTime " +
           "ORDER BY p.periodNumber ASC")
    Optional<PeriodTimer> findByCurrentTime(@Param("currentTime") LocalTime currentTime);

    /**
     * Find period timer whose attendance window contains a specific time
     */
    @Query("SELECT p FROM PeriodTimer p WHERE p.active = true " +
           "AND p.startTime <= :currentTime " +
           "AND p.endTime >= :currentTime " +
           "ORDER BY p.periodNumber ASC")
    Optional<PeriodTimer> findByAttendanceWindow(@Param("currentTime") LocalTime currentTime);

    /**
     * Find all periods that apply to a specific day of week
     */
    @Query("SELECT p FROM PeriodTimer p WHERE p.active = true " +
           "AND (p.daysOfWeek IS NULL OR p.daysOfWeek LIKE CONCAT('%', :dayOfWeek, '%')) " +
           "ORDER BY p.periodNumber ASC")
    List<PeriodTimer> findByDayOfWeek(@Param("dayOfWeek") String dayOfWeek);

    /**
     * Count active period timers
     */
    Long countByActiveTrue();

    /**
     * Find periods with auto mark absent enabled
     */
    List<PeriodTimer> findByActiveTrueAndAutoMarkAbsentTrueOrderByPeriodNumberAsc();

    /**
     * Find period timers within a time range
     */
    @Query("SELECT p FROM PeriodTimer p WHERE p.active = true " +
           "AND ((p.startTime >= :startTime AND p.startTime <= :endTime) " +
           "OR (p.endTime >= :startTime AND p.endTime <= :endTime) " +
           "OR (p.startTime <= :startTime AND p.endTime >= :endTime)) " +
           "ORDER BY p.periodNumber ASC")
    List<PeriodTimer> findOverlappingPeriods(
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    /**
     * Check if a period number exists for a specific academic year
     */
    boolean existsByAcademicYearAndPeriodNumber(AcademicYear academicYear, Integer periodNumber);

    /**
     * Find the next period after a given time
     */
    @Query("SELECT p FROM PeriodTimer p WHERE p.active = true " +
           "AND p.startTime > :currentTime ORDER BY p.startTime ASC")
    Optional<PeriodTimer> findNextPeriod(@Param("currentTime") LocalTime currentTime);

    /**
     * Find the current or next period
     */
    @Query("SELECT p FROM PeriodTimer p WHERE p.active = true " +
           "AND (p.endTime >= :currentTime OR p.startTime > :currentTime) " +
           "ORDER BY p.startTime ASC")
    Optional<PeriodTimer> findCurrentOrNextPeriod(@Param("currentTime") LocalTime currentTime);

    /**
     * Get maximum period number
     */
    @Query("SELECT MAX(p.periodNumber) FROM PeriodTimer p WHERE p.active = true")
    Integer findMaxPeriodNumber();

    /**
     * Find periods by attendance window duration
     */
    List<PeriodTimer> findByActiveTrueAndAttendanceWindowMinutesOrderByPeriodNumberAsc(
        Integer attendanceWindowMinutes
    );
}
