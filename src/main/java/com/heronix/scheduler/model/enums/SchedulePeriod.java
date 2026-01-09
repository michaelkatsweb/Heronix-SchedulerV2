package com.heronix.scheduler.model.enums;

public enum SchedulePeriod {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MASTER("Master Schedule"),
    SEMESTER("Semester"),
    TRIMESTER("Trimester"),
    QUARTER("Quarter"),
    YEARLY("Yearly");

    private final String displayName;

    SchedulePeriod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}