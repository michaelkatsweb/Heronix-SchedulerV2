// FILE: ScheduleType.java
// LOCATION: /src/main/java/com/eduscheduler/model/enums/ScheduleType.java
package com.heronix.scheduler.model.enums;

public enum ScheduleType {
    TRADITIONAL("Traditional (6-8 periods)"),
    BLOCK("Block Schedule (4 periods)"),
    ROTATING("Rotating A/B Schedule"),
    MODULAR("Modular (Short periods)"),
    TRIMESTER("Trimester System"),
    QUARTER("Quarter System"),
    FLEX_MOD("Flexible Modular");

    private final String displayName;

    ScheduleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}