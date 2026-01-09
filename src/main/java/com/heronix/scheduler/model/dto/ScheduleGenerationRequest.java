package com.heronix.scheduler.model.dto;

import com.heronix.scheduler.model.enums.LunchAssignmentMethod;
import com.heronix.scheduler.model.enums.ScheduleType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedule Generation Request DTO
 * Location:
 * src/main/java/com/eduscheduler/model/dto/ScheduleGenerationRequest.java
 * 
 * ✅ ENHANCED: Added @Builder for flexible object creation
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleGenerationRequest {
    // Basic information
    private String scheduleName;
    private ScheduleType scheduleType;
    private LocalDate startDate;
    private LocalDate endDate;

    // Time configuration (Legacy - hour-based)
    private Integer startHour;
    private Integer endHour;
    private Integer periodDuration; // minutes
    private Integer passingPeriodDuration; // minutes (NEW - Phase 2: passing time between classes)

    // Time configuration (New - minute-precise)
    private LocalTime schoolStartTime;      // e.g., 07:00 (school opens)
    private LocalTime firstPeriodStartTime; // e.g., 07:20 (first class)
    private LocalTime schoolEndTime;        // e.g., 14:10 (school closes)
    // Breakfast period (Phase 3: time between schoolStartTime and firstPeriodStartTime)
    // Note: Breakfast is implicit - the gap between schoolStartTime (07:00) and firstPeriodStartTime (07:20)
    // No separate breakfast slots are created - this is arrival/breakfast time

    // Lunch configuration
    private Boolean enableLunch;
    private Integer lunchStartHour;         // Legacy
    private Integer lunchDuration;          // minutes
    private LocalTime lunchStartTime;       // New - precise lunch start (e.g., 10:50)

    // Phase 5: Multiple Lunch Periods
    @Builder.Default
    private Boolean enableMultipleLunches = false;  // Enable multiple rotating lunch periods
    @Builder.Default
    private Integer lunchWaveCount = 1;             // Number of lunch waves (1-6)
    @Builder.Default
    private LunchAssignmentMethod lunchAssignmentMethod = LunchAssignmentMethod.BALANCED;
    @Builder.Default
    private List<LunchWaveConfig> lunchWaveConfigs = new ArrayList<>();

    // Constraints
    private Integer maxConsecutiveHours;
    private Integer maxDailyHours;

    // Optimization
    private Integer optimizationTimeSeconds;

    /**
     * Check if lunch is enabled (null-safe)
     * 
     * @return true if lunch should be scheduled
     */
    public boolean isEnableLunch() {
        return enableLunch != null && enableLunch;
    }

    @Override
    public String toString() {
        return String.format(
                "ScheduleGenerationRequest{name='%s', type=%s, dates=%s to %s, " +
                        "time=%d:00-%d:00, period=%dmin, optimization=%dsec}",
                scheduleName, scheduleType, startDate, endDate,
                startHour, endHour, periodDuration, optimizationTimeSeconds);
    }

    /**
     * Configuration for a single lunch wave
     * Phase 5: Multiple Rotating Lunch Periods
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LunchWaveConfig {
        private String waveName;              // e.g., "Lunch 1", "6th Grade Lunch"
        private Integer waveOrder;            // 1, 2, 3
        private LocalTime startTime;          // e.g., 10:04
        private LocalTime endTime;            // e.g., 10:34
        private Integer maxCapacity;          // e.g., 250
        private Integer gradeLevelRestriction; // null or 6-12
    }
}