package com.heronix.scheduler.model.enums;

/**
 * Education Level Enum
 *
 * Grade level classifications
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
public enum EducationLevel {
    PRE_K("Pre-Kindergarten"),
    KINDERGARTEN("Kindergarten"),
    ELEMENTARY("Elementary School"),
    MIDDLE_SCHOOL("Middle School"),
    HIGH_SCHOOL("High School"),
    COLLEGE("College"),
    UNIVERSITY("University"),
    GRADUATE("Graduate School");

    private final String displayName;

    EducationLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
