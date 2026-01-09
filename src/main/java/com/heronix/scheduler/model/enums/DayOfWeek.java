package com.heronix.scheduler.model.enums;

/**
 * Days of the Week Enum
 * Location: src/main/java/com/eduscheduler/model/enums/DayOfWeek.java
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-10
 */
public enum DayOfWeek {
    MONDAY("Monday"),
    TUESDAY("Tuesday"),
    WEDNESDAY("Wednesday"),
    THURSDAY("Thursday"),
    FRIDAY("Friday"),
    SATURDAY("Saturday"),
    SUNDAY("Sunday");

    private final String displayName;

    DayOfWeek(String displayName) {
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