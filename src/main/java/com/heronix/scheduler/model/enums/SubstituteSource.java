package com.heronix.scheduler.model.enums;

/**
 * Enum representing the source of a substitute teacher
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.1
 * @since 2025-11-16
 */
public enum SubstituteSource {

    /**
     * Internal substitute - Employed directly by the school/district
     * Managed within the school system, added and tracked manually
     */
    INTERNAL("Internal", "School/district-employed substitute"),

    /**
     * Third-party substitute - Provided by external agency
     * Examples: Kelly Services, ESS, Source4Teachers
     * Typically imported via CSV from agency rosters
     */
    THIRD_PARTY("Third-Party", "External agency substitute");

    private final String displayName;
    private final String description;

    SubstituteSource(String displayName, String description) {
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
