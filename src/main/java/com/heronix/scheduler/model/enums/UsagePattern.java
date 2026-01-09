package com.heronix.scheduler.model.enums;

/**
 * Usage Pattern Enum
 *
 * Defines when and how a room is used in a multi-room course assignment
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
public enum UsagePattern {
    ALWAYS("Always"),
    ALTERNATING_DAYS("Alternating Days"),
    ODD_DAYS("Odd-Numbered Days"),
    EVEN_DAYS("Even-Numbered Days"),
    FIRST_HALF("First Half of Period"),
    SECOND_HALF("Second Half of Period"),
    SPECIFIC_DAYS("Specific Days Only"),
    WEEKLY_ROTATION("Weekly Rotation");

    private final String displayName;

    UsagePattern(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public boolean isDayBased() {
        return this == ALWAYS
            || this == ALTERNATING_DAYS
            || this == ODD_DAYS
            || this == EVEN_DAYS
            || this == SPECIFIC_DAYS
            || this == WEEKLY_ROTATION;
    }

    public boolean isTimeBased() {
        return this == FIRST_HALF || this == SECOND_HALF;
    }
}
