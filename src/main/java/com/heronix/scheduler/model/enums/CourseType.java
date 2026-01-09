package com.heronix.scheduler.model.enums;

/**
 * Course Type Enum
 *
 * Represents the difficulty/advancement level of a course
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
public enum CourseType {
    REGULAR("Regular"),
    HONORS("Honors"),
    AP("Advanced Placement (AP)"),
    IB("International Baccalaureate (IB)"),
    DUAL_ENROLLMENT("Dual Enrollment"),
    REMEDIAL("Remedial/Support"),
    GIFTED("Gifted/Talented"),
    ELECTIVE("Elective");

    private final String displayName;

    CourseType(String displayName) {
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
