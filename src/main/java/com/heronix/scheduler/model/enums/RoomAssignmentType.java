package com.heronix.scheduler.model.enums;

/**
 * Room Assignment Type Enum
 *
 * Defines the type of room assignment for multi-room courses
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
public enum RoomAssignmentType {
    PRIMARY("Primary Room"),
    SECONDARY("Secondary Room"),
    OVERFLOW("Overflow Room"),
    BREAKOUT("Breakout Room"),
    ROTATING("Rotating Room");

    private final String displayName;

    RoomAssignmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
