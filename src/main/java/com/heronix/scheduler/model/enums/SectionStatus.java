package com.heronix.scheduler.model.enums;

/**
 * Section Status Enum
 *
 * Status of a course section
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
public enum SectionStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    CANCELLED("Cancelled"),
    FULL("Full"),
    CLOSED("Closed");

    private final String displayName;

    SectionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
