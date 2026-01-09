package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.OptimizationResult;
import com.heronix.scheduler.model.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Optimization Result Repository
 * Data access for optimization results
 *
 * Location: src/main/java/com/eduscheduler/repository/OptimizationResultRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
@Repository
public interface OptimizationResultRepository extends JpaRepository<OptimizationResult, Long> {

    /**
     * Find results for a schedule ordered by start time
     */
    List<OptimizationResult> findByScheduleOrderByStartedAtDesc(Schedule schedule);

    /**
     * Find results by status
     */
    List<OptimizationResult> findByStatus(OptimizationResult.OptimizationStatus status);

    /**
     * Find successful results
     */
    List<OptimizationResult> findBySuccessfulTrue();

    /**
     * Find recent results (top N)
     */
    @Query("SELECT r FROM OptimizationResult r ORDER BY r.startedAt DESC")
    List<OptimizationResult> findTopNByOrderByStartedAtDesc(@Param("limit") int limit);

    /**
     * Find results completed before a date
     */
    List<OptimizationResult> findByCompletedAtBefore(LocalDateTime date);

    /**
     * Find running optimizations
     */
    @Query("SELECT r FROM OptimizationResult r WHERE r.status = 'RUNNING' ORDER BY r.startedAt DESC")
    List<OptimizationResult> findRunningOptimizations();

    /**
     * Count results by status
     */
    long countByStatus(OptimizationResult.OptimizationStatus status);
}
