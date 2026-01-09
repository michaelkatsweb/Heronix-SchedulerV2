package com.heronix.scheduler.model.enums;

/**
 * Priority Rule Type Enum
 *
 * Types of priority rules for course enrollment
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
public enum PriorityRuleType {
    GPA_THRESHOLD("GPA Threshold Rule", "Bonus points for students meeting/exceeding GPA threshold"),
    BEHAVIOR_BASED("Behavior-Based Rule", "Bonus points based on behavior scores"),
    SENIORITY("Seniority Rule", "Bonus points for upperclassmen (juniors, seniors)"),
    SPECIAL_NEEDS("Special Needs Accommodation", "Priority for IEP/504 students"),
    GIFTED("Gifted Student Priority", "Priority for gifted/talented program students"),
    FIRST_GENERATION("First Generation College Student", "Priority for first-gen college students"),
    ATHLETIC("Student Athlete", "Priority for student athletes (for scheduling flexibility)"),
    LEADERSHIP("Leadership Role", "Bonus for student government, club officers, etc."),
    ATTENDANCE("Attendance-Based", "Bonus for excellent attendance records"),
    COURSE_PREREQUISITE("Prerequisite Completion", "Bonus for completing prerequisites with high grade"),
    CUSTOM("Custom Rule", "Administrator-defined custom rule");

    private final String displayName;
    private final String description;

    PriorityRuleType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCharacteristicBased() {
        return this == SPECIAL_NEEDS || this == GIFTED || this == FIRST_GENERATION || this == ATHLETIC;
    }

    public boolean isMeritBased() {
        return this == GPA_THRESHOLD || this == BEHAVIOR_BASED || this == LEADERSHIP ||
               this == ATTENDANCE || this == COURSE_PREREQUISITE;
    }
}
