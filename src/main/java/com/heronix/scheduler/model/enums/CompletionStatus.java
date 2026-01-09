package com.heronix.scheduler.model.enums;

/**
 * Completion status for student course history
 * Tracks whether a student successfully completed a course
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7E - November 21, 2025
 */
public enum CompletionStatus {
    COMPLETED("Completed", "Successfully completed the course"),
    IN_PROGRESS("In Progress", "Currently taking the course"),
    FAILED("Failed", "Did not pass the course"),
    WITHDRAWN("Withdrawn", "Withdrew from the course"),
    TRANSFERRED("Transfer Credit", "Transfer credit from another institution"),
    WAIVED("Waived", "Requirement waived by administration"),
    INCOMPLETE("Incomplete", "Received incomplete grade");

    private final String displayName;
    private final String description;

    CompletionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this status counts as successfully completing the course
     * (for prerequisite purposes)
     */
    public boolean countsAsCompleted() {
        return this == COMPLETED || this == TRANSFERRED || this == WAIVED;
    }

    /**
     * Check if this status is a final/terminal state
     */
    public boolean isFinalStatus() {
        return this == COMPLETED || this == FAILED || this == WITHDRAWN ||
               this == TRANSFERRED || this == WAIVED;
    }

    /**
     * Check if student is still taking this course
     */
    public boolean isActive() {
        return this == IN_PROGRESS || this == INCOMPLETE;
    }
}
