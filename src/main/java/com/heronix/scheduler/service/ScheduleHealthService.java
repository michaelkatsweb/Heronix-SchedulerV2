package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.dto.ScheduleHealthMetrics;

/**
 * Service for calculating and monitoring schedule health
 * Provides comprehensive health scoring and quality metrics
 */
public interface ScheduleHealthService {

    /**
     * Calculate comprehensive health metrics for a schedule
     *
     * @param schedule The schedule to analyze
     * @return Complete health metrics with scores and issues
     */
    ScheduleHealthMetrics calculateHealthMetrics(Schedule schedule);

    /**
     * Calculate overall health score (0-100)
     * Quick method when only the score is needed
     *
     * @param schedule The schedule to score
     * @return Health score (0-100)
     */
    Double calculateHealthScore(Schedule schedule);

    /**
     * Calculate conflict score component (0-100)
     * 100 = no conflicts, 0 = severe conflicts
     *
     * @param schedule The schedule to analyze
     * @return Conflict score
     */
    Double calculateConflictScore(Schedule schedule);

    /**
     * Calculate balance score component (0-100)
     * Measures how evenly students are distributed across sections
     *
     * @param schedule The schedule to analyze
     * @return Balance score
     */
    Double calculateBalanceScore(Schedule schedule);

    /**
     * Calculate utilization score component (0-100)
     * Measures teacher and room utilization efficiency
     *
     * @param schedule The schedule to analyze
     * @return Utilization score
     */
    Double calculateUtilizationScore(Schedule schedule);

    /**
     * Calculate compliance score component (0-100)
     * Measures adherence to prep time, IEP, and other requirements
     *
     * @param schedule The schedule to analyze
     * @return Compliance score
     */
    Double calculateComplianceScore(Schedule schedule);

    /**
     * Calculate coverage score component (0-100)
     * Measures percentage of students fully scheduled
     *
     * @param schedule The schedule to analyze
     * @return Coverage score
     */
    Double calculateCoverageScore(Schedule schedule);

    /**
     * Check if schedule meets minimum quality thresholds
     *
     * @param schedule The schedule to validate
     * @return true if schedule is acceptable for production
     */
    boolean isScheduleAcceptable(Schedule schedule);

    /**
     * Get quick health summary string
     *
     * @param schedule The schedule to summarize
     * @return Summary text (e.g., "EXCELLENT (95/100)")
     */
    String getHealthSummary(Schedule schedule);
}
