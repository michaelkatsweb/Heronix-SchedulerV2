package com.heronix.scheduler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Conflict Priority Score Model - SchedulerV2 Version
 * Location: src/main/java/com/heronix/scheduler/model/dto/ConflictPriorityScore.java
 *
 * ML-based scoring system to prioritize conflict resolution.
 * Higher scores indicate higher priority conflicts that should be resolved first.
 *
 * SIMPLIFIED VERSION: DTO for SchedulerV2 (no JPA dependencies)
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictPriorityScore {

    /**
     * Conflict identifier
     */
    private Long conflictId;

    /**
     * Total priority score (0-100, higher = more urgent)
     */
    private int totalScore;

    /**
     * Hard constraint violation score (0-50)
     * Hard constraints are critical and must be resolved
     */
    private int hardConstraintScore;

    /**
     * Affected entities score (0-25)
     * More affected entities = higher priority
     */
    private int affectedEntitiesScore;

    /**
     * Cascade impact score (0-25)
     * Potential to cause more conflicts = higher priority
     */
    private int cascadeImpactScore;

    /**
     * Historical difficulty score (0-15)
     * Based on past resolution attempts
     */
    private int historicalDifficultyScore;

    /**
     * Time sensitivity score (0-10)
     * Conflicts closer to schedule start date = higher priority
     */
    private int timeSensitivityScore;

    /**
     * Priority level based on score
     */
    private PriorityLevel priorityLevel;

    /**
     * Explanation of why this score was assigned
     */
    private String scoreExplanation;

    /**
     * Recommended action timeline
     */
    private ActionTimeline recommendedTimeline;

    /**
     * Priority Level Enum
     */
    public enum PriorityLevel {
        CRITICAL(90, 100, "Must resolve immediately"),
        HIGH(70, 89, "Resolve within 1 hour"),
        MEDIUM(40, 69, "Resolve within 1 day"),
        LOW(20, 39, "Resolve when convenient"),
        MINIMAL(0, 19, "Optional resolution");

        private final int minScore;
        private final int maxScore;
        private final String description;

        PriorityLevel(int minScore, int maxScore, String description) {
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.description = description;
        }

        public static PriorityLevel fromScore(int score) {
            for (PriorityLevel level : values()) {
                if (score >= level.minScore && score <= level.maxScore) {
                    return level;
                }
            }
            return MINIMAL;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Action Timeline Enum
     */
    public enum ActionTimeline {
        IMMEDIATE("Resolve now"),
        WITHIN_HOUR("Within 1 hour"),
        WITHIN_DAY("Within 1 day"),
        WITHIN_WEEK("Within 1 week"),
        WHEN_CONVENIENT("When convenient");

        private final String description;

        ActionTimeline(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Calculate total score from component scores
     */
    public void calculateTotalScore() {
        this.totalScore = hardConstraintScore +
                         affectedEntitiesScore +
                         cascadeImpactScore +
                         historicalDifficultyScore +
                         timeSensitivityScore;

        // Cap at 100
        if (totalScore > 100) {
            totalScore = 100;
        }

        // Set priority level
        this.priorityLevel = PriorityLevel.fromScore(totalScore);

        // Set recommended timeline
        this.recommendedTimeline = determineTimeline();
    }

    /**
     * Determine action timeline based on priority level
     */
    private ActionTimeline determineTimeline() {
        return switch (priorityLevel) {
            case CRITICAL -> ActionTimeline.IMMEDIATE;
            case HIGH -> ActionTimeline.WITHIN_HOUR;
            case MEDIUM -> ActionTimeline.WITHIN_DAY;
            case LOW -> ActionTimeline.WITHIN_WEEK;
            case MINIMAL -> ActionTimeline.WHEN_CONVENIENT;
        };
    }

    /**
     * Check if this is a critical conflict
     */
    public boolean isCritical() {
        return priorityLevel == PriorityLevel.CRITICAL;
    }

    /**
     * Check if this is a high priority conflict
     */
    public boolean isHighPriority() {
        return priorityLevel == PriorityLevel.HIGH || priorityLevel == PriorityLevel.CRITICAL;
    }

    /**
     * Get color code for UI display
     */
    public String getColorCode() {
        return switch (priorityLevel) {
            case CRITICAL -> "#FF0000";  // Red
            case HIGH -> "#FF8800";      // Orange
            case MEDIUM -> "#FFAA00";    // Yellow
            case LOW -> "#00AA00";       // Green
            case MINIMAL -> "#888888";   // Gray
        };
    }

    /**
     * Get display string for UI
     */
    public String getDisplayString() {
        return String.format("%s Priority (Score: %d) - %s",
            priorityLevel.name(),
            totalScore,
            recommendedTimeline.getDescription()
        );
    }
}
