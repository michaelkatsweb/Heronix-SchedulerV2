package com.heronix.scheduler.model.enums;

/**
 * Enum representing reasons for staff absence requiring substitute coverage
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
public enum AbsenceReason {

    /**
     * Staff member is sick
     */
    SICK_LEAVE("Sick Leave", "Staff illness or medical appointment"),

    /**
     * Personal day off
     */
    PERSONAL_DAY("Personal Day", "Personal time off"),

    /**
     * Attending IEP meeting
     */
    IEP_MEETING("IEP Meeting", "Individual Education Plan meeting"),

    /**
     * Attending 504 Plan meeting
     */
    PLAN_504_MEETING("504 Meeting", "504 Plan meeting or review"),

    /**
     * Professional development or training
     */
    PROFESSIONAL_DEV("Professional Development", "Training or professional development"),

    /**
     * Extended medical leave
     */
    MEDICAL_LEAVE("Medical Leave", "Extended medical leave (FMLA)"),

    /**
     * Maternity or paternity leave
     */
    MATERNITY_LEAVE("Maternity/Paternity", "Parental leave"),

    /**
     * Family emergency
     */
    FAMILY_EMERGENCY("Family Emergency", "Emergency family situation"),

    /**
     * Bereavement leave
     */
    BEREAVEMENT("Bereavement", "Death in family"),

    /**
     * Jury duty
     */
    JURY_DUTY("Jury Duty", "Mandatory jury service"),

    /**
     * Other unspecified reason
     */
    OTHER("Other", "Other reason for absence");

    private final String displayName;
    private final String description;

    AbsenceReason(String displayName, String description) {
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
