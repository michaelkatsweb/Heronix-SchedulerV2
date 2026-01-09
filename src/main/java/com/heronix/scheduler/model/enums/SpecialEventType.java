package com.heronix.scheduler.model.enums;

/**
 * Special Event Type Enum
 * Represents types of special events that can be scheduled
 *
 * Location: src/main/java/com/eduscheduler/model/enums/SpecialEventType.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
public enum SpecialEventType {
    ASSEMBLY("Assembly"),
    TESTING("Testing"),
    FIELD_TRIP("Field Trip"),
    PROFESSIONAL_DEVELOPMENT("Professional Development"),
    PARENT_CONFERENCE("Parent Conference"),
    SCHOOL_EVENT("School Event"),
    SPORTS_EVENT("Sports Event"),
    CLUB_MEETING("Club Meeting"),
    GRADUATION("Graduation"),
    OTHER("Other");

    private final String displayName;

    SpecialEventType(String displayName) {
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
