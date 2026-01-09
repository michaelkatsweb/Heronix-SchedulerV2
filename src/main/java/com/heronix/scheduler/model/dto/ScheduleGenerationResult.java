package com.heronix.scheduler.model.dto;

import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.enums.ScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Result of schedule generation including success/partial status and conflicts
 *
 * This DTO encapsulates the complete result of a schedule generation attempt,
 * including the generated schedule, completion percentage, detailed conflict
 * information, and actionable recommendations.
 *
 * Location: src/main/java/com/eduscheduler/model/dto/ScheduleGenerationResult.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleGenerationResult {

    /**
     * The generated schedule (may be partial)
     */
    private Schedule schedule;

    /**
     * Overall status of generation
     */
    private ScheduleStatus status;

    /**
     * Detailed list of conflicts encountered
     */
    @Builder.Default
    private List<ConflictDetail> conflicts = new ArrayList<>();

    /**
     * Percentage of courses successfully scheduled (0-100)
     */
    private Double completionPercentage;

    /**
     * Total number of courses to schedule
     */
    private Integer totalCourses;

    /**
     * Number of courses successfully scheduled
     */
    private Integer scheduledCourses;

    /**
     * Number of unscheduled courses
     */
    private Integer unscheduledCourses;

    /**
     * OptaPlanner score (hard/soft)
     */
    private String optimizationScore;

    /**
     * Total generation time in seconds
     */
    private Long generationTimeSeconds;

    /**
     * Number of conflicts by severity
     */
    private Map<String, Integer> conflictsBySeverity;

    /**
     * Number of conflicts by type
     */
    private Map<String, Integer> conflictsByType;

    /**
     * User-friendly summary message
     */
    private String summaryMessage;

    /**
     * Estimated time to fix all conflicts (minutes)
     */
    private Integer estimatedFixTimeMinutes;

    /**
     * Whether schedule is acceptable to publish/use
     */
    private Boolean acceptable;

    /**
     * Recommendations for improving schedule quality
     */
    @Builder.Default
    private List<String> recommendations = new ArrayList<>();

    /**
     * Add a conflict to the result
     *
     * @param conflict Conflict detail to add
     */
    public void addConflict(ConflictDetail conflict) {
        if (this.conflicts == null) {
            this.conflicts = new ArrayList<>();
        }
        this.conflicts.add(conflict);
    }

    /**
     * Add a recommendation
     *
     * @param recommendation Recommendation text
     */
    public void addRecommendation(String recommendation) {
        if (this.recommendations == null) {
            this.recommendations = new ArrayList<>();
        }
        this.recommendations.add(recommendation);
    }

    /**
     * Calculate summary statistics
     * Should be called after all conflicts are added
     */
    public void calculateStatistics() {
        if (conflicts != null && !conflicts.isEmpty()) {
            // Group by severity
            this.conflictsBySeverity = conflicts.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getSeverity() != null ? c.getSeverity().toString() : "UNKNOWN",
                    Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

            // Group by type
            this.conflictsByType = conflicts.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getType() != null ? c.getType().toString() : "UNKNOWN",
                    Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

            // Calculate estimated fix time
            this.estimatedFixTimeMinutes = conflicts.stream()
                .mapToInt(c -> c.getEstimatedFixTimeMinutes() != null ? c.getEstimatedFixTimeMinutes() : 5)
                .sum();
        }

        // Calculate completion percentage
        if (totalCourses != null && totalCourses > 0) {
            this.scheduledCourses = totalCourses - (unscheduledCourses != null ? unscheduledCourses : 0);
            this.completionPercentage = (scheduledCourses * 100.0) / totalCourses;
        } else {
            this.completionPercentage = 0.0;
        }

        // Determine if schedule is acceptable
        this.acceptable = (completionPercentage != null && completionPercentage >= 90.0) &&
                         (conflicts == null || conflicts.stream()
                             .noneMatch(c -> c.getSeverity() == com.heronix.scheduler.model.enums.ConflictSeverity.CRITICAL));

        // Build summary message
        buildSummaryMessage();
    }

    /**
     * Build user-friendly summary message
     */
    private void buildSummaryMessage() {
        StringBuilder message = new StringBuilder();

        if (completionPercentage >= 100.0) {
            message.append("✅ Schedule generation completed successfully! ");
            message.append("All ").append(totalCourses).append(" courses have been scheduled.");
        } else if (completionPercentage >= 90.0) {
            message.append("⚠️ Schedule generation mostly successful. ");
            message.append(scheduledCourses).append(" of ").append(totalCourses)
                   .append(" courses scheduled (").append(String.format("%.1f", completionPercentage)).append("%). ");
            if (unscheduledCourses != null && unscheduledCourses > 0) {
                message.append(unscheduledCourses).append(" course(s) need attention.");
            }
        } else if (completionPercentage >= 70.0) {
            message.append("⚠️ Partial schedule generated. ");
            message.append(scheduledCourses).append(" of ").append(totalCourses)
                   .append(" courses scheduled (").append(String.format("%.1f", completionPercentage)).append("%). ");
            message.append("Please review conflicts and fix data issues.");
        } else {
            message.append("❌ Schedule generation encountered significant issues. ");
            message.append("Only ").append(String.format("%.1f", completionPercentage))
                   .append("% of courses could be scheduled. ");
            message.append("Please address the conflicts listed below.");
        }

        this.summaryMessage = message.toString();
    }

    /**
     * Get count of blocking conflicts
     *
     * @return Number of blocking conflicts
     */
    public int getBlockingConflictCount() {
        if (conflicts == null) return 0;
        return (int) conflicts.stream()
            .filter(c -> c.getSeverity() == com.heronix.scheduler.model.enums.ConflictSeverity.CRITICAL)
            .count();
    }

    /**
     * Get count of major conflicts
     *
     * @return Number of major conflicts
     */
    public int getMajorConflictCount() {
        if (conflicts == null) return 0;
        return (int) conflicts.stream()
            .filter(c -> c.getSeverity() == com.heronix.scheduler.model.enums.ConflictSeverity.HIGH)
            .count();
    }

    /**
     * Check if result indicates success
     *
     * @return true if schedule is complete or acceptable
     */
    public boolean isSuccess() {
        return acceptable != null && acceptable;
    }

    /**
     * Check if result requires user action
     *
     * @return true if conflicts need to be resolved
     */
    public boolean requiresUserAction() {
        return conflicts != null && !conflicts.isEmpty() &&
               conflicts.stream().anyMatch(c ->
                   c.getSeverity() == com.heronix.scheduler.model.enums.ConflictSeverity.CRITICAL ||
                   c.getSeverity() == com.heronix.scheduler.model.enums.ConflictSeverity.HIGH
               );
    }
}
