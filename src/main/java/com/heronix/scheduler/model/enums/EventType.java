package com.heronix.scheduler.model.enums;

/**
 * Event Type Enum
 *
 * Types of special events in the school calendar
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
public enum EventType {
    // Academic Events
    ASSEMBLY("School Assembly"),
    EXAM("Examination"),
    MIDTERM("Midterm Exam"),
    FINAL_EXAM("Final Examination"),
    TESTING("Standardized Testing"),
    GRADUATION("Graduation Ceremony"),
    
    // Special Education & Compliance
    IEP_MEETING("IEP Meeting"),
    IEP_ANNUAL_REVIEW("IEP Annual Review"),
    SECTION_504_MEETING("504 Plan Meeting"),
    SECTION_504_REVIEW("504 Plan Review"),
    RTI_MEETING("Response to Intervention Meeting"),
    SPED_EVALUATION("Special Education Evaluation"),
    
    // Parent/Guardian Engagement
    PARENT_TEACHER_CONFERENCE("Parent-Teacher Conference"),
    OPEN_HOUSE("Open House"),
    BACK_TO_SCHOOL_NIGHT("Back to School Night"),
    FAMILY_NIGHT("Family Engagement Night"),
    
    // Professional Development
    PROFESSIONAL_DEVELOPMENT("Professional Development"),
    FACULTY_MEETING("Faculty Meeting"),
    DEPARTMENT_MEETING("Department Meeting"),
    TEACHER_TRAINING("Teacher Training"),
    PLC_MEETING("Professional Learning Community"),
    
    // Activities & Sports
    SPORTS_EVENT("Sports Event"),
    FIELD_TRIP("Field Trip"),
    CLUB_MEETING("Club Meeting"),
    REHEARSAL("Performance Rehearsal"),
    SCHOOL_PLAY("School Play/Performance"),
    
    // Administrative
    BOARD_MEETING("School Board Meeting"),
    DISCIPLINE_HEARING("Disciplinary Hearing"),
    ENROLLMENT("Student Enrollment"),
    
    // Special Occasions
    HOLIDAY("School Holiday"),
    EARLY_DISMISSAL("Early Dismissal"),
    LATE_START("Late Start"),
    TEACHER_PLANNING_DAY("Teacher Planning Day"),
    SCHOOL_CLOSED("School Closed"),
    WEATHER_DELAY("Weather Delay/Closure"),
    
    // Emergency & Safety
    FIRE_DRILL("Fire Drill"),
    LOCKDOWN_DRILL("Lockdown Drill"),
    EMERGENCY_DRILL("Emergency Drill"),
    
    // Other
    LUNCH_EVENT("Lunch Event"),
    FUNDRAISER("Fundraising Event"),
    COMMUNITY_SERVICE("Community Service"),
    OTHER("Other Event");
    
    private final String displayName;
    
    EventType(String displayName) {
        this.displayName = displayName;
    }
    
    EventType() {
        this.displayName = name().replace("_", " ");
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isScheduleBlocking() {
        return switch (this) {
            case ASSEMBLY, EXAM, MIDTERM, FINAL_EXAM, TESTING,
                 GRADUATION, EARLY_DISMISSAL, LATE_START, 
                 TEACHER_PLANNING_DAY, SCHOOL_CLOSED, WEATHER_DELAY,
                 FIRE_DRILL, LOCKDOWN_DRILL, EMERGENCY_DRILL,
                 PROFESSIONAL_DEVELOPMENT, BACK_TO_SCHOOL_NIGHT,
                 OPEN_HOUSE, HOLIDAY -> true;
            default -> false;
        };
    }
    
    public boolean requiresAccommodations() {
        return switch (this) {
            case IEP_MEETING, IEP_ANNUAL_REVIEW, 
                 SECTION_504_MEETING, SECTION_504_REVIEW,
                 RTI_MEETING, SPED_EVALUATION -> true;
            default -> false;
        };
    }
    
    public boolean requiresParentNotification() {
        return switch (this) {
            case IEP_MEETING, IEP_ANNUAL_REVIEW,
                 SECTION_504_MEETING, SECTION_504_REVIEW,
                 PARENT_TEACHER_CONFERENCE, OPEN_HOUSE,
                 BACK_TO_SCHOOL_NIGHT, FAMILY_NIGHT,
                 EARLY_DISMISSAL, SCHOOL_CLOSED, WEATHER_DELAY,
                 FIELD_TRIP, DISCIPLINE_HEARING -> true;
            default -> false;
        };
    }
}
