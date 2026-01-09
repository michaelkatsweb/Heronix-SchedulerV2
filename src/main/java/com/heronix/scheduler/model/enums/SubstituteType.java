package com.heronix.scheduler.model.enums;

/**
 * Enum representing different types of substitute staff
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
public enum SubstituteType {

    /**
     * Fully certified teacher qualified to teach specific subjects
     */
    CERTIFIED_TEACHER("Certified Teacher", "Fully certified teaching staff"),

    /**
     * Non-certified substitute with limited teaching authorization
     */
    UNCERTIFIED_SUBSTITUTE("Uncertified Substitute", "Non-certified substitute teacher"),

    /**
     * Paraprofessional or teaching aide
     */
    PARAPROFESSIONAL("Paraprofessional", "Paraprofessional/teaching aide"),

    /**
     * Long-term substitute for extended assignments
     */
    LONG_TERM_SUBSTITUTE("Long-Term Substitute", "Long-term assignment substitute");

    private final String displayName;
    private final String description;

    SubstituteType(String displayName, String description) {
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
