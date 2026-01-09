package com.heronix.scheduler.model.enums;

/**
 * Event Block Type Enum
 *
 * Types of special event time blocks for scheduling.
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
public enum EventBlockType {
    IEP_MEETING("IEP Meeting"),
    SECTION_504_MEETING("504 Meeting"),
    TEACHER_PLANNING("Teacher Planning"),
    DEPARTMENT_MEETING("Department Meeting"),
    PARENT_CONFERENCE("Parent Conference"),
    STAFF_DEVELOPMENT("Staff Development"),
    RECURRING_WEEKLY("Recurring Weekly"),
    RECURRING_DAILY("Recurring Daily"),
    ONE_TIME("One-Time Event");

    private final String displayName;

    EventBlockType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
