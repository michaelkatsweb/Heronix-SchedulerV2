package com.heronix.scheduler.model.enums;

/**
 * Status of a Schedule (master schedule)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 */
public enum ScheduleStatus {
    DRAFT("Draft", "Schedule is being drafted"),
    IN_PROGRESS("In Progress", "Schedule is being built"),
    REVIEW("Review", "Schedule is under review"),
    PUBLISHED("Published", "Schedule is published and active"),
    ARCHIVED("Archived", "Schedule has been archived");

    private final String displayName;
    private final String description;

    ScheduleStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPublished() {
        return this == PUBLISHED;
    }

    public boolean isEditable() {
        return this == DRAFT || this == IN_PROGRESS;
    }
}
