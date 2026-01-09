package com.heronix.scheduler.model.dto;

import com.heronix.scheduler.model.enums.ConflictSeverity;
import com.heronix.scheduler.model.enums.ConflictType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed information about a scheduling conflict
 *
 * Provides comprehensive diagnostic information about why a course
 * could not be scheduled, along with suggested solutions and manual
 * override options.
 *
 * Location: src/main/java/com/eduscheduler/model/dto/ConflictDetail.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictDetail {

    /**
     * ID of the course that has a conflict
     */
    private Long courseId;

    /**
     * Name/code of the conflicted course for display
     */
    private String courseName;

    /**
     * Code for the course (e.g., MATH101)
     */
    private String courseCode;

    /**
     * Type of conflict encountered
     */
    private ConflictType type;

    /**
     * Severity level of this conflict
     */
    private ConflictSeverity severity;

    /**
     * Human-readable description of the problem
     */
    private String description;

    /**
     * Number of students affected by this conflict
     */
    private Integer studentsAffected;

    /**
     * ID of the schedule slot that couldn't be assigned
     */
    private Long slotId;

    /**
     * Suggested solutions to resolve this conflict
     */
    @Builder.Default
    private List<String> possibleSolutions = new ArrayList<>();

    /**
     * Manual override options available
     */
    @Builder.Default
    private List<ManualOverrideOption> overrideOptions = new ArrayList<>();

    /**
     * Related entity IDs (e.g., teacher ID, room ID that caused conflict)
     */
    @Builder.Default
    private List<Long> relatedEntityIds = new ArrayList<>();

    /**
     * Whether this conflict is blocking (prevents any schedule generation)
     */
    private Boolean blocking;

    /**
     * Estimated time to fix this issue (in minutes)
     */
    private Integer estimatedFixTimeMinutes;

    /**
     * Constraint that was violated
     */
    private String violatedConstraint;

    /**
     * Current value vs required value
     */
    private String constraintViolationDetails;

    /**
     * Add a possible solution
     *
     * @param solution Description of how to fix this conflict
     */
    public void addPossibleSolution(String solution) {
        if (this.possibleSolutions == null) {
            this.possibleSolutions = new ArrayList<>();
        }
        this.possibleSolutions.add(solution);
    }

    /**
     * Add a manual override option
     *
     * @param option Override option available to user
     */
    public void addOverrideOption(ManualOverrideOption option) {
        if (this.overrideOptions == null) {
            this.overrideOptions = new ArrayList<>();
        }
        this.overrideOptions.add(option);
    }

    /**
     * Add a related entity ID
     *
     * @param entityId ID of related entity (teacher, room, etc.)
     */
    public void addRelatedEntityId(Long entityId) {
        if (this.relatedEntityIds == null) {
            this.relatedEntityIds = new ArrayList<>();
        }
        if (entityId != null) {
            this.relatedEntityIds.add(entityId);
        }
    }

    /**
     * Manual override option for conflict resolution
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManualOverrideOption {
        /**
         * Display text for this option
         */
        private String label;

        /**
         * Action identifier (e.g., "ASSIGN_TO_ROOM_101")
         */
        private String action;

        /**
         * Teacher ID to assign (if applicable)
         */
        private Long teacherId;

        /**
         * Room ID to assign (if applicable)
         */
        private Long roomId;

        /**
         * Time slot ID to assign (if applicable)
         */
        private Long timeSlotId;

        /**
         * Warning message if this override has consequences
         */
        private String warningMessage;

        /**
         * Whether this override requires confirmation
         */
        private Boolean requiresConfirmation;
    }
}
