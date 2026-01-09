package com.heronix.scheduler.model.enums;

/**
 * Enrollment Status Enum
 * Location: src/main/java/com/eduscheduler/model/enums/EnrollmentStatus.java
 * 
 * Status of a student's enrollment in a course
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-18
 */
public enum EnrollmentStatus {

    /**
     * Currently enrolled and active
     */
    ACTIVE("Active", "Student is currently enrolled in this course"),

    /**
     * Student dropped the course
     */
    DROPPED("Dropped", "Student has dropped this course"),

    /**
     * Course completed successfully
     */
    COMPLETED("Completed", "Student completed this course"),

    /**
     * Waiting for available slot
     */
    WAITLISTED("Waitlisted", "Student is on waitlist for this course"),

    /**
     * Transferred to different section
     */
    TRANSFERRED("Transferred", "Student transferred to a different section");

    private final String displayName;
    private final String description;

    EnrollmentStatus(String displayName, String description) {
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