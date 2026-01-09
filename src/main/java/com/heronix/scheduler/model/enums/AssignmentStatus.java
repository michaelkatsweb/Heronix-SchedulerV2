package com.heronix.scheduler.model.enums;

/**
 * Enum representing the status of a substitute assignment
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
public enum AssignmentStatus {

    /**
     * Assignment created but not yet confirmed
     */
    PENDING("Pending", "Assignment created, awaiting confirmation"),

    /**
     * Substitute has confirmed the assignment
     */
    CONFIRMED("Confirmed", "Substitute confirmed assignment"),

    /**
     * Assignment is currently active
     */
    IN_PROGRESS("In Progress", "Assignment currently active"),

    /**
     * Assignment completed successfully
     */
    COMPLETED("Completed", "Assignment completed successfully"),

    /**
     * Assignment was cancelled
     */
    CANCELLED("Cancelled", "Assignment cancelled"),

    /**
     * Substitute did not show up
     */
    NO_SHOW("No Show", "Substitute did not arrive");

    private final String displayName;
    private final String description;

    AssignmentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
