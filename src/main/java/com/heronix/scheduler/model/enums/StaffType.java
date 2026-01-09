package com.heronix.scheduler.model.enums;

/**
 * Enum representing the type of staff being replaced by a substitute
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
public enum StaffType {

    /**
     * Regular classroom teacher
     */
    TEACHER("Teacher", "Regular classroom teacher"),

    /**
     * Co-teaching staff member
     */
    CO_TEACHER("Co-Teacher", "Co-teaching support staff"),

    /**
     * Paraprofessional or teaching aide
     */
    PARAPROFESSIONAL("Paraprofessional", "Paraprofessional or teaching aide"),

    /**
     * Special education teacher
     */
    SPECIAL_ED("Special Education", "Special education teacher"),

    /**
     * Other staff type
     */
    OTHER("Other", "Other staff type");

    private final String displayName;
    private final String description;

    StaffType(String displayName, String description) {
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
