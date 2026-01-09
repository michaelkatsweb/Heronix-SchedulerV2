package com.heronix.scheduler.model.enums;

/**
 * Duty Type Enum - Types of special duties that can be assigned
 * Covers daily routines and special event duties
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-06
 */
public enum DutyType {
    // Daily Routine Duties
    BUS_DUTY("Bus Duty", "Supervise student arrival/departure at bus area", true),
    HALL_MONITOR("Hall Monitor", "Monitor hallways between classes", true),
    CAFETERIA_DUTY("Cafeteria Duty", "Supervise students during lunch periods", true),
    DETENTION_SUPERVISION("Detention Supervision", "Supervise detention sessions", true),
    LIBRARY_DUTY("Library Duty", "Monitor library during study periods", true),
    PLAYGROUND_DUTY("Playground/Recess Duty", "Supervise outdoor activities", true),
    PARKING_LOT_DUTY("Parking Lot Duty", "Monitor parking lot and traffic flow", true),
    ARRIVAL_DUTY("Arrival Duty", "Supervise morning student arrival", true),
    DISMISSAL_DUTY("Dismissal Duty", "Supervise afternoon student dismissal", true),
    STUDY_HALL("Study Hall Supervision", "Monitor study hall periods", true),

    // Special Event Duties
    ATHLETIC_EVENT("Athletic Event Supervision", "Supervise sports games/matches", false),
    SCHOOL_DANCE("School Dance Chaperone", "Chaperone school dances", false),
    FIELD_TRIP("Field Trip Chaperone", "Accompany students on field trips", false),
    CONCERT_PERFORMANCE("Concert/Performance", "Supervise music or theater events", false),
    OPEN_HOUSE("Open House/Parent Night", "Staff open house events", false),
    GRADUATION("Graduation Ceremony", "Assist with graduation activities", false),
    PROM_FORMAL("Prom/Formal Chaperone", "Chaperone prom or formal events", false),
    CLUB_ACTIVITY("Club Activity Supervision", "Supervise after-school clubs", false),
    PARENT_CONFERENCE("Parent-Teacher Conferences", "Attend conference nights", false),
    TESTING_PROCTORING("Testing Proctor", "Proctor standardized tests", false),
    ASSEMBLY("Assembly Supervision", "Monitor school assemblies", false),
    FUNDRAISER("Fundraiser Event", "Assist with fundraising events", false),
    FIELD_DAY("Field Day", "Supervise field day activities", false),
    SCIENCE_FAIR("Science Fair", "Monitor science fair event", false),
    TALENT_SHOW("Talent Show", "Supervise talent show performance", false),
    HOMECOMING("Homecoming Activities", "Assist with homecoming events", false),

    // Administrative/Special Duties
    SAFETY_PATROL("Safety Patrol Coordination", "Coordinate student safety patrol", true),
    EMERGENCY_DRILL("Emergency Drill Coordinator", "Lead fire/lockdown drills", true),
    SUBSTITUTE_COORDINATION("Substitute Coordinator", "Manage substitute assignments", true),
    CURRICULUM_NIGHT("Curriculum Night", "Present curriculum to parents", false),
    PROFESSIONAL_DEVELOPMENT("Professional Development", "Attend or lead PD sessions", false),
    CUSTOM("Custom Duty", "User-defined custom duty", true);

    private final String displayName;
    private final String description;
    private final boolean isDaily; // true for daily/routine, false for special events

    DutyType(String displayName, String description, boolean isDaily) {
        this.displayName = displayName;
        this.description = description;
        this.isDaily = isDaily;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDaily() {
        return isDaily;
    }

    public boolean isSpecialEvent() {
        return !isDaily;
    }

    /**
     * Get all daily routine duties
     */
    public static DutyType[] getDailyDuties() {
        return java.util.Arrays.stream(values())
                .filter(DutyType::isDaily)
                .toArray(DutyType[]::new);
    }

    /**
     * Get all special event duties
     */
    public static DutyType[] getSpecialEventDuties() {
        return java.util.Arrays.stream(values())
                .filter(DutyType::isSpecialEvent)
                .toArray(DutyType[]::new);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
