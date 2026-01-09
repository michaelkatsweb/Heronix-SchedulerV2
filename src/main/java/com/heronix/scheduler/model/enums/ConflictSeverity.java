package com.heronix.scheduler.model.enums;

/**
 * Conflict Severity Enum
 * Defines severity levels for scheduling conflicts
 *
 * Location: src/main/java/com/eduscheduler/model/enums/ConflictSeverity.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7B - Conflict Detection
 */
public enum ConflictSeverity {

    /**
     * CRITICAL - Must be resolved before schedule can be published
     * Examples: Teacher double-booking, room double-booking, student schedule conflicts
     */
    CRITICAL(
        "Critical",
        "Must be resolved immediately",
        "#F44336",  // Red
        "🔴",
        100
    ),

    /**
     * HIGH - Should be resolved soon, schedule quality significantly impacted
     * Examples: No lunch break, excessive teaching hours, capacity exceeded
     */
    HIGH(
        "High",
        "Should be resolved soon",
        "#FF9800",  // Orange
        "🟠",
        75
    ),

    /**
     * MEDIUM - Should be addressed, but schedule can function
     * Examples: Room type mismatch, insufficient travel time, under-enrollment
     */
    MEDIUM(
        "Medium",
        "Should be addressed when possible",
        "#FFC107",  // Amber
        "🟡",
        50
    ),

    /**
     * LOW - Minor issue, consider resolving for optimization
     * Examples: Suboptimal room assignment, preference violations
     */
    LOW(
        "Low",
        "Minor issue, can be addressed later",
        "#2196F3",  // Blue
        "🔵",
        25
    ),

    /**
     * INFO - Informational, not necessarily a problem
     * Examples: Notifications, reminders, suggestions
     */
    INFO(
        "Info",
        "Informational only",
        "#4CAF50",  // Green
        "ℹ️",
        0
    );

    private final String displayName;
    private final String description;
    private final String color;
    private final String icon;
    private final int priorityScore;

    ConflictSeverity(String displayName, String description, String color, String icon, int priorityScore) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
        this.icon = icon;
        this.priorityScore = priorityScore;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get color for UI display (hex color code)
     */
    public String getColor() {
        return color;
    }

    /**
     * Get icon for UI display
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Get priority score for sorting (higher = more severe)
     */
    public int getPriorityScore() {
        return priorityScore;
    }

    /**
     * Check if severity requires immediate action
     */
    public boolean requiresImmediateAction() {
        return this == CRITICAL || this == HIGH;
    }

    /**
     * Check if severity blocks schedule publication
     */
    public boolean blocksPublication() {
        return this == CRITICAL;
    }

    /**
     * Get CSS style class for UI styling
     */
    public String getStyleClass() {
        return "severity-" + name().toLowerCase();
    }
}
