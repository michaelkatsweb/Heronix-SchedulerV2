package com.heronix.scheduler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Conflict Resolution Suggestion Model - SchedulerV2 Version
 * Location: src/main/java/com/heronix/scheduler/model/dto/ConflictResolutionSuggestion.java
 *
 * Represents an AI-generated suggestion for resolving a scheduling conflict.
 * Includes impact analysis, success probability, and detailed resolution steps.
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
public class ConflictResolutionSuggestion {

    /**
     * Unique identifier for this suggestion
     */
    private String id;

    /**
     * Type of resolution suggested
     */
    private ResolutionType type;

    /**
     * Human-readable description of the fix
     */
    private String description;

    /**
     * Detailed explanation of why this fix works
     */
    private String explanation;

    /**
     * Impact score (0-100, lower is better)
     * Represents how many entities will be affected by this change
     */
    private int impactScore;

    /**
     * Success probability (0-100, higher is better)
     * AI-estimated likelihood this fix will resolve the conflict
     */
    private int successProbability;

    /**
     * Number of entities affected by this fix
     */
    private int affectedEntitiesCount;

    /**
     * List of entities that will be modified
     */
    private List<String> affectedEntities;

    /**
     * Specific changes to be made
     */
    private List<ResolutionAction> actions;

    /**
     * Potential side effects or risks
     */
    private List<String> warnings;

    /**
     * Estimated time to apply this fix (in seconds)
     */
    private int estimatedTimeSeconds;

    /**
     * Whether this fix requires user confirmation
     */
    private boolean requiresConfirmation;

    /**
     * AI confidence level (0-100)
     */
    private int confidenceLevel;

    /**
     * Resolution Type Enum
     */
    public enum ResolutionType {
        CHANGE_TEACHER("Change Teacher Assignment"),
        CHANGE_ROOM("Change Room Assignment"),
        CHANGE_TIME_SLOT("Change Time Slot"),
        SWAP_TEACHERS("Swap Teachers Between Slots"),
        SWAP_ROOMS("Swap Rooms Between Slots"),
        SWAP_TIME_SLOTS("Swap Time Slots"),
        REASSIGN_STUDENT("Reassign Student to Different Section"),
        ADJUST_CAPACITY("Adjust Room or Section Capacity"),
        SPLIT_SECTION("Split Large Section"),
        COMBINE_SECTIONS("Combine Small Sections"),
        ADD_CO_TEACHER("Add Co-Teacher"),
        REMOVE_CONSTRAINT("Remove Soft Constraint"),
        OTHER("Other Resolution");

        private final String displayName;

        ResolutionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Resolution Action - Specific change to be made
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolutionAction {
        private String entityType;  // "Teacher", "Room", "TimeSlot", etc.
        private Long entityId;
        private String currentValue;
        private String newValue;
        private String changeDescription;
    }

    /**
     * Get priority ranking (lower is higher priority)
     * Combines impact score and success probability
     */
    public int getPriorityRanking() {
        // Lower impact + higher success = better ranking (lower number)
        return (int) (impactScore * 0.6 + (100 - successProbability) * 0.4);
    }

    /**
     * Get display string for UI
     */
    public String getDisplayString() {
        return String.format("%s (Impact: %d, Success: %d%%)",
            type.getDisplayName(),
            impactScore,
            successProbability
        );
    }

    /**
     * Check if this is a high-confidence suggestion
     */
    public boolean isHighConfidence() {
        return confidenceLevel >= 80 && successProbability >= 75;
    }

    /**
     * Check if this is a low-risk suggestion
     */
    public boolean isLowRisk() {
        return impactScore <= 20 && affectedEntitiesCount <= 3;
    }

    /**
     * Check if this can be auto-applied without confirmation
     */
    public boolean canAutoApply() {
        return !requiresConfirmation &&
               isHighConfidence() &&
               isLowRisk() &&
               (warnings == null || warnings.isEmpty());
    }
}
