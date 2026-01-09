package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.LunchPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Lunch Period Repository - COMPLETE VERSION
 * Location:
 * src/main/java/com/eduscheduler/repository/LunchPeriodRepository.java
 * 
 * Compatible with existing LunchPeriodServiceImpl
 * 
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-10-18
 */
@Repository
public interface LunchPeriodRepository extends JpaRepository<LunchPeriod, Long> {

    /**
     * Find all active lunch periods
     */
    List<LunchPeriod> findByActiveTrue();

    /**
     * Find lunch period by name
     */
    Optional<LunchPeriod> findByName(String name);

    /**
     * Check if lunch period exists by name
     */
    boolean existsByName(String name);

    /**
     * Find all lunch periods ordered by display order
     */
    List<LunchPeriod> findAllByOrderByDisplayOrderAsc();

    /**
     * Find lunch periods with available capacity
     */
    @Query("SELECT lp FROM LunchPeriod lp WHERE lp.currentCount < lp.maxCapacity")
    List<LunchPeriod> findByCurrentCountLessThanMaxCapacity();

    /**
     * Find lunch periods by lunch group
     * Example: Find all "Period 4/5" lunches
     */
    List<LunchPeriod> findByLunchGroup(String lunchGroup);

    /**
     * Find lunch periods by day of week
     * For rotating schedules where lunch times vary by day
     */
    List<LunchPeriod> findByDayOfWeek(Integer dayOfWeek);

    /**
     * Find lunch periods by grade level
     * Note: This searches within the gradeLevels string field
     */
    @Query("SELECT lp FROM LunchPeriod lp WHERE lp.gradeLevels LIKE %:gradeLevel%")
    List<LunchPeriod> findByGradeLevel(@Param("gradeLevel") String gradeLevel);

    /**
     * Find lunch periods by location
     */
    List<LunchPeriod> findByLocation(String location);

    /**
     * Find lunch periods by priority (ordered by priority descending)
     */
    List<LunchPeriod> findAllByOrderByPriorityDesc();

    /**
     * Get lunch period statistics
     */
    @Query("SELECT COUNT(lp) FROM LunchPeriod lp WHERE lp.active = true")
    long countActiveLunchPeriods();

    /**
     * Get total capacity across all active lunch periods
     */
    @Query("SELECT SUM(lp.maxCapacity) FROM LunchPeriod lp WHERE lp.active = true")
    Integer getTotalCapacity();

    /**
     * Get total current enrollment across all active lunch periods
     */
    @Query("SELECT SUM(lp.currentCount) FROM LunchPeriod lp WHERE lp.active = true")
    Integer getTotalEnrollment();

    /**
     * Find overcapacity lunch periods
     */
    @Query("SELECT lp FROM LunchPeriod lp WHERE lp.currentCount > lp.maxCapacity")
    List<LunchPeriod> findOvercapacityPeriods();

    /**
     * Find undercapacity lunch periods
     */
    @Query("SELECT lp FROM LunchPeriod lp WHERE lp.currentCount < (lp.maxCapacity * 0.8)")
    List<LunchPeriod> findUndercapacityPeriods();

    /**
     * Find lunch periods for a specific schedule
     * (If you link lunch periods to schedules in the future)
     */
    @Query("SELECT lp FROM LunchPeriod lp WHERE lp.active = true ORDER BY lp.displayOrder")
    List<LunchPeriod> findAllActiveOrdered();
}