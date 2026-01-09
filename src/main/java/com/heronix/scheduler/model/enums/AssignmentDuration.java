package com.heronix.scheduler.model.enums;

/**
 * Enum representing different durations of substitute assignments
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
public enum AssignmentDuration {

    /**
     * Short hourly coverage (1-2 hours) for meetings or planning periods
     */
    HOURLY("Hourly", "1-2 hours coverage for meetings or planning"),

    /**
     * Half-day coverage (morning or afternoon)
     */
    HALF_DAY("Half Day", "Morning or afternoon coverage"),

    /**
     * Full school day coverage
     */
    FULL_DAY("Full Day", "Entire school day coverage"),

    /**
     * Multiple consecutive days (2-5 days)
     */
    MULTI_DAY("Multi-Day", "2-5 consecutive days coverage"),

    /**
     * Long-term assignment (1+ weeks)
     */
    LONG_TERM("Long-Term", "Extended coverage of 1+ weeks");

    private final String displayName;
    private final String description;

    AssignmentDuration(String displayName, String description) {
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
